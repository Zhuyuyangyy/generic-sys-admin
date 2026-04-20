package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.mapper.EquipmentMapper;
import com.zyy.model.dto.EquipmentSaveDTO;
import com.zyy.model.dto.EquipmentUpdateDTO;
import com.zyy.model.entity.EquipmentEntity;
import com.zyy.model.vo.EquipmentVO;
import com.zyy.model.vo.PageVO;
import com.zyy.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of equipment management business service.
 * <p>
 * Provides equipment lifecycle management including registration,
 * maintenance scheduling, and status state machine transitions.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;

    /**
     * Valid state transitions for equipment status:
     * <ul>
     *   <li>0 (Maintenance)  鈫?1 (Normal) or 2 (Scrapped)</li>
     *   <li>1 (Normal)      鈫?0 (Maintenance) or 2 (Scrapped)</li>
     *   <li>2 (Scrapped)    鈫?(terminal state, no transitions allowed)</li>
     * </ul>
     */
    private static final Set<Integer> VALID_FROM_NORMAL = Set.of(0, 2);
    private static final Set<Integer> VALID_FROM_MAINTENANCE = Set.of(1, 2);
    private static final Set<Integer> SCRAPPED_TARGETS = Set.of();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EquipmentVO save(EquipmentSaveDTO saveDTO, Long operatorId) {
        // Check equipment code uniqueness
        long existing = equipmentMapper.selectCount(
                new LambdaQueryWrapper<EquipmentEntity>()
                        .eq(EquipmentEntity::getEquipmentCode, saveDTO.getEquipmentCode())
                        .eq(EquipmentEntity::getIsDeleted, 0)
        );
        if (existing > 0) {
            throw new BusinessException("Equipment code already exists: " + saveDTO.getEquipmentCode());
        }

        EquipmentEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        entity.setStatus(1); // Default to Normal

        // Auto-calculate next maintenance date if cycle is set
        if (entity.getMaintenanceCycleDays() != null && entity.getPurchaseDate() != null) {
            entity.setNextMaintenanceDate(
                    entity.getPurchaseDate().plusDays(entity.getMaintenanceCycleDays())
            );
        }

        equipmentMapper.insert(entity);
        log.info("Equipment registered - id={}, code={}, operatorId={}",
                entity.getId(), entity.getEquipmentCode(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EquipmentVO update(EquipmentUpdateDTO updateDTO, Long operatorId) {
        EquipmentEntity entity = equipmentMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Equipment not found: " + updateDTO.getId());
        }

        if (updateDTO.getName() != null) entity.setName(updateDTO.getName());
        if (updateDTO.getCategory() != null) entity.setCategory(updateDTO.getCategory());
        if (updateDTO.getModel() != null) entity.setModel(updateDTO.getModel());
        if (updateDTO.getManufacturer() != null) entity.setManufacturer(updateDTO.getManufacturer());
        if (updateDTO.getPurchaseDate() != null) entity.setPurchaseDate(updateDTO.getPurchaseDate());
        if (updateDTO.getWarrantyExpiry() != null) entity.setWarrantyExpiry(updateDTO.getWarrantyExpiry());
        if (updateDTO.getLocation() != null) entity.setLocation(updateDTO.getLocation());
        if (updateDTO.getMaintenanceCycleDays() != null) entity.setMaintenanceCycleDays(updateDTO.getMaintenanceCycleDays());
        if (updateDTO.getNextMaintenanceDate() != null) entity.setNextMaintenanceDate(updateDTO.getNextMaintenanceDate());
        if (updateDTO.getRemarks() != null) entity.setRemarks(updateDTO.getRemarks());

        // Status update requires explicit validation via updateStatus()
        if (updateDTO.getStatus() != null) {
            validateStatusTransition(entity.getStatus(), updateDTO.getStatus());
            entity.setStatus(updateDTO.getStatus());
        }

        equipmentMapper.updateById(entity);
        log.info("Equipment updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public EquipmentVO getById(Long id) {
        EquipmentEntity entity = equipmentMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    public PageVO<EquipmentVO> getPage(Long pageNum, Long pageSize, String name, String category, Integer status) {
        Page<EquipmentEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EquipmentEntity> query = new LambdaQueryWrapper<>();
        query.eq(EquipmentEntity::getIsDeleted, 0);

        if (name != null && !name.isBlank()) {
            query.like(EquipmentEntity::getName, name);
        }
        if (category != null && !category.isBlank()) {
            query.eq(EquipmentEntity::getCategory, category);
        }
        if (status != null) {
            query.eq(EquipmentEntity::getStatus, status);
        }

        query.orderByDesc(EquipmentEntity::getCreateTime);
        Page<EquipmentEntity> result = equipmentMapper.selectPage(page, query);

        List<EquipmentVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<EquipmentVO>builder()
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
    public void updateStatus(Long id, Integer newStatus, Long operatorId) {
        EquipmentEntity entity = equipmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Equipment not found: " + id);
        }

        validateStatusTransition(entity.getStatus(), newStatus);
        entity.setStatus(newStatus);
        equipmentMapper.updateById(entity);

        log.info("Equipment status changed - id={}, oldStatus={}, newStatus={}, operatorId={}",
                id, entity.getStatus(), newStatus, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        equipmentMapper.deleteById(id);
        log.info("Equipment deleted (soft) - id={}, operatorId={}", id, operatorId);
    }

    // ==================== Private Helper Methods ====================

    private void validateStatusTransition(Integer currentStatus, Integer newStatus) {
        if (currentStatus == null || newStatus == null) {
            throw new BusinessException("Invalid status: both current and new status must be provided");
        }
        if (currentStatus.equals(newStatus)) {
            return; // No-op, no validation needed
        }
        if (currentStatus == 2) {
            throw new BusinessException("Cannot change status of scrapped equipment");
        }

        boolean valid = switch (currentStatus) {
            case 0 -> VALID_FROM_MAINTENANCE.contains(newStatus);
            case 1 -> VALID_FROM_NORMAL.contains(newStatus);
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(String.format(
                    "Invalid status transition: %d 鈫?%d", currentStatus, newStatus));
        }
    }

    private EquipmentEntity toEntity(EquipmentSaveDTO dto) {
        EquipmentEntity entity = new EquipmentEntity();
        entity.setEquipmentCode(dto.getEquipmentCode());
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setModel(dto.getModel());
        entity.setManufacturer(dto.getManufacturer());
        entity.setPurchaseDate(dto.getPurchaseDate());
        entity.setWarrantyExpiry(dto.getWarrantyExpiry());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setLocation(dto.getLocation());
        entity.setMaintenanceCycleDays(dto.getMaintenanceCycleDays());
        entity.setNextMaintenanceDate(dto.getNextMaintenanceDate());
        entity.setRemarks(dto.getRemarks());
        return entity;
    }

    private EquipmentVO entityToVO(EquipmentEntity entity) {
        if (entity == null) return null;

        LocalDate today = LocalDate.now();
        boolean maintenanceOverdue = entity.getNextMaintenanceDate() != null
                && entity.getNextMaintenanceDate().isBefore(today)
                && entity.getStatus() != 2;
        boolean warrantyExpired = entity.getWarrantyExpiry() != null
                && entity.getWarrantyExpiry().isBefore(today);

        String statusText = switch (entity.getStatus()) {
            case 0 -> "Under Maintenance";
            case 1 -> "Normal";
            case 2 -> "Scrapped";
            default -> "Unknown";
        };

        return EquipmentVO.builder()
                .id(entity.getId())
                .equipmentCode(entity.getEquipmentCode())
                .name(entity.getName())
                .category(entity.getCategory())
                .model(entity.getModel())
                .manufacturer(entity.getManufacturer())
                .purchaseDate(entity.getPurchaseDate())
                .warrantyExpiry(entity.getWarrantyExpiry())
                .status(entity.getStatus())
                .statusText(statusText)
                .location(entity.getLocation())
                .maintenanceCycleDays(entity.getMaintenanceCycleDays())
                .nextMaintenanceDate(entity.getNextMaintenanceDate())
                .maintenanceOverdue(maintenanceOverdue)
                .warrantyExpired(warrantyExpired)
                .remarks(entity.getRemarks())
                .createTime(entity.getCreateTime())
                .build();
    }
}
