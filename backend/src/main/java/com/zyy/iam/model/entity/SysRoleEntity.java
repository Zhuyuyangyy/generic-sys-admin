package com.zyy.iam.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色实体
 * 映射表：sys_role
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRoleEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色名称 */
    private String name;

    /** 角色编码（唯一标识，如 ADMIN / STUDENT / TEACHER） */
    private String code;

    /** 角色描述 */
    private String description;

    /** 状态：0=禁用，1=正常 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;
}
