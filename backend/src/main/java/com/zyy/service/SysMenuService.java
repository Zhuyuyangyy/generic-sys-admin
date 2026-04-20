package com.zyy.service;

import com.zyy.model.entity.SysMenuEntity;
import com.zyy.model.vo.SysMenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface SysMenuService {

    /**
     * 根据用户ID获取有权限的菜单树（前端动态菜单渲染用）
     */
    List<SysMenuVO> getMenusByUserId(Long userId);

    /**
     * 获取所有菜单（树形结构，角色管理用）
     */
    List<SysMenuVO> getAllMenusTree();

    /**
     * 根据角色ID获取菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 角色授权：设置角色菜单权限
     */
    void grantMenus(Long roleId, List<Long> menuIds);
}
