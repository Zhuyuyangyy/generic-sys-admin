package com.zyy.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.zyy.exception.UnauthorizedException;

/**
 * 当前登录用户解析工具。
 * <p>
 * JwtAuthFilter 会把 {@link LoginUser} 放进 {@code SecurityContextHolder}，
 * 所有需要 operatorId 的 Controller/Service 统一从这里取，避免各 Controller
 * 自行从 request attribute 猜测、或退回硬编码的开发默认值。
 * </p>
 *
 * 用法：
 * <pre>
 * Long operatorId = securityUtils.currentUserId();
 * </pre>
 *
 * @author System Architect
 */
@Component("securityUtils")
public class SecurityUtils {

    /**
     * 获取当前登录用户ID。
     *
     * @return 用户ID，永不为 null
     * @throws UnauthorizedException 未登录、principal 类型不符或缺少 userId
     */
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("用户未登录");
        }
        if (authentication.getPrincipal() instanceof LoginUser loginUser) {
            if (loginUser.getUserId() == null) {
                throw new UnauthorizedException("无法获取用户信息");
            }
            return loginUser.getUserId();
        }
        throw new UnauthorizedException("无法获取用户信息");
    }

    /**
     * 获取当前登录用户名；未登录返回 null（不抛异常），供日志/审计场景使用。
     */
    public String currentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return null;
    }
}
