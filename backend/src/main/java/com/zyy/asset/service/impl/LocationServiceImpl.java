package com.zyy.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.exception.BusinessException;
import com.zyy.asset.mapper.LocationMapper;
import com.zyy.asset.model.dto.LocationSaveDTO;
import com.zyy.asset.model.dto.LocationUpdateDTO;
import com.zyy.asset.model.entity.LocationEntity;
import com.zyy.asset.model.vo.LocationVO;
import com.zyy.asset.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of location management business service.
 * <p>
 * Provides location hierarchy management with tree structure support.
 *
 * @author System Architect
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationMapper locationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocationVO save(LocationSaveDTO saveDTO, Long operatorId) {
        // Check location code uniqueness
        long existing = locationMapper.selectCount(
                new LambdaQueryWrapper<LocationEntity>()
                        .eq(LocationEntity::getLocationCode, saveDTO.getLocationCode())
                        .eq(LocationEntity::getIsDeleted, 0)
        );
        if (existing > 0) {
            throw new BusinessException("Location code already exists: " + saveDTO.getLocationCode());
        }

        // Validate parent exists if specified
        if (saveDTO.getParentId() != null) {
            LocationEntity parent = locationMapper.selectById(saveDTO.getParentId());
            if (parent == null) {
                throw new BusinessException("Parent location not found: " + saveDTO.getParentId());
            }
        }

        LocationEntity entity = toEntity(saveDTO);
        entity.setCreateUser(operatorId);
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);

        locationMapper.insert(entity);
        log.info("Location created - id={}, code={}, operatorId={}",
                entity.getId(), entity.getLocationCode(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocationVO update(LocationUpdateDTO updateDTO, Long operatorId) {
        LocationEntity entity = locationMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Location not found: " + updateDTO.getId());
        }

        // Prevent circular reference: cannot set self as parent
        if (updateDTO.getParentId() != null && updateDTO.getParentId().equals(updateDTO.getId())) {
            throw new BusinessException("Location cannot be its own parent");
        }

        if (updateDTO.getLocationName() != null) entity.setLocationName(updateDTO.getLocationName());
        if (updateDTO.getLocationCode() != null) entity.setLocationCode(updateDTO.getLocationCode());
        if (updateDTO.getParentId() != null) entity.setParentId(updateDTO.getParentId());
        if (updateDTO.getAddress() != null) entity.setAddress(updateDTO.getAddress());
        if (updateDTO.getDescription() != null) entity.setDescription(updateDTO.getDescription());
        if (updateDTO.getStatus() != null) entity.setStatus(updateDTO.getStatus());

        locationMapper.updateById(entity);
        log.info("Location updated - id={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    public List<LocationVO> getTree() {
        List<LocationEntity> allLocations = locationMapper.selectList(
                new LambdaQueryWrapper<LocationEntity>()
                        .eq(LocationEntity::getIsDeleted, 0)
                        .orderByAsc(LocationEntity::getLocationCode)
        );

        List<LocationVO> voList = allLocations.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return buildTree(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        LocationEntity entity = locationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Location not found: " + id);
        }

        // Check for child locations
        long childCount = locationMapper.selectCount(
                new LambdaQueryWrapper<LocationEntity>()
                        .eq(LocationEntity::getParentId, id)
                        .eq(LocationEntity::getIsDeleted, 0)
        );
        if (childCount > 0) {
            throw new BusinessException("Cannot delete location with child locations. Remove children first.");
        }

        locationMapper.deleteById(id);
        log.info("Location deleted (soft) - id={}, operatorId={}", id, operatorId);
    }

    // ==================== Private Helper Methods ====================

    private List<LocationVO> buildTree(List<LocationVO> allNodes) {
        Map<Long, List<LocationVO>> childrenMap = allNodes.stream()
                .filter(node -> node.getParentId() != null)
                .collect(Collectors.groupingBy(LocationVO::getParentId));

        List<LocationVO> roots = allNodes.stream()
                .filter(node -> node.getParentId() == null)
                .collect(Collectors.toList());

        for (LocationVO root : roots) {
            populateChildren(root, childrenMap);
        }

        return roots;
    }

    private void populateChildren(LocationVO parent, Map<Long, List<LocationVO>> childrenMap) {
        List<LocationVO> children = childrenMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (LocationVO child : children) {
                populateChildren(child, childrenMap);
            }
        }
    }

    private LocationEntity toEntity(LocationSaveDTO dto) {
        LocationEntity entity = new LocationEntity();
        entity.setLocationName(dto.getLocationName());
        entity.setLocationCode(dto.getLocationCode());
        entity.setParentId(dto.getParentId());
        entity.setAddress(dto.getAddress());
        entity.setDescription(dto.getDescription());
        return entity;
    }

    private LocationVO entityToVO(LocationEntity entity) {
        if (entity == null) return null;

        String statusText = entity.getStatus() != null && entity.getStatus() == 1 ? "Active" : "Inactive";

        return LocationVO.builder()
                .id(entity.getId())
                .locationName(entity.getLocationName())
                .locationCode(entity.getLocationCode())
                .parentId(entity.getParentId())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .statusText(statusText)
                .createTime(entity.getCreateTime())
                .build();
    }
}
