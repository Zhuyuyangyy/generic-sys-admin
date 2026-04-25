package com.zyy.rbac.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;

    @TableField("status")
    private Integer status; // 0=禁用，1=正常，2=锁定

    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private Long createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
