package com.zyy.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.asset.mapper.AssignmentMapper;
import com.zyy.asset.model.dto.AssignmentSaveDTO;
import com.zyy.asset.model.entity.AssignmentEntity;
import com.zyy.asset.model.vo.AssignmentVO;
import com.zyy.common.PageVO;
import com.zyy.asset.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of equipment assignment business service.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentMapper assignmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentVO save(AssignmentSaveDTO saveDTO, Long operatorId) {
        AssignmentEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        entity.setStatus("ACTIVE");
        assignmentMapper.insert(entity);

        log.info("Equipment assigned - id={}, equipmentId={}, assignedTo={}, operatorId={}",
                entity.getId(), entity.getEquipmentId(), entity.getAssignedToUserId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentVO returnEquipment(Long id, Long operatorId) {
        AssignmentEntity entity = assignmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Assignment not found: " + id);
        }

        if ("RETURNED".equals(entity.getStatus())) {
            throw new BusinessException("Equipment already returned");
        }

        entity.setStatus("RETURNED");
        entity.setReturnedDate(LocalDate.now());
        assignmentMapper.updateById(entity);

        log.info("Equipment returned - id={}, equipmentId={}, operatorId={}",
                entity.getId(), entity.getEquipmentId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public PageVO<AssignmentVO> getPage(Long pageNum, Long pageSize, Long equipmentId, Long userId, String status) {
        Page<AssignmentEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssignmentEntity> query = new LambdaQueryWrapper<>();
        query.eq(AssignmentEntity::getIsDeleted, 0);

        if (equipmentId != null) {
            query.eq(AssignmentEntity::getEquipmentId, equipmentId);
        }
        if (userId != null) {
            query.eq(AssignmentEntity::getAssignedToUserId, userId);
        }
        if (status != null && !status.isBlank()) {
            query.eq(AssignmentEntity::getStatus, status);
        }

        query.orderByDesc(AssignmentEntity::getCreateTime);
        Page<AssignmentEntity> result = assignmentMapper.selectPage(page, query);

        List<AssignmentVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<AssignmentVO>builder()
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
    public void delete(Long id, Long operatorId) {
        assignmentMapper.deleteById(id);
        log.info("Assignment deleted (soft) - id={}, operatorId={}", id, operatorId);
    }

    // ==================== Private Helper Methods ====================

    private AssignmentEntity toEntity(AssignmentSaveDTO dto) {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setEquipmentId(dto.getEquipmentId());
        entity.setAssignedToUserId(dto.getAssignedToUserId());
        entity.setAssignedToUserName(dto.getAssignedToUserName());
        entity.setAssignedByUserId(dto.getAssignedByUserId());
        entity.setAssignedDate(dto.getAssignedDate());
        entity.setAssignmentType(dto.getAssignmentType());
        entity.setRemarks(dto.getRemarks());
        return entity;
    }

    private AssignmentVO entityToVO(AssignmentEntity entity) {
        if (entity == null) return null;

        return AssignmentVO.builder()
                .id(entity.getId())
                .equipmentId(entity.getEquipmentId())
                .assignedToUserId(entity.getAssignedToUserId())
                .assignedToUserName(entity.getAssignedToUserName())
                .assignedByUserId(entity.getAssignedByUserId())
                .assignedDate(entity.getAssignedDate())
                .returnedDate(entity.getReturnedDate())
                .assignmentType(entity.getAssignmentType())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .createTime(entity.getCreateTime())
                .build();
    }
}
