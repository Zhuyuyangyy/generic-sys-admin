package com.zyy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色-菜单关联实体
 * 映射表：sys_role_menu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
public class SysRoleMenuEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 菜单ID */
    private Long menuId;
}
