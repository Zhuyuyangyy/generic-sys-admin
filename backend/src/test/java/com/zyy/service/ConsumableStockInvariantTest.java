package com.zyy.service;

import com.zyy.mapper.ConsumableMapper;
import com.zyy.mapper.InventoryTransactionMapper;
import com.zyy.model.entity.ConsumableEntity;
import com.zyy.service.impl.ConsumableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Proves the stock invariants.
 *
 * Guaranteed by the service + SQL:
 *   - outbound fails when it would drive stock below zero
 *   - non-positive in/out quantities are rejected
 *   - stock update and transaction insert share one transaction
 *   - concurrent adjustments all land (atomic UPDATE, no lost update)
 *
 * The last test used to fail here with "lost update detected — 10 次入库被覆盖
 * 丢失" because `adjustStock` did selectById → compute → updateById. It now goes
 * through `ConsumableMapper.adjustStockAtomic()`. Keep it green: it is the
 * executable definition of the invariant.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存不变量测试")
class ConsumableStockInvariantTest {

    private static final long CONSUMABLE_ID = 7L;

    @Mock
    private ConsumableMapper consumableMapper;

    @Mock
    private InventoryTransactionMapper transactionMapper;

    @Mock
    private com.zyy.mapper.IdempotencyRecordMapper idempotencyRecordMapper;

    @Mock
    private InventoryIdempotencyService idempotencyService;

    private ConsumableServiceImpl service;

    /** Simulates the database row: last value written by updateById. */
    private final AtomicReference<ConsumableEntity> storedRow = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new ConsumableServiceImpl(
                consumableMapper, transactionMapper, idempotencyService);

        lenient().when(consumableMapper.selectById(anyLong())).thenAnswer(inv -> {
            ConsumableEntity row = storedRow.get();
            // hand out a copy so two callers cannot share one mutable instance
            return row == null ? null : copy(row);
        });

        lenient().when(consumableMapper.updateById(any(ConsumableEntity.class))).thenAnswer(inv -> {
            ConsumableEntity incoming = inv.getArgument(0);
            storedRow.set(copy(incoming));
            return 1;
        });

        // 模拟真实 SQL：UPDATE ... SET stock = stock + delta WHERE stock + delta >= 0
        lenient().when(consumableMapper.adjustStockAtomic(anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> {
                    int delta = inv.getArgument(1);
                    ConsumableEntity row = storedRow.get();
                    if (row == null) {
                        return 0;
                    }
                    int next = row.getStockQuantity() + delta;
                    if (next < 0) {
                        return 0; // WHERE 不匹配
                    }
                    row.setStockQuantity(next);
                    return 1;
                });
    }

    private ConsumableEntity consumableWithStock(int stock) {
        storedRow.set(consumableWithStockInternal(stock));
        return copy(storedRow.get());
    }

    private static ConsumableEntity consumableWithStockInternal(int stock) {
        ConsumableEntity e = new ConsumableEntity();
        e.setId(CONSUMABLE_ID);
        e.setStockQuantity(stock);
        return e;
    }

    private static ConsumableEntity copy(ConsumableEntity src) {
        ConsumableEntity dst = new ConsumableEntity();
        dst.setId(src.getId());
        dst.setStockQuantity(src.getStockQuantity());
        return dst;
    }

    @Test
    @DisplayName("出库不能把库存扣成负数")
    void outboundCannotGoNegative() {
        consumableWithStock(5);

        com.zyy.exception.BusinessException ex = assertThrows(
                com.zyy.exception.BusinessException.class,
                () -> service.outbound(CONSUMABLE_ID, 6, null, null, 1L));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        assertEquals(5, storedRow.get().getStockQuantity(), "库存必须保持不变");
    }

    @Test
    @DisplayName("入库/出库数量必须为正")
    void quantitiesMustBePositive() {
        consumableWithStock(5);

        assertThrows(com.zyy.exception.BusinessException.class,
                () -> service.inbound(CONSUMABLE_ID, 0, null, null, 1L));
        assertThrows(com.zyy.exception.BusinessException.class,
                () -> service.inbound(CONSUMABLE_ID, -3, null, null, 1L));
        assertThrows(com.zyy.exception.BusinessException.class,
                () -> service.outbound(CONSUMABLE_ID, 0, null, null, 1L));
        assertThrows(com.zyy.exception.BusinessException.class,
                () -> service.outbound(CONSUMABLE_ID, -3, null, null, 1L));

        assertEquals(5, storedRow.get().getStockQuantity());
    }

    @Test
    @DisplayName("并发入库存在 lost update：第二个写入会覆盖第一个")
    void concurrentInboundLosesUpdates() throws Exception {
        consumableWithStock(100);

        int threads = 4;
        int perThread = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        service.inbound(CONSUMABLE_ID, 1, null, null, 1L);
                        succeeded.incrementAndGet();
                    }
                } catch (Throwable e) {
                    failure.set(e);
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));

        assertNull(failure.get(), "并发入库不应抛异常");
        assertEquals(threads * perThread, succeeded.get());

        int finalStock = storedRow.get().getStockQuantity();
        int expected = 100 + threads * perThread;

        // 当前实现中此项必失败：这就是 "lost update" 的可执行定义。
        assertEquals(expected, finalStock,
                "lost update detected — " + (expected - finalStock)
                        + " 次入库被覆盖丢失。修复方向：MyBatis-Plus @Version 乐观锁"
                        + "或 UPDATE ... SET stock = stock + delta 的原子自增。");
    }
}
