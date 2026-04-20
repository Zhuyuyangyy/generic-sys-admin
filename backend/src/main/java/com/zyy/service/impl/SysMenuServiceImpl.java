package com.zyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyy.exception.BusinessException;
import com.zyy.mapper.SysMenuMapper;
import com.zyy.mapper.SysRoleMapper;
import com.zyy.mapper.SysRoleMenuMapper;
import com.zyy.mapper.SysUserRoleMapper;
import com.zyy.model.entity.SysMenuEntity;
import com.zyy.model.entity.SysRoleMenuEntity;
import com.zyy.model.vo.SysMenuVO;
import com.zyy.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysMenuVO> getMenusByUserId(Long userId) {
        // 1. 获取用户角色ID列表
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 根据角色ID列表获取有权限的菜单
        List<SysMenuEntity> menus = menuMapper.selectByRoleIds(roleIds);
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 转换为VO并构建树形结构
        List<SysMenuVO> allMenus = menus.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return buildTree(allMenus, 0L);
    }

    @Override
    public List<SysMenuVO> getAllMenusTree() {
        List<SysMenuEntity> allMenus = menuMapper.selectAllEnabled();
        List<SysMenuVO> voList = allMenus.stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());
        return buildTree(voList, 0L);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        // 删除该角色的旧菜单权限
        LambdaQueryWrapper<SysRoleMenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenuEntity::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);

        // 插入新的菜单权限
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenuEntity e = new SysRoleMenuEntity();
                e.setRoleId(roleId);
                e.setMenuId(menuId);
                roleMenuMapper.insert(e);
            }
        }

        log.info("角色菜单授权完成 - roleId={}, menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);
    }

    /**
     * 构建菜单树
     */
    private List<SysMenuVO> buildTree(List<SysMenuVO> menus, Long parentId) {
        return menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .peek(m -> {
                    List<SysMenuVO> children = buildTree(menus, m.getId());
                    if (!children.isEmpty()) {
                        m.setChildren(children);
                    }
                })
                .collect(Collectors.toList());
    }

    private SysMenuVO entityToVO(SysMenuEntity entity) {
        return SysMenuVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .path(entity.getPath())
                .component(entity.getComponent())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .visible(entity.getVisible())
                .permission(entity.getPermission())
                .menuType(entity.getMenuType())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
