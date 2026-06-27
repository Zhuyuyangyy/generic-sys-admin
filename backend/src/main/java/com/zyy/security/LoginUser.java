package com.zyy.security;

import lombok.Data;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

/**
 * 登录用户实体
 */
@Data
public class LoginUser {

    private final Long userId;
    private final String username;
    private final String realName;
    private final Long tenantId;
    private final Long departmentId;
    private final Collection<SimpleGrantedAuthority> authorities;

    public LoginUser(Long userId, String username,
                     Collection<SimpleGrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.realName = "";
        this.tenantId = 1L;
        this.departmentId = null;
        this.authorities = authorities;
    }

    public LoginUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.realName = "";
        this.tenantId = 1L;
        this.departmentId = null;
        this.authorities = java.util.List.of();
    }

    public LoginUser(Long userId, String username, Long tenantId,
                     Long departmentId,
                     Collection<SimpleGrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.realName = "";
        this.tenantId = tenantId != null ? tenantId : 1L;
        this.departmentId = departmentId;
        this.authorities = authorities;
    }
}
