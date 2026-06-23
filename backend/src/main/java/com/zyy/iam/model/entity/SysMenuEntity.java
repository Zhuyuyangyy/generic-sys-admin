package com.zyy.iam.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统菜单实体
 * 映射表：sys_menu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenuEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 父菜单ID，0表示顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径（前端 Vue 组件路径） */
    private String component;

    /** 菜单图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 是否显示：0=隐藏，1=显示 */
    private Integer visible;

    /** 是否外链：0=否，1=是 */
    private Integer isExternal;

    /** 权限标识（如 system:user:list） */
    private String permission;

    /** 菜单类型：1=目录，2=菜单，3=按钮 */
    private Integer menuType;

    /** 状态：0=禁用，1=正常 */
    private Integer status;
}
