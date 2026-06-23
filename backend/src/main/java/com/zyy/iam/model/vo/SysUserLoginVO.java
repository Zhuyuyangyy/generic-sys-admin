package com.zyy.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for authentication response.
 * <p>
 * Contains JWT token and associated metadata returned upon successful authentication.
 *
 * @author System Architect
 * @version 1.0.0
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysUserLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT access token (Bearer token) */
    private String accessToken;

    /** JWT refresh token */
    private String refreshToken;

    /** Token expiration time */
    private Long expiresIn;

    /** Token type (always "Bearer") */
    private String tokenType;

    /** Authenticated user profile */
    private SysUserVO user;

    /** Login timestamp */
    private LocalDateTime loginTime;
}
