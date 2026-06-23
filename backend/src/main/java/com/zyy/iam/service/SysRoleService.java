package com.zyy.iam.service;

import com.zyy.common.PageVO;
import com.zyy.iam.model.dto.SysRoleSaveDTO;
import com.zyy.iam.model.dto.SysRoleUpdateDTO;
import com.zyy.iam.model.vo.SysMenuVO;
import com.zyy.iam.model.vo.SysRoleVO;

import java.util.List;

/**
 * System role business service interface.
 */
public interface SysRoleService {

    PageVO<SysRoleVO> getPage(Long pageNum, Long pageSize, String roleName, Integer status);

    SysRoleVO getById(Long id);

    SysRoleVO create(SysRoleSaveDTO saveDTO, Long operatorId);

    SysRoleVO update(SysRoleUpdateDTO updateDTO, Long operatorId);

    void delete(Long id, Long operatorId);

    void assignMenus(Long roleId, List<Long> menuIds, Long operatorId);

    List<SysMenuVO> getRoleMenus(Long roleId);
}
