package com.zyy.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.asset.mapper.EquipmentMapper;
import com.zyy.inventory.mapper.InventoryRecordMapper;
import com.zyy.inventory.model.dto.InventoryRecordSaveDTO;
import com.zyy.inventory.model.dto.InventoryRecordUpdateDTO;
import com.zyy.asset.model.entity.EquipmentEntity;
import com.zyy.inventory.model.entity.InventoryRecordEntity;
import com.zyy.inventory.model.vo.InventoryRecordVO;
import com.zyy.common.PageVO;
import com.zyy.inventory.service.InventoryRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of equipment maintenance record management service.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryRecordServiceImpl implements InventoryRecordService {

    private final InventoryRecordMapper recordMapper;
    private final EquipmentMapper equipmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryRecordVO save(InventoryRecordSaveDTO saveDTO, Long operatorId) {
        // Verify equipment exists
        EquipmentEntity equipment = equipmentMapper.selectById(saveDTO.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException("Equipment not found: " + saveDTO.getEquipmentId());
        }

        InventoryRecordEntity entity = toEntity(saveDTO);
        entity.setInspectorId(operatorId);
        entity.setCreateUser(operatorId);

        // If nextDate not set but maintenance cycle exists, auto-calculate
        if (entity.getNextDate() == null && equipment.getMaintenanceCycleDays() != null) {
            LocalDate recordDate = saveDTO.getRecordDate() != null ? saveDTO.getRecordDate() : LocalDate.now();
            entity.setNextDate(recordDate.plusDays(equipment.getMaintenanceCycleDays()));
        }

        recordMapper.insert(entity);
        log.info("Inventory record created - id={}, equipmentId={}, type={}, operatorId={}",
                entity.getId(), saveDTO.getEquipmentId(), saveDTO.getRecordType(), operatorId);

        return entityToVO(entity, equipment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryRecordVO update(InventoryRecordUpdateDTO updateDTO, Long operatorId) {
        InventoryRecordEntity entity = recordMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Record not found: " + updateDTO.getId());
        }

        if (updateDTO.getRecordType() != null) entity.setRecordType(updateDTO.getRecordType());
        if (updateDTO.getRecordDate() != null) entity.setRecordDate(updateDTO.getRecordDate());
        if (updateDTO.getInspectorName() != null) entity.setInspectorName(updateDTO.getInspectorName());
        if (updateDTO.getResult() != null) entity.setResult(updateDTO.getResult());
        if (updateDTO.getStatus() != null) entity.setStatus(updateDTO.getStatus());
        if (updateDTO.getEquipmentCondition() != null) entity.setEquipmentCondition(updateDTO.getEquipmentCondition());
        if (updateDTO.getDescription() != null) entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getMaterialsUsed() != null) entity.setMaterialsUsed(updateDTO.getMaterialsUsed());
        if (updateDTO.getManHours() != null) entity.setManHours(updateDTO.getManHours());
        if (updateDTO.getNextDate() != null) entity.setNextDate(updateDTO.getNextDate());
        if (updateDTO.getSignature() != null) entity.setSignature(updateDTO.getSignature());

        recordMapper.updateById(entity);
        log.info("Inventory record updated - id={}, operatorId={}", entity.getId(), operatorId);

        EquipmentEntity equipment = equipmentMapper.selectById(entity.getEquipmentId());
        return entityToVO(entity, equipment);
    }

    @Override
    public InventoryRecordVO getById(Long id) {
        InventoryRecordEntity entity = recordMapper.selectById(id);
        if (entity == null) return null;
        EquipmentEntity equipment = equipmentMapper.selectById(entity.getEquipmentId());
        return entityToVO(entity, equipment);
    }

    @Override
    public PageVO<InventoryRecordVO> getPage(Long pageNum, Long pageSize, Long equipmentId,
                                              String recordType, String startDate, String endDate) {
        Page<InventoryRecordEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InventoryRecordEntity> query = new LambdaQueryWrapper<>();

        if (equipmentId != null) {
            query.eq(InventoryRecordEntity::getEquipmentId, equipmentId);
        }
        if (recordType != null && !recordType.isBlank()) {
            query.eq(InventoryRecordEntity::getRecordType, recordType);
        }
        if (startDate != null && !startDate.isBlank()) {
            query.ge(InventoryRecordEntity::getRecordDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            query.le(InventoryRecordEntity::getRecordDate, LocalDate.parse(endDate));
        }

        query.orderByDesc(InventoryRecordEntity::getRecordDate);
        Page<InventoryRecordEntity> result = recordMapper.selectPage(page, query);

        List<InventoryRecordVO> voList = result.getRecords().stream()
                .map(e -> {
                    EquipmentEntity eq = equipmentMapper.selectById(e.getEquipmentId());
                    return entityToVO(e, eq);
                })
                .collect(Collectors.toList());

        return PageVO.<InventoryRecordVO>builder()
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
        recordMapper.deleteById(id);
        log.info("Inventory record deleted - id={}, operatorId={}", id, operatorId);
    }

    @Override
    public PageVO<InventoryRecordVO> getByEquipment(Long equipmentId, Long pageNum, Long pageSize) {
        return getPage(pageNum, pageSize, equipmentId, null, null, null);
    }

    // ==================== Private Helper Methods ====================

    private InventoryRecordEntity toEntity(InventoryRecordSaveDTO dto) {
        InventoryRecordEntity e = new InventoryRecordEntity();
        e.setEquipmentId(dto.getEquipmentId());
        e.setRecordType(dto.getRecordType());
        e.setRecordDate(dto.getRecordDate() != null ? dto.getRecordDate() : LocalDate.now());
        e.setInspectorName(dto.getInspectorName());
        e.setResult(dto.getResult());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        e.setEquipmentCondition(dto.getEquipmentCondition());
        e.setDescription(dto.getDescription());
        e.setMaterialsUsed(dto.getMaterialsUsed());
        e.setManHours(dto.getManHours());
        e.setNextDate(dto.getNextDate());
        e.setSignature(dto.getSignature());
        return e;
    }

    private InventoryRecordVO entityToVO(InventoryRecordEntity entity, EquipmentEntity equipment) {
        if (entity == null) return null;

        String statusText = switch (entity.getStatus()) {
            case 0 -> "Pending";
            case 1 -> "Completed";
            case 2 -> "Anomaly Found";
            default -> "Unknown";
        };

        return InventoryRecordVO.builder()
                .id(entity.getId())
                .equipmentId(entity.getEquipmentId())
                .equipmentName(equipment != null ? equipment.getName() : null)
                .equipmentCode(equipment != null ? equipment.getEquipmentCode() : null)
                .recordType(entity.getRecordType())
                .recordDate(entity.getRecordDate())
                .inspectorId(entity.getInspectorId())
                .inspectorName(entity.getInspectorName())
                .result(entity.getResult())
                .status(entity.getStatus())
                .statusText(statusText)
                .equipmentCondition(entity.getEquipmentCondition())
                .description(entity.getDescription())
                .materialsUsed(entity.getMaterialsUsed())
                .manHours(entity.getManHours())
                .nextDate(entity.getNextDate())
                .signature(entity.getSignature())
                .createTime(entity.getCreateTime())
                .build();
    }
}
