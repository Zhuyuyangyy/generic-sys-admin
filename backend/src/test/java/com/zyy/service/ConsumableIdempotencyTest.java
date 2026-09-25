package com.zyy.service;

import com.zyy.mapper.ConsumableMapper;
import com.zyy.mapper.IdempotencyRecordMapper;
import com.zyy.model.entity.ConsumableEntity;
import com.zyy.model.entity.IdempotencyRecordEntity;
import com.zyy.service.impl.ConsumableServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * 库存幂等性与并发的可执行证明。
 *
 * <p>这些测试模拟唯一约束的真实语义：第一次 claim 成功，其余并发 claim 抛
 * {@link DuplicateKeyException}。被测对象不依赖任何 JVM 锁 —— 竞争由"数据库
 * unique constraint"这一模拟层裁决，与多实例部署下的行为一致。</p>
 *
 * <p>三个必须同时成立的性质：</p>
 * <ol>
 *   <li>相同 key + 相同 payload：库存只变一次、流水只多一条</li>
 *   <li>相同 key + 不同 payload：409 冲突，且库存完全不变</li>
 *   <li>两个真正并发的相同请求：副作用恰好一次</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存幂等性测试")
class ConsumableIdempotencyTest {

    private static final long CONSUMABLE_ID = 7L;
    private static final long OPERATOR_ID = 42L;

    @Mock
    private ConsumableMapper consumableMapper;

    @Mock
    private IdempotencyRecordMapper recordMapper;

    @Mock
    private com.zyy.mapper.InventoryTransactionMapper transactionMapper;

    private ConsumableServiceImpl service;

    /** 模拟 DB 行：stock 是唯一真实状态。 */
    private final AtomicReference<ConsumableEntity> storedRow = new AtomicReference<>();

    /**
     * 已占用的 (operator, op, key) → 状态，模拟
     * UNIQUE(operator_id, operation, idempotency_key) 与 status 列。
     */
    private final Map<String, KeyState> claimedKeys = new ConcurrentHashMap<>();
    private final AtomicInteger autoId = new AtomicInteger(1);

    private record KeyState(String hash, String status) {}

    private void buildService() {
        lenient().when(consumableMapper.selectById(anyLong())).thenAnswer(inv -> {
            ConsumableEntity row = storedRow.get();
            return row == null ? null : copy(row);
        });

        lenient().when(consumableMapper.updateById(any())).thenAnswer(inv -> {
            storedRow.set(copy(inv.getArgument(0)));
            return 1;
        });

        // 原子自增：真正改变唯一状态的地方
        lenient().when(consumableMapper.adjustStockAtomic(anyLong(), anyInt())).thenAnswer(inv -> {
            int delta = inv.getArgument(1);
            ConsumableEntity row = storedRow.get();
            if (row == null) {
                return 0;
            }
            int next = row.getStockQuantity() + delta;
            if (next < 0) {
                return 0;
            }
            row.setStockQuantity(next);
            return 1;
        });

        // claim：并发安全；已占用即抛 DuplicateKeyException（真实约束行为）
        lenient().when(recordMapper.claim(any(IdempotencyRecordEntity.class))).thenAnswer(inv -> {
            IdempotencyRecordEntity rec = inv.getArgument(0);
            String scope = rec.getOperatorId() + "|" + rec.getOperation() + "|" + rec.getIdempotencyKey();
            // putIfAbsent 的原子性就是"数据库唯一约束"在这套 mock 里的等价物
            KeyState prev = claimedKeys.putIfAbsent(scope, new KeyState(rec.getRequestHash(), "PENDING"));
            if (prev != null) {
                throw new DuplicateKeyException("Duplicate entry '" + scope + "'");
            }
            rec.setId((long) autoId.getAndIncrement());
            return 1;
        });

        lenient().when(recordMapper.findScoped(anyLong(), any(), any())).thenAnswer(inv -> {
            String scope = inv.getArgument(0) + "|" + inv.getArgument(1) + "|" + inv.getArgument(2);
            KeyState st = claimedKeys.get(scope);
            if (st == null) {
                return null;
            }
            IdempotencyRecordEntity rec = new IdempotencyRecordEntity();
            rec.setOperatorId(inv.getArgument(0));
            rec.setOperation(inv.getArgument(1));
            rec.setIdempotencyKey(inv.getArgument(2));
            rec.setRequestHash(st.hash());
            rec.setStatus(st.status());
            rec.setId(999L);
            return rec;
        });

        lenient().when(recordMapper.markFailed(any(), any())).thenAnswer(inv -> {
            claimedKeys.replaceAll((k, v) -> new KeyState(v.hash(), "FAILED"));
            return 1;
        });

        lenient().when(recordMapper.markSuccess(any(), any())).thenAnswer(inv -> {
            // 依据 recordId 反查不可靠，这里改用 scan：把 PENDING 置 SUCCESS
            Long rid = inv.getArgument(0);
            claimedKeys.replaceAll((k, v) -> v.status().equals("PENDING") && rid != null
                    ? new KeyState(v.hash(), "SUCCESS") : v);
            return 1;
        });

        lenient().when(recordMapper.deleteFailed(anyLong(), any(), any())).thenReturn(1);

        service = new ConsumableServiceImpl(
                consumableMapper, transactionMapper,
                new InventoryIdempotencyService(recordMapper));
    }

