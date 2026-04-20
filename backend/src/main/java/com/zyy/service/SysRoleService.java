package com.zyy.service;

import com.zyy.model.entity.SysRoleEntity;
import com.zyy.model.vo.PageVO;
import com.zyy.model.vo.SysRoleVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService {

    /** 分页查询角色 */
    PageVO<SysRoleVO> getPage(Long pageNum, Long pageSize, String name, Integer status);

    /** 根据ID查询 */
    SysRoleVO getById(Long id);

    /** 新增角色 */
    SysRoleVO save(SysRoleEntity entity, Long operatorId);

    /** 更新角色 */
    SysRoleVO update(SysRoleEntity entity, Long operatorId);

    /** 删除角色 */
    void delete(Long id, Long operatorId);

    /** 获取所有启用角色（下拉框用） */
    List<SysRoleVO> getAllEnabled();

    /** 角色授权菜单 */
    void grantMenus(Long roleId, List<Long> menuIds);
}
