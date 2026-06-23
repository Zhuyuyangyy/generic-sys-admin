package com.zyy.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.exception.BusinessException;
import com.zyy.inventory.mapper.SupplierMapper;
import com.zyy.inventory.model.dto.SupplierSaveDTO;
import com.zyy.inventory.model.dto.SupplierUpdateDTO;
import com.zyy.inventory.model.entity.SupplierEntity;
import com.zyy.inventory.model.vo.SupplierVO;
import com.zyy.common.PageVO;
import com.zyy.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of supplier management business service.
 * <p>
 * Provides supplier lifecycle management including registration,
 * updates, and soft deletion.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierVO save(SupplierSaveDTO saveDTO, Long operatorId) {
        // Check supplier code uniqueness
        long existing = supplierMapper.selectCount(
                new LambdaQueryWrapper<SupplierEntity>()
                        .eq(SupplierEntity::getSupplierCode, saveDTO.getSupplierCode())
                        .eq(SupplierEntity::getIsDeleted, 0)
        );
        if (existing > 0) {
            throw new BusinessException("Supplier code already exists: " + saveDTO.getSupplierCode());
        }

        SupplierEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);

        supplierMapper.insert(entity);
        log.info("Supplier created - id={}, code={}, operatorId={}",
                entity.getId(), entity.getSupplierCode(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierVO update(SupplierUpdateDTO updateDTO, Long operatorId) {
        SupplierEntity entity = supplierMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Supplier not found: " + updateDTO.getId());
        }

        if (updateDTO.getSupplierName() != null) entity.setSupplierName(updateDTO.getSupplierName());
        if (updateDTO.getContactPerson() != null) entity.setContactPerson(updateDTO.getContactPerson());
        if (updateDTO.getContactPhone() != null) entity.setContactPhone(updateDTO.getContactPhone());
        if (updateDTO.getEmail() != null) entity.setEmail(updateDTO.getEmail());
        if (updateDTO.getAddress() != null) entity.setAddress(updateDTO.getAddress());
        if (updateDTO.getDescription() != null) entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getStatus() != null) entity.setStatus(updateDTO.getStatus());

        supplierMapper.updateById(entity);
        log.info("Supplier updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public SupplierVO getById(Long id) {
        SupplierEntity entity = supplierMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    public PageVO<SupplierVO> getPage(Long pageNum, Long pageSize, String name, Integer status) {
        Page<SupplierEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SupplierEntity> query = new LambdaQueryWrapper<>();
        query.eq(SupplierEntity::getIsDeleted, 0);

        if (name != null && !name.isBlank()) {
            query.like(SupplierEntity::getSupplierName, name);
        }
        if (status != null) {
            query.eq(SupplierEntity::getStatus, status);
        }

        query.orderByDesc(SupplierEntity::getCreateTime);
        Page<SupplierEntity> result = supplierMapper.selectPage(page, query);

        List<SupplierVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<SupplierVO>builder()
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
        supplierMapper.deleteById(id);
        log.info("Supplier deleted (soft) - id={}, operatorId={}", id, operatorId);
    }

    // ==================== Private Helper Methods ====================

    private SupplierEntity toEntity(SupplierSaveDTO dto) {
        SupplierEntity entity = new SupplierEntity();
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setSupplierName(dto.getSupplierName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setContactPhone(dto.getContactPhone());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setDescription(dto.getDescription());
        return entity;
    }

    private SupplierVO entityToVO(SupplierEntity entity) {
        if (entity == null) return null;

        String statusText = entity.getStatus() != null && entity.getStatus() == 1 ? "Active" : "Inactive";

        return SupplierVO.builder()
                .id(entity.getId())
                .supplierCode(entity.getSupplierCode())
                .supplierName(entity.getSupplierName())
                .contactPerson(entity.getContactPerson())
                .contactPhone(entity.getContactPhone())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .statusText(statusText)
                .createTime(entity.getCreateTime())
                .build();
    }
}
