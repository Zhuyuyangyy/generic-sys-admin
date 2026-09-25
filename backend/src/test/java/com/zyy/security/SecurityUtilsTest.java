package com.zyy.security;

import com.zyy.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityUtils 单元测试。
 *
 * 重点守着历史回归：controller 过去用 request.getAttribute("userId") 取操作人，
 * 取不到就 return 1L，导致审计记录把 operator 记成 user 1。
 * 现在未登录必须抛 UnauthorizedException，而不是悄悄返回任何 ID。
 */
@DisplayName("SecurityUtils 当前用户解析测试")
class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String username, Collection<SimpleGrantedAuthority> authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new LoginUser(userId, username, authorities),
                        null,
                        authorities
                )
        );
    }

    @Test
    @DisplayName("已登录时返回 LoginUser 中的 userId")
    void returnsLoginUserId() {
        authenticate(42L, "operator", List.of(new SimpleGrantedAuthority("equipment:list")));

        assertEquals(42L, securityUtils.currentUserId());
        assertEquals("operator", securityUtils.currentUsernameOrNull());
    }

    @Test
    @DisplayName("未认证上下文必须抛 UnauthorizedException，而不是回退到固定 ID")
    void throwsWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                securityUtils::currentUserId);
        assertEquals("用户未登录", ex.getMessage());
        assertNull(securityUtils.currentUsernameOrNull());
    }

    @Test
    @DisplayName("principal 不是 LoginUser 时抛异常，不猜测身份")
    void throwsWhenPrincipalIsNotLoginUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null, List.of()));

        assertThrows(UnauthorizedException.class, securityUtils::currentUserId);
    }

    @Test
    @DisplayName("LoginUser.userId 为 null 时抛异常")
    void throwsWhenUserIdMissing() {
        authenticate(null, "ghost", Set.of());

        assertThrows(UnauthorizedException.class, securityUtils::currentUserId);
    }
}
