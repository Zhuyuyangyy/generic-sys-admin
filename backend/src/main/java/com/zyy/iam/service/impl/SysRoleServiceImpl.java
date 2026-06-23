package com.zyy.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.common.PageVO;
import com.zyy.exception.BusinessException;
import com.zyy.iam.mapper.SysRoleMapper;
import com.zyy.iam.mapper.SysRoleMenuMapper;
import com.zyy.iam.mapper.SysMenuMapper;
import com.zyy.iam.model.dto.SysRoleSaveDTO;
import com.zyy.iam.model.dto.SysRoleUpdateDTO;
import com.zyy.iam.model.entity.SysMenuEntity;
import com.zyy.iam.model.entity.SysRoleEntity;
import com.zyy.iam.model.entity.SysRoleMenuEntity;
import com.zyy.iam.model.vo.SysMenuVO;
import com.zyy.iam.model.vo.SysRoleVO;
import com.zyy.iam.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of system role business service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageVO<SysRoleVO> getPage(Long pageNum, Long pageSize, String roleName, Integer status) {
        Page<SysRoleEntity> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysRoleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleEntity::getIsDeleted, 0);

        if (roleName != null && !roleName.isBlank()) {
            queryWrapper.like(SysRoleEntity::getName, roleName);
        }
        if (status != null) {
            queryWrapper.eq(SysRoleEntity::getStatus, status);
        }

        queryWrapper.orderByAsc(SysRoleEntity::getSortOrder);

        Page<SysRoleEntity> result = roleMapper.selectPage(page, queryWrapper);

        List<SysRoleVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<SysRoleVO>builder()
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
    public SysRoleVO getById(Long id) {
        SysRoleEntity entity = roleMapper.selectById(id);
        return entity != null ? entityToVO(entity) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO create(SysRoleSaveDTO saveDTO, Long operatorId) {
        // Check role code uniqueness
        long existingCount = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .eq(SysRoleEntity::getCode, saveDTO.getRoleCode())
                        .eq(SysRoleEntity::getIsDeleted, 0)
        );
        if (existingCount > 0) {
            throw new BusinessException("Role code already exists: " + saveDTO.getRoleCode());
        }

        SysRoleEntity entity = new SysRoleEntity();
        entity.setCode(saveDTO.getRoleCode());
        entity.setName(saveDTO.getRoleName());
        entity.setDescription(saveDTO.getDescription());
        entity.setStatus(saveDTO.getStatus() != null ? saveDTO.getStatus() : 1);
        entity.setCreateUser(operatorId);

        roleMapper.insert(entity);

        log.info("Role created - roleId={}, roleCode={}, operatorId={}",
                entity.getId(), entity.getCode(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRoleVO update(SysRoleUpdateDTO updateDTO, Long operatorId) {
        SysRoleEntity entity = roleMapper.selectById(updateDTO.getId());
        if (entity == null) {
            throw new BusinessException("Role not found: " + updateDTO.getId());
        }

        if (updateDTO.getRoleName() != null) {
            entity.setName(updateDTO.getRoleName());
        }
        if (updateDTO.getDescription() != null) {
            entity.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getStatus() != null) {
            entity.setStatus(updateDTO.getStatus());
        }

        roleMapper.updateById(entity);

        log.info("Role updated - roleId={}, operatorId={}", entity.getId(), operatorId);

        return entityToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long operatorId) {
        SysRoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("Role not found: " + id);
        }

        roleMapper.deleteById(id);

        log.info("Role deleted (soft) - roleId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds, Long operatorId) {
        SysRoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("Role not found: " + roleId);
        }

        // Remove existing role-menu associations
        roleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .eq(SysRoleMenuEntity::getRoleId, roleId)
        );

        // Insert new associations
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenuEntity roleMenu = new SysRoleMenuEntity();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setCreateUser(operatorId);
                roleMenuMapper.insert(roleMenu);
            }
        }

        log.info("Role menus assigned - roleId={}, menuCount={}, operatorId={}",
                roleId, menuIds != null ? menuIds.size() : 0, operatorId);
    }

    @Override
    public List<SysMenuVO> getRoleMenus(Long roleId) {
        List<SysRoleMenuEntity> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .eq(SysRoleMenuEntity::getRoleId, roleId)
        );

        if (roleMenus.isEmpty()) {
            return List.of();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenuEntity::getMenuId)
                .collect(Collectors.toList());

        List<SysMenuEntity> menus = menuMapper.selectBatchIds(menuIds);

        return menus.stream()
                .map(this::menuEntityToVO)
                .collect(Collectors.toList());
    }

    private SysRoleVO entityToVO(SysRoleEntity entity) {
        if (entity == null) {
            return null;
        }

        String statusText = switch (entity.getStatus()) {
            case 0 -> "Disabled";
            case 1 -> "Normal";
            default -> "Unknown";
        };

        return SysRoleVO.builder()
                .id(entity.getId())
                .roleCode(entity.getCode())
                .roleName(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .statusText(statusText)
                .createTime(entity.getCreateTime())
                .build();
    }

    private SysMenuVO menuEntityToVO(SysMenuEntity entity) {
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
}
