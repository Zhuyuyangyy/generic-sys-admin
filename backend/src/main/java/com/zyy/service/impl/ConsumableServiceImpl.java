package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.mapper.ConsumableMapper;
import com.zyy.mapper.InventoryTransactionMapper;
import com.zyy.service.InventoryIdempotencyService;
import com.zyy.model.dto.ConsumableSaveDTO;
import com.zyy.model.dto.ConsumableUpdateDTO;
import com.zyy.model.entity.ConsumableEntity;
import com.zyy.model.entity.InventoryTransactionEntity;
import com.zyy.model.vo.ConsumableVO;
import com.zyy.model.vo.PageVO;
import com.zyy.service.ConsumableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of consumable inventory management service.
 * <p>
 * Handles consumable lifecycle including stock level monitoring,
 * low-stock alerting, and inventory transaction recording.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumableServiceImpl implements ConsumableService {

    private final ConsumableMapper consumableMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final InventoryIdempotencyService idempotencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumableVO save(ConsumableSaveDTO saveDTO, Long operatorId) {
        long existing = consumableMapper.selectCount(
                new LambdaQueryWrapper<ConsumableEntity>()
                        .eq(ConsumableEntity::getProductCode, saveDTO.getProductCode())
                        .eq(ConsumableEntity::getIsDeleted, 0)
        );
        if (existing > 0) {
            throw new BusinessException("Product code already exists: " + saveDTO.getProductCode());
        }

        ConsumableEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        consumableMapper.insert(entity);

        log.info("Consumable registered - id={}, productCode={}, operatorId={}",
                entity.getId(), entity.getProductCode(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumableVO update(ConsumableUpdateDTO updateDTO, Long operatorId) {
        ConsumableEntity entity = consumableMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Consumable not found: " + updateDTO.getId());
        }

        if (updateDTO.getName() != null) entity.setName(updateDTO.getName());
        if (updateDTO.getCategory() != null) entity.setCategory(updateDTO.getCategory());
        if (updateDTO.getUnit() != null) entity.setUnit(updateDTO.getUnit());
        if (updateDTO.getMinStockLevel() != null) entity.setMinStockLevel(updateDTO.getMinStockLevel());
        if (updateDTO.getMaxStockLevel() != null) entity.setMaxStockLevel(updateDTO.getMaxStockLevel());
        if (updateDTO.getUnitCost() != null) entity.setUnitCost(updateDTO.getUnitCost());
        if (updateDTO.getExpirationDate() != null) entity.setExpirationDate(updateDTO.getExpirationDate());
        if (updateDTO.getSupplier() != null) entity.setSupplier(updateDTO.getSupplier());
        if (updateDTO.getStorageLocation() != null) entity.setStorageLocation(updateDTO.getStorageLocation());
        if (updateDTO.getStatus() != null) entity.setStatus(updateDTO.getStatus());
        if (updateDTO.getReorderPoint() != null) entity.setReorderPoint(updateDTO.getReorderPoint());
        if (updateDTO.getLastCheckDate() != null) entity.setLastCheckDate(updateDTO.getLastCheckDate());
        if (updateDTO.getRemarks() != null) entity.setRemarks(updateDTO.getRemarks());

        consumableMapper.updateById(entity);
        log.info("Consumable updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public ConsumableVO getById(Long id) {
        ConsumableEntity entity = consumableMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    public PageVO<ConsumableVO> getPage(Long pageNum, Long pageSize, String name, String category, Integer status) {
        Page<ConsumableEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConsumableEntity> query = new LambdaQueryWrapper<>();
        query.eq(ConsumableEntity::getIsDeleted, 0);

        if (name != null && !name.isBlank()) {
            query.like(ConsumableEntity::getName, name);
        }
        if (category != null && !category.isBlank()) {
            query.eq(ConsumableEntity::getCategory, category);
        }
        if (status != null) {
            query.eq(ConsumableEntity::getStatus, status);
        }

        query.orderByDesc(ConsumableEntity::getCreateTime);
        Page<ConsumableEntity> result = consumableMapper.selectPage(page, query);

        List<ConsumableVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<ConsumableVO>builder()
                .items(voList)
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .isFirst(result.getCurrent() == 1)
                .isLast(result.getCurrent() >= result.getPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long id, Integer delta, String referenceNo, String remarks, Long operatorId) {
        adjustStock(id, delta, referenceNo, remarks, operatorId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long id, Integer delta, String referenceNo, String remarks,
                            Long operatorId, String idempotencyKey) {
        if (delta == null || delta == 0) {
            throw new BusinessException("Stock delta must be non-zero");
        }
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Consumable not found: " + id);
        }

        // ── 幂等控制 ────────────────────────────────────────────────
        // claim 与下面的 stock 更新、流水插入处于同一事务：任一步回滚，三者
        // 一起消失，因此失败后的重试不会被半条 claim 永久锁死。
        Long recordId = claimIdempotency(operatorId, InventoryIdempotencyService.OP_ADJUST,
                idempotencyKey,
                orderedPayload("id", id, "delta", delta, "referenceNo", referenceNo));

        // ── 原子自增 ────────────────────────────────────────────────
        // 不用 selectById 出来的值算好再 updateById，否则并发下会 lost update。
        // stock >= 0 由 UPDATE 的 WHERE 保证，并发出库也不可能扣成负数。
        applyStockDelta(entity, id, delta, referenceNo, remarks, operatorId, recordId,
                "ADJUSTMENT", "ADJUSTMENT:" + id + ":" + delta);
    }

    /**
     * 唯一真正改变库存的位置。
     *
     * <p>原子自增 + 流水插入，全部在调用方事务内。幂等 claim 在此之前取得、
     * 在此之后完成，因此三者同生同灭。</p>
     */
    private void applyStockDelta(ConsumableEntity entity, Long id, int delta,
                                 String referenceNo, String remarks, Long operatorId,
                                 Long recordId, String ledgerType, String resultReference) {
        int updated = consumableMapper.adjustStockAtomic(id, delta);
        if (updated == 0) {
            idempotencyService.markFailed(recordId, "Insufficient stock");
            throw new BusinessException("Insufficient stock. Current: "
                    + entity.getStockQuantity() + ", Requested: " + Math.abs(delta));
        }

        int newStock = entity.getStockQuantity() + delta;

        recordTransaction(id, ledgerType, delta, newStock, referenceNo, remarks, operatorId);

        log.info("Consumable stock {} - id={}, delta={}, newStock={}, operatorId={}",
                ledgerType, id, delta, newStock, operatorId);

        entity.setStockQuantity(newStock);
        checkStockAlerts(entity);

        idempotencyService.markSuccess(recordId, resultReference);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long id, Integer quantity, String referenceNo, String remarks, Long operatorId) {
        inbound(id, quantity, referenceNo, remarks, operatorId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long id, Integer quantity, String referenceNo, String remarks,
                       Long operatorId, String idempotencyKey) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Inbound quantity must be positive");
        }
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Consumable not found: " + id);
        }
        Long recordId = claimIdempotency(operatorId, InventoryIdempotencyService.OP_INBOUND,
                idempotencyKey,
                orderedPayload("id", id, "quantity", quantity, "referenceNo", referenceNo));

        applyStockDelta(entity, id, quantity, referenceNo, remarks, operatorId, recordId,
                "INBOUND", "INBOUND:" + id + ":" + quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(Long id, Integer quantity, String referenceNo, String remarks, Long operatorId) {
        outbound(id, quantity, referenceNo, remarks, operatorId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(Long id, Integer quantity, String referenceNo, String remarks,
                         Long operatorId, String idempotencyKey) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Outbound quantity must be positive");
        }
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Consumable not found: " + id);
        }
        Long recordId = claimIdempotency(operatorId, InventoryIdempotencyService.OP_OUTBOUND,
                idempotencyKey,
                orderedPayload("id", id, "quantity", quantity, "referenceNo", referenceNo));

        applyStockDelta(entity, id, -quantity, referenceNo, remarks, operatorId, recordId,
                "OUTBOUND", "OUTBOUND:" + id + ":" + quantity);
    }

    /**
     * 取得幂等 key 的所有权；未提供 key 时直接放行（返回 null，不做任何记录）。
     *
     * <p>相同 key + 相同 payload 已成功时按重放处理：返回 ALREADY_SUCCEEDED，
     * 由调用方转为可读结果，不重复执行业务变更。相同 key + 不同 payload 判定为
     * key 被复用，抛冲突异常（Controller 映射为 409）。</p>
     */
    private Long claimIdempotency(Long operatorId, String operation, String idempotencyKey,
                                  Map<String, Object> payload) {
        // payload 允许 value 为 null（例如 referenceNo 未填），用 LinkedHashMap
        // 之外再包一层，Map.of 不接受 null 会让普通请求直接 500。
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        if (!InventoryIdempotencyService.isKeyLengthAcceptable(idempotencyKey)) {
            throw new BusinessException("Idempotency-Key 长度必须在 1.."
                    + InventoryIdempotencyService.MAX_KEY_LENGTH + " 字符之间");
        }
        if (operatorId == null) {
            // 没有调用方标识就无法界定 key 归属，宁可拒绝也不猜。
            throw new BusinessException("无法确定操作人，拒绝使用 Idempotency-Key");
        }

        String hash = InventoryIdempotencyService.fingerprint(operation, payload);
        InventoryIdempotencyService.Claim claim =
                idempotencyService.claim(operatorId, operation, idempotencyKey, hash);

        switch (claim.outcome()) {
            case CLAIMED -> {
                return claim.recordId();
            }
            case ALREADY_SUCCEEDED -> {
                // 重放：上一次已成功提交，本次必须【完全不执行业务变更】。
                // 抛出一个专用信号，Controller 捕获后返回首次结果，
                // 绝不能让流程继续走到库存自增。
                log.info("Idempotent replay: op={} key={} reference={}",
                        operation, idempotencyKey, claim.resultReference());
                throw new ReplayedRequestException(claim.resultReference());
            }
            case PREVIOUSLY_FAILED -> {
                // 上一次失败且事务已回滚，key 未被占用，允许重试。
                log.info("Idempotency retry after failure: op={} key={} reason={}",
                        operation, idempotencyKey, claim.errorDetail());
                return retryClaim(operatorId, operation, idempotencyKey, hash);
            }
            case ALREADY_PROCESSING -> throw new BusinessException(
                    "相同 Idempotency-Key 的请求正在处理中，请稍后再试");
            case KEY_CONFLICT -> throw new InventoryIdempotencyService
                    .IdempotencyKeyConflictException(
                    "Idempotency-Key 已被不同请求使用");
            default -> throw new BusinessException("无法确认 Idempotency-Key 状态");
        }
    }

    /**
     * FAILED 记录重试：先把旧行标记为已替换，再重新 claim。
     * 复用同一 (operator, operation, key) 需要先删掉旧行，否则唯一约束会挡住。
     */
    private Long retryClaim(Long operatorId, String operation, String idempotencyKey, String hash) {
        idempotencyService.deleteFailed(operatorId, operation, idempotencyKey);
        InventoryIdempotencyService.Claim retry =
                idempotencyService.claim(operatorId, operation, idempotencyKey, hash);
        if (retry.outcome() == InventoryIdempotencyService.Outcome.CLAIMED) {
            return retry.recordId();
        }
        throw new BusinessException("Idempotency-Key 状态异常：" + retry.outcome());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        consumableMapper.deleteById(id);
        log.info("Consumable deleted (soft) - id={}, operatorId={}", id, operatorId);
    }


    /**
     * 重放信号：同一 Idempotency-Key + 同一 payload 的请求已经成功提交过。
     *
     * <p>抛出它是为了在库存自增【之前】中断流程；Carrier 携带首次调用的结果
     * 引用，Controller 据此返回一致的响应。它不是错误 —— 是"这次不用再做
     * 一遍"。</p>
     */
    public static class ReplayedRequestException extends RuntimeException {
        private final String resultReference;

        public ReplayedRequestException(String resultReference) {
            super("请求已处理（幂等重放）");
            this.resultReference = resultReference;
        }

        public String getResultReference() {
            return resultReference;
        }
    }


    /** 构造有序且允许 null value 的 payload map（Map.of 不接受 null）。 */
    private static Map<String, Object> orderedPayload(String k1, Object v1,
                                                      String k2, Object v2,
                                                      String k3, Object v3) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    // ==================== Private Helper Methods ====================

    private void recordTransaction(Long consumableId, String type, Integer quantity,
                                    Integer balanceAfter, String referenceNo, String remarks, Long operatorId) {
        InventoryTransactionEntity tx = new InventoryTransactionEntity();
        tx.setConsumableId(consumableId);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceNo(referenceNo);
        tx.setOperatorId(operatorId);
        tx.setRemarks(remarks);
        transactionMapper.insert(tx);
    }

    private void checkStockAlerts(ConsumableEntity entity) {
        if (entity.getMinStockLevel() != null && entity.getStockQuantity() < entity.getMinStockLevel()) {
            log.warn("Low stock alert - productCode={}, name={}, currentStock={}, minStock={}",
                    entity.getProductCode(), entity.getName(),
                    entity.getStockQuantity(), entity.getMinStockLevel());
        }
    }

    private ConsumableEntity toEntity(ConsumableSaveDTO dto) {
        ConsumableEntity e = new ConsumableEntity();
        e.setProductCode(dto.getProductCode());
        e.setName(dto.getName());
        e.setCategory(dto.getCategory());
        e.setUnit(dto.getUnit() != null ? dto.getUnit() : "piece");
        e.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        e.setMinStockLevel(dto.getMinStockLevel() != null ? dto.getMinStockLevel() : 0);
        e.setMaxStockLevel(dto.getMaxStockLevel());
        e.setUnitCost(dto.getUnitCost());
        e.setExpirationDate(dto.getExpirationDate());
        e.setSupplier(dto.getSupplier());
        e.setStorageLocation(dto.getStorageLocation());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        e.setReorderPoint(dto.getReorderPoint());
        e.setLastCheckDate(dto.getLastCheckDate());
        e.setRemarks(dto.getRemarks());
        return e;
    }

    private ConsumableVO entityToVO(ConsumableEntity entity) {
        if (entity == null) return null;

        LocalDate today = LocalDate.now();
        boolean lowStock = entity.getMinStockLevel() != null
                && entity.getStockQuantity() < entity.getMinStockLevel();
        boolean expiring = entity.getExpirationDate() != null
                && !entity.getExpirationDate().isAfter(today.plusDays(30));

        String statusText = entity.getStatus() == 0 ? "Unavailable" : "Available";

        return ConsumableVO.builder()
                .id(entity.getId())
                .productCode(entity.getProductCode())
                .name(entity.getName())
                .category(entity.getCategory())
                .unit(entity.getUnit())
                .stockQuantity(entity.getStockQuantity())
                .minStockLevel(entity.getMinStockLevel())
                .maxStockLevel(entity.getMaxStockLevel())
                .unitCost(entity.getUnitCost())
                .expirationDate(entity.getExpirationDate())
                .supplier(entity.getSupplier())
                .storageLocation(entity.getStorageLocation())
                .status(entity.getStatus())
                .statusText(statusText)
                .reorderPoint(entity.getReorderPoint())
                .lastCheckDate(entity.getLastCheckDate())
                .lowStockAlert(lowStock)
                .expiringAlert(expiring)
                .remarks(entity.getRemarks())
                .createTime(entity.getCreateTime())
                .build();
    }
}
