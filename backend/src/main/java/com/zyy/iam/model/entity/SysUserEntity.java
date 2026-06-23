package com.zyy.iam.model.entity;

import com.zyy.common.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System user persistent entity.
 * <p>
 * Maps to the sys_user table, representing authenticated system users
 * within the RBAC (Role-Based Access Control) architecture.
 * This entity is exclusively used by the persistence layer (Mapper).
 * <p>
 * Security notes:
 * <ul>
 *   <li>Password is stored using BCrypt encoding</li>
 *   <li>Status values: 0=disabled, 1=normal, 2=locked</li>
 * </ul>
 *
 * @author System Architect
 * @see com.zyy.iam.model.vo.SysUserVO
 * @see com.zyy.iam.model.dto.SysUserSaveDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {

    /** Unique username identifier */
    private String username;

    /** BCrypt-encoded password (never exposed to presentation layer) */
    private String password;

    /** User's real name */
    private String realName;

    /** Contact email address */
    private String email;

    /** Contact phone number */
    private String phone;

    /** Profile avatar URL */
    private String avatarUrl;

    /**
     * Account status.
     * 0 = disabled (forbidden to login)
     * 1 = normal (active)
     * 2 = locked (temporary access restriction)
     */
    @TableField("status")
    private Integer status;

    /** Last login IP address */
    private String lastLoginIp;

    /** Last successful login timestamp */
    private LocalDateTime lastLoginAt;

    /** Consecutive failed login attempts counter */
    private Integer failedAttempts;

    /** Account lockout expiration timestamp */
    private LocalDateTime lockedUntil;

    /** Account password last change timestamp */
    private LocalDateTime passwordChangedAt;
}
