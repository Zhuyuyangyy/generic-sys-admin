package com.zyy.iam.service;

import com.zyy.iam.model.dto.SysMenuSaveDTO;
import com.zyy.iam.model.dto.SysMenuUpdateDTO;
import com.zyy.iam.model.vo.SysMenuVO;

import java.util.List;

/**
 * System menu business service interface.
 */
public interface SysMenuService {

    List<SysMenuVO> getMenuTree();

    SysMenuVO getById(Long id);

    SysMenuVO create(SysMenuSaveDTO saveDTO, Long operatorId);

    SysMenuVO update(SysMenuUpdateDTO updateDTO, Long operatorId);

    void delete(Long id, Long operatorId);
}
