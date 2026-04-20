package com.zyy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 权限校验组件
 * SpEL 表达式中使用，例如：@PreAuthorize("@ss.hasAuthority('system:user:add')")
 * 替代原有的 @PreAuthorize("hasAuthority('system:user:add')")，
 * 解决 Spring Security 5.x ~ 6.x 对 SpEL 中 principal 字段访问的限制
 */
@Slf4j
@Component("ss")
public class SecurityChecker {

    /**
     * 检查当前登录用户是否拥有指定权限标识
     * 从 SecurityContext 中获取已认证用户的权限集合进行比对
     *
     * @param permission 权限标识，例如 "system:user:add"
     * @return true 表示有权限，false 表示无权限
     */
    public boolean hasAuthority(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 遍历所有 GrantedAuthority 进行比对
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }

    /**
     * 检查当前用户是否为管理员角色
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * 检查当前用户是否拥有指定角色
     *
     * @param roleCode 角色编码，例如 "ADMIN"
     * @return true 表示有角色，false 表示无角色
     */
    public boolean hasRole(String roleCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return auth.equals("ROLE_" + roleCode) || auth.equals(roleCode);
                });
    }
}
