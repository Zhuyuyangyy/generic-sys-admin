package com.zyy.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.asset.mapper.InspectionMapper;
import com.zyy.asset.model.dto.InspectionSaveDTO;
import com.zyy.asset.model.dto.InspectionUpdateDTO;
import com.zyy.asset.model.entity.InspectionEntity;
import com.zyy.asset.model.vo.InspectionVO;
import com.zyy.common.PageVO;
import com.zyy.asset.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of equipment inspection business service.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private final InspectionMapper inspectionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionVO save(InspectionSaveDTO saveDTO, Long operatorId) {
        InspectionEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        inspectionMapper.insert(entity);

        log.info("Inspection created - id={}, equipmentId={}, result={}, operatorId={}",
                entity.getId(), entity.getEquipmentId(), entity.getResult(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionVO update(InspectionUpdateDTO updateDTO, Long operatorId) {
        InspectionEntity entity = inspectionMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Inspection not found: " + updateDTO.getId());
        }

        if (updateDTO.getInspectionType() != null) entity.setInspectionType(updateDTO.getInspectionType());
        if (updateDTO.getInspectionDate() != null) entity.setInspectionDate(updateDTO.getInspectionDate());
        if (updateDTO.getInspectorId() != null) entity.setInspectorId(updateDTO.getInspectorId());
        if (updateDTO.getInspectorName() != null) entity.setInspectorName(updateDTO.getInspectorName());
        if (updateDTO.getResult() != null) entity.setResult(updateDTO.getResult());
        if (updateDTO.getFindings() != null) entity.setFindings(updateDTO.getFindings());
        if (updateDTO.getCorrectiveAction() != null) entity.setCorrectiveAction(updateDTO.getCorrectiveAction());
        if (updateDTO.getNextInspectionDate() != null) entity.setNextInspectionDate(updateDTO.getNextInspectionDate());

        inspectionMapper.updateById(entity);
        log.info("Inspection updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public InspectionVO getById(Long id) {
        InspectionEntity entity = inspectionMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    public PageVO<InspectionVO> getPage(Long pageNum, Long pageSize, Long equipmentId, String result) {
        Page<InspectionEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InspectionEntity> query = new LambdaQueryWrapper<>();
        query.eq(InspectionEntity::getIsDeleted, 0);

        if (equipmentId != null) {
            query.eq(InspectionEntity::getEquipmentId, equipmentId);
        }
        if (result != null && !result.isBlank()) {
            query.eq(InspectionEntity::getResult, result);
        }

        query.orderByDesc(InspectionEntity::getCreateTime);
        Page<InspectionEntity> resultPage = inspectionMapper.selectPage(page, query);

        List<InspectionVO> voList = resultPage.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<InspectionVO>builder()
                .items(voList)
                .pageNum(resultPage.getCurrent())
                .pageSize(resultPage.getSize())
                .total(resultPage.getTotal())
                .pages(resultPage.getPages())
                .isFirst(resultPage.getCurrent() == 1)
                .isLast(resultPage.getCurrent() >= resultPage.getPages())
                .hasNext(resultPage.hasNext())
                .hasPrevious(resultPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        inspectionMapper.deleteById(id);
        log.info("Inspection deleted (soft) - id={}, operatorId={}", id, operatorId);
    }

    // ==================== Private Helper Methods ====================

    private InspectionEntity toEntity(InspectionSaveDTO dto) {
        InspectionEntity entity = new InspectionEntity();
        entity.setEquipmentId(dto.getEquipmentId());
        entity.setInspectionType(dto.getInspectionType());
        entity.setInspectionDate(dto.getInspectionDate());
        entity.setInspectorId(dto.getInspectorId());
        entity.setInspectorName(dto.getInspectorName());
        entity.setResult(dto.getResult());
        entity.setFindings(dto.getFindings());
        entity.setCorrectiveAction(dto.getCorrectiveAction());
        entity.setNextInspectionDate(dto.getNextInspectionDate());
        return entity;
    }

    private InspectionVO entityToVO(InspectionEntity entity) {
        if (entity == null) return null;

        return InspectionVO.builder()
                .id(entity.getId())
                .equipmentId(entity.getEquipmentId())
                .inspectionType(entity.getInspectionType())
                .inspectionDate(entity.getInspectionDate())
                .inspectorId(entity.getInspectorId())
                .inspectorName(entity.getInspectorName())
                .result(entity.getResult())
                .findings(entity.getFindings())
                .correctiveAction(entity.getCorrectiveAction())
                .nextInspectionDate(entity.getNextInspectionDate())
                .createTime(entity.getCreateTime())
                .build();
    }
}
