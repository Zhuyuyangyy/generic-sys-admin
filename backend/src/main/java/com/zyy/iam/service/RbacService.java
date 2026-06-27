package com.zyy.iam.service;

import com.zyy.iam.model.vo.SysMenuVO;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 * 提供用户角色、菜单、权限标识的查询
 */
public interface RbacService {

    /**
     * 获取用户的所有角色编码列表
     * @param userId 用户ID
     * @return 角色编码集合，如 ["ROLE_ADMIN", "ROLE_OPERATOR"]
     */
    Set<String> getRolesByUserId(Long userId);

    /**
     * 获取用户的所有菜单权限标识列表
     * @param userId 用户ID
     * @return 权限标识集合，如 ["system:user:list", "system:user:add"]
     */
    Set<String> getPermissionsByUserId(Long userId);

    /**
     * 获取用户有权限访问的菜单树（用于前端动态菜单渲染）
     * @param userId 用户ID
     * @return 菜单树形结构
     */
    List<SysMenuVO> getMenuTreeByUserId(Long userId);
}
