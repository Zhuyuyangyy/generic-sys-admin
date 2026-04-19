package com.zyy.rbac.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统角色实体
 */
@Data
@TableName("sys_role")
public class SysRole {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer status; // 0=禁用，1=正常
    private Integer sortOrder;
    private Long createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