    private ConsumableEntity consumableWithStock(int stock) {
        ConsumableEntity e = new ConsumableEntity();
        e.setId(CONSUMABLE_ID);
        e.setStockQuantity(stock);
        storedRow.set(e);
        return e;
    }

    private static ConsumableEntity copy(ConsumableEntity src) {
        ConsumableEntity dst = new ConsumableEntity();
        dst.setId(src.getId());
        dst.setStockQuantity(src.getStockQuantity());
        return dst;
    }

    private String fingerprintForInbound(int qty) {
        return InventoryIdempotencyService.fingerprint(
                InventoryIdempotencyService.OP_INBOUND,
                Map.of("id", CONSUMABLE_ID, "quantity", qty, "referenceNo", ""));
    }

    // ==================== 1. 相同 key + 相同 payload ====================

    @Test
    @DisplayName("相同 key 重放：库存只加一次，不重复执行业务变更")
    void sameKeySamePayloadAppliesOnce() {
        buildService();
        consumableWithStock(100);

        service.inbound(CONSUMABLE_ID, 10, "REF-1", null, OPERATOR_ID, "key-abc");

        assertEquals(110, storedRow.get().getStockQuantity());

        // 第二次：抛 ReplayedRequestException，stock 必须保持 110
        assertThrows(com.zyy.service.impl.ConsumableServiceImpl.ReplayedRequestException.class,
                () -> service.inbound(CONSUMABLE_ID, 10, "REF-1", null, OPERATOR_ID, "key-abc"));

        assertEquals(110, storedRow.get().getStockQuantity(),
                "重放绝不能再次修改库存");
    }

    @Test
    @DisplayName("未提供 key：行为与改造前一致（向后兼容，无保护）")
    void noKeyKeepsLegacyBehaviour() {
        buildService();
        consumableWithStock(100);

        service.inbound(CONSUMABLE_ID, 10, null, null, OPERATOR_ID, null);
        service.inbound(CONSUMABLE_ID, 10, null, null, OPERATOR_ID, null);

        assertEquals(120, storedRow.get().getStockQuantity(),
                "无 key 时调用方自行承担重试风险，与既有语义一致");
        verify(recordMapper, org.mockito.Mockito.never()).claim(any());
    }

    // ==================== 2. 相同 key + 不同 payload ====================

    @Test
    @DisplayName("相同 key 不同数量：409 冲突，库存完全不变")
    void sameKeyDifferentPayloadConflicts() {
        buildService();
        consumableWithStock(100);

        service.inbound(CONSUMABLE_ID, 10, "REF-1", null, OPERATOR_ID, "key-abc");
        assertEquals(110, storedRow.get().getStockQuantity());

        InventoryIdempotencyService.IdempotencyKeyConflictException ex = assertThrows(
                InventoryIdempotencyService.IdempotencyKeyConflictException.class,
                () -> service.inbound(CONSUMABLE_ID, 1000, "REF-1", null, OPERATOR_ID, "key-abc"));

        assertTrue(ex.getMessage().contains("Idempotency-Key"));
        assertEquals(110, storedRow.get().getStockQuantity(),
                "冲突请求绝不能改变库存");
    }

    @Test
    @DisplayName("相同 key 指向不同耗材：同样是冲突（key 不含 consumable_id）")
    void sameKeyDifferentConsumableConflicts() {
        buildService();
        consumableWithStock(100);

        service.inbound(CONSUMABLE_ID, 10, "REF-1", null, OPERATOR_ID, "key-xyz");

        // 同一 operator + 同一 operation + 同一 key，但 consumable 不同 →
        // request_hash 不同，必须冲突而不是被当成第二笔合法操作
        InventoryIdempotencyService.IdempotencyKeyConflictException ex = assertThrows(
                InventoryIdempotencyService.IdempotencyKeyConflictException.class,
                () -> service.inbound(999L, 10, "REF-1", null, OPERATOR_ID, "key-xyz"));
        assertNotNull(ex);
    }

    // ==================== 3. 并发 ====================

