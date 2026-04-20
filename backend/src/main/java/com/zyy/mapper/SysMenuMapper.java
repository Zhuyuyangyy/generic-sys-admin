package com.zyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.model.entity.SysMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuEntity> {

    /**
     * 根据角色ID查询所有菜单ID列表
     */
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.is_deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort_order")
    List<SysMenuEntity> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据多个角色ID查询菜单（去重）
     */
    @Select("<script>" +
            "SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id IN " +
            "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>" +
            "#{rid}" +
            "</foreach>" +
            " AND m.is_deleted = 0 AND m.status = 1 AND m.visible = 1 " +
            "ORDER BY m.sort_order" +
            "</script>")
    List<SysMenuEntity> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 查询所有启用的菜单（树形结构用）
     */
    @Select("SELECT * FROM sys_menu WHERE is_deleted = 0 AND status = 1 ORDER BY sort_order")
    List<SysMenuEntity> selectAllEnabled();
}