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
    private final Collection<SimpleGrantedAuthority> authorities;

    public LoginUser(Long userId, String username, 
                     Collection<SimpleGrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.realName = "";
        this.authorities = authorities;
    }

    public LoginUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.realName = "";
        this.authorities = java.util.List.of();
    }
}