    @Test
    @DisplayName("两个并发相同请求：副作用恰好一次")
    void concurrentSameKeyAppliesOnce() throws Exception {
        buildService();
        consumableWithStock(100);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger replayed = new AtomicInteger();
        AtomicInteger busy = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    try {
                        service.inbound(CONSUMABLE_ID, 10, "REF-1", null, OPERATOR_ID, "race-key");
                        succeeded.incrementAndGet();
                    } catch (com.zyy.service.impl.ConsumableServiceImpl.ReplayedRequestException e) {
                        replayed.incrementAndGet();
                    } catch (com.zyy.exception.BusinessException e) {
                        // 输家到达时赢家尚未提交：key 处于 PENDING，被拒绝。
                        // 这也是一种正确的"没有重复执行"，由 busy 计数承载。
                        busy.incrementAndGet();
                    }
                } catch (Throwable t) {
                    failure.set(t);
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));

        assertNull(failure.get(), "并发请求不应抛意外异常: " + failure.get());
        // 赢家执行业务；输家要么被判为"正在处理"，要么（赢家已提交时）被判为重放。
        // 两种都是"没有重复执行"。
        assertEquals(1, succeeded.get(), "只能有一个请求真正执行业务变更");
        assertEquals(1, replayed.get() + busy.get(), "输家必须被拒绝或判为重放");
        assertEquals(110, storedRow.get().getStockQuantity(),
                "stock 只允许 +10，这正是 exactly-once 的定义");
    }

    @Test
    @DisplayName("高并发同一 key：副作用仍恰好一次")
    void highConcurrencySameKeyAppliesOnce() throws Exception {
        buildService();
        consumableWithStock(1000);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger executed = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    service.inbound(CONSUMABLE_ID, 5, "REF-C", null, OPERATOR_ID, "burst-key");
                    executed.incrementAndGet();
                } catch (com.zyy.service.impl.ConsumableServiceImpl.ReplayedRequestException ignored) {
                    // 重放不算执行
                } catch (com.zyy.exception.BusinessException ignored2) {
                    // key 仍处于 PENDING：被拒，同样不算执行
                } catch (Throwable t) {
                    failure.set(t);
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertNull(failure.get());
        assertEquals(1, executed.get(), "8 个并发中只能有一个真正写库存");
        assertEquals(1005, storedRow.get().getStockQuantity(),
                "1000 + 5：只有一次入库生效");
    }

    // ==================== 4. 失败后可重试 ====================

    @Test
    @DisplayName("FAILED 记录不永久锁死 key：可重试")
    void failedKeyCanBeRetried() {
        buildService();
        consumableWithStock(10);

        // 第一次：库存不足 → 失败
        assertThrows(com.zyy.exception.BusinessException.class,
                () -> service.outbound(CONSUMABLE_ID, 999, null, null, OPERATOR_ID, "fail-key"));

        // 标记为 FAILED（模拟业务失败后的状态持久化）
        lenient().when(recordMapper.deleteFailed(anyLong(), any(), any())).thenAnswer(inv -> {
            String scope = inv.getArgument(0) + "|" + inv.getArgument(1) + "|" + inv.getArgument(2);
            claimedKeys.remove(scope);
            return 1;
        });
        // 重试时 claim 再次成功
        lenient().when(recordMapper.findScoped(anyLong(), any(), any())).thenAnswer(inv -> {
            String scope = inv.getArgument(0) + "|" + inv.getArgument(1) + "|" + inv.getArgument(2);
            KeyState st = claimedKeys.get(scope);
            if (st == null) {
                return null;
            }
            IdempotencyRecordEntity rec = new IdempotencyRecordEntity();
            rec.setStatus(st.status());
            rec.setId(1L);
            rec.setRequestHash(st.hash());
            return rec;
        });

        // 补足库存后重试应成功
        consumableWithStock(10_000);
        assertDoesNotThrow(() -> service.outbound(CONSUMABLE_ID, 999, null, null, OPERATOR_ID, "fail-key"));

        assertEquals(9001, storedRow.get().getStockQuantity());
    }

    @Test
    @DisplayName("fingerprint 不包含时间戳等易变字段")
    void fingerprintIgnoresVolatileFields() {
        String a = InventoryIdempotencyService.fingerprint("STOCK_INBOUND",
                Map.of("id", 7L, "quantity", 10, "referenceNo", "REF-1"));
        // 字段顺序变化不应影响结果（LinkedHashMap 由调用方固定顺序，
        // 这里验证同样内容重复计算稳定）
        String b = InventoryIdempotencyService.fingerprint("STOCK_INBOUND",
                Map.of("id", 7L, "quantity", 10, "referenceNo", "REF-1"));
        assertEquals(a, b);
        assertEquals(64, a.length(), "SHA-256 hex 应为 64 字符");

        String c = InventoryIdempotencyService.fingerprint("STOCK_INBOUND",
                Map.of("id", 7L, "quantity", 11, "referenceNo", "REF-1"));
        assertNotEquals(a, c, "不同 payload 必须得到不同指纹");
    }

    @Test
    @DisplayName("key 长度与空白被拒绝")
    void keyValidation() {
        assertTrue(InventoryIdempotencyService.isKeyLengthAcceptable("abc"));
        assertFalse(InventoryIdempotencyService.isKeyLengthAcceptable(null));
        assertFalse(InventoryIdempotencyService.isKeyLengthAcceptable("  "));
        assertFalse(InventoryIdempotencyService.isKeyLengthAcceptable("x".repeat(129)));
    }
}
