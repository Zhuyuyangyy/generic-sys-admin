package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.mapper.ConsumableMapper;
import com.zyy.mapper.InventoryTransactionMapper;
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
        if (delta == null || delta == 0) {
            throw new BusinessException("Stock delta must be non-zero");
        }
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Consumable not found: " + id);
        }

        // 原子自增：UPDATE ... SET stock = stock + delta。
        // 不用 selectById 出来的值算好再 updateById，否则并发下会 lost update
        // （两个请求读到同一 stock，后写的把前一个覆盖掉）。
        // stock >= 0 由 UPDATE 的 WHERE 保证，并发出库也不可能扣成负数。
        int updated = consumableMapper.adjustStockAtomic(id, delta);
        if (updated == 0) {
            throw new BusinessException("Insufficient stock. Current: "
                    + entity.getStockQuantity() + ", Requested: " + Math.abs(delta));
        }

        int newStock = entity.getStockQuantity() + delta;

        recordTransaction(id, "ADJUSTMENT", delta, newStock, referenceNo, remarks, operatorId);

        log.info("Consumable stock adjusted - id={}, delta={}, newStock={}, operatorId={}",
                id, delta, newStock, operatorId);

        entity.setStockQuantity(newStock);
        checkStockAlerts(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long id, Integer quantity, String referenceNo, String remarks, Long operatorId) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Inbound quantity must be positive");
        }
        adjustStock(id, quantity, referenceNo, remarks, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(Long id, Integer quantity, String referenceNo, String remarks, Long operatorId) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Outbound quantity must be positive");
        }
        adjustStock(id, -quantity, referenceNo, remarks, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        consumableMapper.deleteById(id);
        log.info("Consumable deleted (soft) - id={}, operatorId={}", id, operatorId);
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
