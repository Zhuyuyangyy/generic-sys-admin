package com.zyy.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.inventory.mapper.BatchMapper;
import com.zyy.inventory.model.dto.BatchSaveDTO;
import com.zyy.inventory.model.dto.BatchUpdateDTO;
import com.zyy.inventory.model.entity.BatchEntity;
import com.zyy.inventory.model.vo.BatchVO;
import com.zyy.common.PageVO;
import com.zyy.inventory.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of consumable batch business service.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final BatchMapper batchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchVO save(BatchSaveDTO saveDTO, Long operatorId) {
        BatchEntity entity = toEntity(saveDTO);

        // Calculate total cost
        if (entity.getUnitCost() != null && entity.getQuantity() != null) {
            entity.setTotalCost(entity.getUnitCost().multiply(BigDecimal.valueOf(entity.getQuantity())));
        }

        // Set remaining quantity equal to initial quantity
        entity.setRemainingQuantity(entity.getQuantity());
        entity.setStatus("ACTIVE");
        entity.setCreateUser(operatorId);

        batchMapper.insert(entity);

        log.info("Batch created (inbound) - id={}, consumableId={}, batchNo={}, quantity={}, operatorId={}",
                entity.getId(), entity.getConsumableId(), entity.getBatchNo(), entity.getQuantity(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchVO update(BatchUpdateDTO updateDTO, Long operatorId) {
        BatchEntity entity = batchMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Batch not found: " + updateDTO.getId());
        }

        if (updateDTO.getBatchNo() != null) entity.setBatchNo(updateDTO.getBatchNo());
        if (updateDTO.getSupplierId() != null) entity.setSupplierId(updateDTO.getSupplierId());
        if (updateDTO.getUnitCost() != null) {
            entity.setUnitCost(updateDTO.getUnitCost());
            // Recalculate total cost
            if (entity.getQuantity() != null) {
                entity.setTotalCost(updateDTO.getUnitCost().multiply(BigDecimal.valueOf(entity.getQuantity())));
            }
        }
        if (updateDTO.getProductionDate() != null) entity.setProductionDate(updateDTO.getProductionDate());
        if (updateDTO.getExpirationDate() != null) entity.setExpirationDate(updateDTO.getExpirationDate());
        if (updateDTO.getStorageLocation() != null) entity.setStorageLocation(updateDTO.getStorageLocation());

        batchMapper.updateById(entity);
        log.info("Batch updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public BatchVO getById(Long id) {
        BatchEntity entity = batchMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    public PageVO<BatchVO> getPage(Long pageNum, Long pageSize, Long consumableId, String status) {
        Page<BatchEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BatchEntity> query = new LambdaQueryWrapper<>();
        query.eq(BatchEntity::getIsDeleted, 0);

        if (consumableId != null) {
            query.eq(BatchEntity::getConsumableId, consumableId);
        }
        if (status != null && !status.isBlank()) {
            query.eq(BatchEntity::getStatus, status);
        }

        query.orderByDesc(BatchEntity::getCreateTime);
        Page<BatchEntity> result = batchMapper.selectPage(page, query);

        List<BatchVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<BatchVO>builder()
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
    public void updateStatus(Long id, String status, Long operatorId) {
        BatchEntity entity = batchMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Batch not found: " + id);
        }

        entity.setStatus(status);
        batchMapper.updateById(entity);

        log.info("Batch status updated - id={}, newStatus={}, operatorId={}", id, status, operatorId);
    }

    @Override
    public List<BatchVO> getExpiringBatches(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        LambdaQueryWrapper<BatchEntity> query = new LambdaQueryWrapper<>();
        query.eq(BatchEntity::getIsDeleted, 0)
             .eq(BatchEntity::getStatus, "ACTIVE")
             .isNotNull(BatchEntity::getExpirationDate)
             .le(BatchEntity::getExpirationDate, threshold)
             .gt(BatchEntity::getExpirationDate, LocalDate.now())
             .orderByAsc(BatchEntity::getExpirationDate);

        List<BatchEntity> entities = batchMapper.selectList(query);
        return entities.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private BatchEntity toEntity(BatchSaveDTO dto) {
        BatchEntity entity = new BatchEntity();
        entity.setConsumableId(dto.getConsumableId());
        entity.setBatchNo(dto.getBatchNo());
        entity.setSupplierId(dto.getSupplierId());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitCost(dto.getUnitCost());
        entity.setProductionDate(dto.getProductionDate());
        entity.setExpirationDate(dto.getExpirationDate());
        entity.setStorageLocation(dto.getStorageLocation());
        return entity;
    }

    private BatchVO entityToVO(BatchEntity entity) {
        if (entity == null) return null;

        return BatchVO.builder()
                .id(entity.getId())
                .consumableId(entity.getConsumableId())
                .batchNo(entity.getBatchNo())
                .supplierId(entity.getSupplierId())
                .quantity(entity.getQuantity())
                .remainingQuantity(entity.getRemainingQuantity())
                .unitCost(entity.getUnitCost())
                .totalCost(entity.getTotalCost())
                .productionDate(entity.getProductionDate())
                .expirationDate(entity.getExpirationDate())
                .status(entity.getStatus())
                .storageLocation(entity.getStorageLocation())
                .createTime(entity.getCreateTime())
                .build();
    }
}
