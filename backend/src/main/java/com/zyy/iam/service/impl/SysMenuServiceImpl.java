package com.zyy.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.exception.BusinessException;
import com.zyy.iam.mapper.SysMenuMapper;
import com.zyy.iam.model.dto.SysMenuSaveDTO;
import com.zyy.iam.model.dto.SysMenuUpdateDTO;
import com.zyy.iam.model.entity.SysMenuEntity;
import com.zyy.iam.model.vo.SysMenuVO;
import com.zyy.iam.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of system menu business service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;

    @Override
    public List<SysMenuVO> getMenuTree() {
        List<SysMenuEntity> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .eq(SysMenuEntity::getIsDeleted, 0)
                        .orderByAsc(SysMenuEntity::getSortOrder)
        );

        List<SysMenuVO> voList = allMenus.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return buildTree(voList);
    }

    @Override
    public SysMenuVO getById(Long id) {
        SysMenuEntity entity = menuMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuVO create(SysMenuSaveDTO saveDTO, Long operatorId) {
        SysMenuEntity entity = new SysMenuEntity();
        entity.setParentId(saveDTO.getParentId() != null ? saveDTO.getParentId() : 0L);
        entity.setName(saveDTO.getMenuName());
        entity.setMenuType(parseMenuType(saveDTO.getMenuType()));
        entity.setPath(saveDTO.getPath());
        entity.setIcon(saveDTO.getIcon());
        entity.setPermission(saveDTO.getPermission());
        entity.setSortOrder(saveDTO.getSortOrder() != null ? saveDTO.getSortOrder() : 0);
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);
        entity.setCreateUser(operatorId);

        menuMapper.insert(entity);

        log.info("Menu created - menuId={}, menuName={}, operatorId={}",
                entity.getId(), entity.getName(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuVO update(SysMenuUpdateDTO updateDTO, Long operatorId) {
        SysMenuEntity entity = menuMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Menu not found: " + updateDTO.getId());
        }

        if (updateDTO.getMenuName() != null) {
            entity.setName(updateDTO.getMenuName());
        }
        if (updateDTO.getMenuType() != null) {
            entity.setMenuType(parseMenuType(updateDTO.getMenuType()));
        }
        if (updateDTO.getPath() != null) {
            entity.setPath(updateDTO.getPath());
        }
        if (updateDTO.getIcon() != null) {
            entity.setIcon(updateDTO.getIcon());
        }
        if (updateDTO.getPermission() != null) {
            entity.setPermission(updateDTO.getPermission());
        }
        if (updateDTO.getSortOrder() != null) {
            entity.setSortOrder(updateDTO.getSortOrder());
        }
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }

        menuMapper.updateById(entity);

        log.info("Menu updated - menuId={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        SysMenuEntity entity = menuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Menu not found: " + id);
        }

        // Check for child menus
        long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .eq(SysMenuEntity::getParentId, id)
                        .eq(SysMenuEntity::getIsDeleted, 0)
        );
        if (childCount > 0) {
            throw new BusinessException("Cannot delete menu with child menus. Please delete or move child menus first.");
        }

        menuMapper.deleteById(id);

        log.info("Menu deleted (soft) - menuId={}, operatorId={}", id, operatorId);
    }

    /**
     * Build tree structure from flat menu list.
     */
    private List<SysMenuVO> buildTree(List<SysMenuVO> allMenus) {
        Map<Long, List<SysMenuVO>> groupedByParent = allMenus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() != null ? m.getParentId() : 0L));

        List<SysMenuVO> roots = groupedByParent.getOrDefault(0L, new ArrayList<>());

        for (SysMenuVO root : roots) {
            root.setChildren(findChildren(root.getId(), groupedByParent));
        }

        return roots;
    }

    private List<SysMenuVO> findChildren(Long parentId, Map<Long, List<SysMenuVO>> groupedByParent) {
        List<SysMenuVO> children = groupedByParent.getOrDefault(parentId, new ArrayList<>());
        for (SysMenuVO child : children) {
            child.setChildren(findChildren(child.getId(), groupedByParent));
        }
        return children.isEmpty() ? null : children;
    }

    private SysMenuVO entityToVO(SysMenuEntity entity) {
        if (entity == null) {
            return null;
        }

        String menuTypeText = switch (entity.getMenuType()) {
            case 1 -> "DIRECTORY";
            case 2 -> "MENU";
            case 3 -> "BUTTON";
            default -> "UNKNOWN";
        };

        return SysMenuVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .menuName(entity.getName())
                .menuType(menuTypeText)
                .path(entity.getPath())
                .icon(entity.getIcon())
                .permission(entity.getPermission())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }

    private Integer parseMenuType(String menuType) {
        if (menuType == null) {
            return 2; // Default to MENU
        }
        return switch (menuType.toUpperCase()) {
            case "DIRECTORY" -> 1;
            case "MENU" -> 2;
            case "BUTTON" -> 3;
            default -> 2;
        };
    }
}
