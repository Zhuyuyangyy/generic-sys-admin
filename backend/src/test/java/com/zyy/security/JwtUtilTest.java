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
            var appProps = new com.zyy.config.AppProperties();
            appProps.getSecurity().setJwtSecret("test-secret-key-must-be-at-least-32-chars-long-for-hs256!");
            appProps.getSecurity().setJwtExpiration(7200);
            appProps.getSecurity().setJwtRefreshExpiration(604800);

            var field = JwtUtil.class.getDeclaredField("appProperties");
            field.setAccessible(true);
            field.set(jwtUtil, appProps);

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

        String refreshToken = jwtUtil.signRefreshToken(userId, username, null);
        assertNotNull(refreshToken);
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertEquals("refresh", jwtUtil.getTokenType(refreshToken));
    }

    @Test
    @DisplayName("过期Token验证失败")
    void expiredTokenValidation() {
        try {
            JwtUtil shortLived = new JwtUtil();
            var appProps = new com.zyy.config.AppProperties();
            appProps.getSecurity().setJwtSecret("test-secret-key-must-be-at-least-32-chars-long-for-hs256!");
            appProps.getSecurity().setJwtExpiration(-1);

            var field = JwtUtil.class.getDeclaredField("appProperties");
            field.setAccessible(true);
            field.set(shortLived, appProps);

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
        String original = jwtUtil.sign(1L, "admin", null);
        String refreshed = jwtUtil.refresh(1L, "admin", null);
        // refresh()底层也是sign()，相同参数结果相同
        assertEquals(original, refreshed);
        assertTrue(jwtUtil.validate(refreshed));
    }
    // ==================== fail-fast：弱/缺失 secret 必须拒绝启动 ====================

    private JwtUtil jwtUtilWithSecret(String secret) throws Exception {
        JwtUtil util = new JwtUtil();
        var appProps = new com.zyy.config.AppProperties();
        appProps.getSecurity().setJwtSecret(secret);
        var field = JwtUtil.class.getDeclaredField("appProperties");
        field.setAccessible(true);
        field.set(util, appProps);
        return util;
    }

    private void assertInitFails(String secret) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            try {
                var m = JwtUtil.class.getDeclaredMethod("afterPropertiesSet");
                m.setAccessible(true);
                m.invoke(jwtUtilWithSecret(secret));
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw (IllegalStateException) e.getCause();
            }
        });
        assertTrue(ex.getMessage().contains("JWT_SECRET"), "错误信息应指向 JWT_SECRET");
    }

    @Test
    @DisplayName("JWT_SECRET 缺失时必须启动失败")
    void blankSecretFailsFast() {
        assertInitFails(null);
        assertInitFails("");
        assertInitFails("   ");
    }

    @Test
    @DisplayName("使用占位默认 secret 时必须启动失败（防止静默进入生产）")
    void knownWeakSecretFailsFast() {
        assertInitFails("dev-secret-do-not-use-in-production-32chars!");
        assertInitFails("123456");
        assertInitFails("please-change-me");
        assertInitFails("secret");
    }

    @Test
    @DisplayName("合法 secret 正常初始化")
    void strongSecretInitialises() throws Exception {
        var m = JwtUtil.class.getDeclaredMethod("afterPropertiesSet");
        m.setAccessible(true);
        assertDoesNotThrow(() -> {
            try {
                m.invoke(jwtUtilWithSecret("another-strong-secret-at-least-32-characters!"));
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        });
    }
}
