package com.zyy.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.asset.mapper.MaintenancePlanMapper;
import com.zyy.asset.model.dto.MaintenancePlanSaveDTO;
import com.zyy.asset.model.dto.MaintenancePlanUpdateDTO;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.asset.model.entity.MaintenancePlanEntity;
import com.zyy.asset.model.vo.MaintenancePlanVO;
import com.zyy.common.PageVO;
import com.zyy.asset.service.MaintenancePlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of maintenance plan business service.
 * <p>
 * Provides maintenance plan lifecycle management including creation,
 * status transitions, and equipment association validation.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenancePlanServiceImpl implements MaintenancePlanService {

    private final MaintenancePlanMapper maintenancePlanMapper;
    private final EquipmentMapper equipmentMapper;

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaintenancePlanVO save(MaintenancePlanSaveDTO saveDTO, Long operatorId) {
        // Validate equipment exists
        EquipmentEntity equipment = equipmentMapper.selectById(saveDTO.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException("Equipment not found: " + saveDTO.getEquipmentId());
        }

        MaintenancePlanEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : "PENDING");

        maintenancePlanMapper.insert(entity);
        log.info("Maintenance plan created - id={}, equipmentId={}, operatorId={}",
                entity.getId(), entity.getEquipmentId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaintenancePlanVO update(MaintenancePlanUpdateDTO updateDTO, Long operatorId) {
        MaintenancePlanEntity entity = maintenancePlanMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Maintenance plan not found: " + updateDTO.getId());
        }

        if (updateDTO.getPlanName() != null) entity.setPlanName(updateDTO.getPlanName());
        if (updateDTO.getPlanType() != null) entity.setPlanType(updateDTO.getPlanType());
        if (updateDTO.getDescription() != null) entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getScheduledDate() != null) entity.setScheduledDate(updateDTO.getScheduledDate());
        if (updateDTO.getCompletedDate() != null) entity.setCompletedDate(updateDTO.getCompletedDate());
        if (updateDTO.getAssignedTo() != null) entity.setAssignedTo(updateDTO.getAssignedTo());
        if (updateDTO.getPriority() != null) entity.setPriority(updateDTO.getPriority());
        if (updateDTO.getCost() != null) entity.setCost(updateDTO.getCost());
        if (updateDTO.getRemarks() != null) entity.setRemarks(updateDTO.getRemarks());

        maintenancePlanMapper.updateById(entity);
        log.info("Maintenance plan updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public MaintenancePlanVO getById(Long id) {
        MaintenancePlanEntity entity = maintenancePlanMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    public PageVO<MaintenancePlanVO> getPage(Long pageNum, Long pageSize, Long equipmentId, String status) {
        Page<MaintenancePlanEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaintenancePlanEntity> query = new LambdaQueryWrapper<>();
        query.eq(MaintenancePlanEntity::getIsDeleted, 0);

        if (equipmentId != null) {
            query.eq(MaintenancePlanEntity::getEquipmentId, equipmentId);
        }
        if (status != null && !status.isBlank()) {
            query.eq(MaintenancePlanEntity::getStatus, status);
        }

        query.orderByDesc(MaintenancePlanEntity::getCreateTime);
        Page<MaintenancePlanEntity> result = maintenancePlanMapper.selectPage(page, query);

        List<MaintenancePlanVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<MaintenancePlanVO>builder()
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
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException("Invalid status: " + status);
        }

        MaintenancePlanEntity entity = maintenancePlanMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Maintenance plan not found: " + id);
        }

        // Validate status transition
        validateStatusTransition(entity.getStatus(), status);

        entity.setStatus(status);

        // Auto-set completed date when completing
        if ("COMPLETED".equals(status) && entity.getCompletedDate() == null) {
            entity.setCompletedDate(LocalDate.now());
        }

        maintenancePlanMapper.updateById(entity);
        log.info("Maintenance plan status changed - id={}, oldStatus={}, newStatus={}, operatorId={}",
                id, entity.getStatus(), status, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        maintenancePlanMapper.deleteById(id);
        log.info("Maintenance plan deleted (soft) - id={}, operatorId={}", id, operatorId);
    }

    // ==================== Private Helper Methods ====================

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) {
            return;
        }

        // COMPLETED and CANCELLED are terminal states
        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            throw new BusinessException("Cannot change status of a " + currentStatus.toLowerCase() + " plan");
        }

        // PENDING -> IN_PROGRESS, COMPLETED, CANCELLED
        // IN_PROGRESS -> COMPLETED, CANCELLED
        boolean valid = switch (currentStatus) {
            case "PENDING" -> Set.of("IN_PROGRESS", "COMPLETED", "CANCELLED").contains(newStatus);
            case "IN_PROGRESS" -> Set.of("COMPLETED", "CANCELLED").contains(newStatus);
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(String.format(
                    "Invalid status transition: %s -> %s", currentStatus, newStatus));
        }
    }

    private MaintenancePlanEntity toEntity(MaintenancePlanSaveDTO dto) {
        MaintenancePlanEntity entity = new MaintenancePlanEntity();
        entity.setEquipmentId(dto.getEquipmentId());
        entity.setPlanName(dto.getPlanName());
        entity.setPlanType(dto.getPlanType());
        entity.setDescription(dto.getDescription());
        entity.setScheduledDate(dto.getScheduledDate());
        entity.setAssignedTo(dto.getAssignedTo());
        entity.setPriority(dto.getPriority());
        entity.setCost(dto.getCost());
        entity.setRemarks(dto.getRemarks());
        return entity;
    }

    private MaintenancePlanVO entityToVO(MaintenancePlanEntity entity) {
        if (entity == null) return null;

        // Resolve equipment name
        String equipmentName = null;
        if (entity.getEquipmentId() != null) {
            EquipmentEntity equipment = equipmentMapper.selectById(entity.getEquipmentId());
            if (equipment != null) {
                equipmentName = equipment.getName();
            }
        }

        return MaintenancePlanVO.builder()
                .id(entity.getId())
                .equipmentId(entity.getEquipmentId())
                .equipmentName(equipmentName)
                .planName(entity.getPlanName())
                .planType(entity.getPlanType())
                .description(entity.getDescription())
                .scheduledDate(entity.getScheduledDate())
                .completedDate(entity.getCompletedDate())
                .assignedTo(entity.getAssignedTo())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .cost(entity.getCost())
                .remarks(entity.getRemarks())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
