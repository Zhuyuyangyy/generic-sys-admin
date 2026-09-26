package com.zyy.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
@DisplayName("JwtUtil 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        try {
            // JwtUtil 在 master 上改为直接 @Value 注入 jwt.secret/jwt.expiration/
            // jwt.refresh-expiration，不再有 appProperties 字段。
            // 纯单测：@Value 不注入，long 字段默认 0 会让 token 立刻过期。
            // 显式给 secret / 两个有效期赋值，模拟 Spring 注入后的状态。
            var secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, "test-secret-key-must-be-at-least-32-chars-long-for-hs256!");
            var expField = JwtUtil.class.getDeclaredField("expiration");
            expField.setAccessible(true);
            expField.set(jwtUtil, 7200L);
            var refreshField = JwtUtil.class.getDeclaredField("refreshExpiration");
            refreshField.setAccessible(true);
            refreshField.set(jwtUtil, 604800L);

            var initMethod = JwtUtil.class.getDeclaredMethod("afterPropertiesSet");
            initMethod.setAccessible(true);
            initMethod.invoke(jwtUtil);
        } catch (Exception e) {
            throw new RuntimeException("JwtUtil init failed", e);
        }
    }

    @Test
    @DisplayName("签发AccessToken并解析成功")
    void signAndParseAccessToken() {
        Long userId = 1L;
        String username = "admin";
        Map<String, Object> claims = Map.of("roles", java.util.List.of("ROLE_ADMIN"));

        String token = jwtUtil.sign(userId, username, claims);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals(userId, jwtUtil.getUserId(token));
        assertEquals(username, jwtUtil.getUsername(token));
        assertFalse(jwtUtil.isExpired(token));
    }

    @Test
    @DisplayName("signRefreshToken签发专用的RefreshToken并验证类型")
    void signRefreshToken() {
        Long userId = 1L;
        String username = "admin";

        String refreshToken = jwtUtil.signRefresh(userId, username, null);
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        
    }

    @Test
    @DisplayName("过期Token验证失败")
    void expiredTokenValidation() {
        try {
            JwtUtil shortLived = new JwtUtil();
            var secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(shortLived, "test-secret-key-must-be-at-least-32-chars-long-for-hs256!");
            // 负的 expiration 让 token 立即过期
            var expField = JwtUtil.class.getDeclaredField("expiration");
            expField.setAccessible(true);
            expField.set(shortLived, -1L);

            var initMethod = JwtUtil.class.getDeclaredMethod("afterPropertiesSet");
            initMethod.setAccessible(true);
            initMethod.invoke(shortLived);

            String token = shortLived.sign(1L, "admin", null);
            assertTrue(shortLived.isExpired(token));
        } catch (Exception e) {
            fail("Should not throw when checking expired token");
        }
    }

    @Test
    @DisplayName("无效Token解析抛出异常")
    void invalidTokenThrows() {
        assertThrows(Exception.class, () -> jwtUtil.parse("invalid.token.here"));
    }

    @Test
    @DisplayName("validate方法对无效Token返回false")
    void validateInvalidToken() {
        assertFalse(jwtUtil.validate("not.a.valid.token"));
    }

    @Test
    @DisplayName("refresh方法基于现有Token生成新Token（刷新有效期）")
    void refreshToken() {
        // master 上 refresh() 走 signRefresh：带 tokenType=refresh 且有效期更长，
        // 因此不再与 access token 逐字节相同。改为断言契约本身。
        String refreshed = jwtUtil.refresh(1L, "admin", null);
        assertNotNull(refreshed);
        assertTrue(jwtUtil.validate(refreshed));
        assertTrue(jwtUtil.isRefreshToken(refreshed));
        assertFalse(jwtUtil.isExpired(refreshed));
    }
}