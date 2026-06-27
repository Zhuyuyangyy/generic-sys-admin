package com.zyy.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.iam.model.entity.SysMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus Mapper interface for sys_menu persistence operations.
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuEntity> {

    /**
     * Select menus by role IDs through role-menu association.
     *
     * @param roleIds List of role IDs
     * @return List of menu entities accessible by the given roles
     */
    @Select("<script>" +
            "SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id IN " +
            "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND m.is_deleted = 0" +
            " ORDER BY m.sort_order ASC" +
            "</script>")
    List<SysMenuEntity> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}
