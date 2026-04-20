package com.zyy.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for user data presentation.
 * <p>
 * This DTO is exclusively used for API responses. It implements data masking
 * and excludes sensitive fields such as password and internal audit fields.
 * <p>
 * Transformation: {@link com.zyy.model.entity.SysUserEntity} 鈫?{@link SysUserVO}
 *
 * @author System Architect
 * @version 1.0.0
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** User identifier */
    private Long id;

    /** Username (login credential) */
    private String username;

    /** Real name (display name) */
    private String realName;

    /** Email address */
    private String email;

    /** Phone number (masked if configured) */
    private String phone;

    /** Profile avatar URL */
    private String avatarUrl;

    /**
     * Account status.
     * 0 = disabled, 1 = normal, 2 = locked
     */
    private Integer status;

    /** Human-readable status text */
    private String statusText;

    /** Last login IP address */
    private String lastLoginIp;

    /** Last successful login timestamp */
    private LocalDateTime lastLoginAt;

    /** Account creation timestamp */
    private LocalDateTime createTime;

    /** Password last change timestamp */
    private LocalDateTime passwordChangedAt;

    /** Days since last password change */
    private Integer passwordAgeDays;
}
