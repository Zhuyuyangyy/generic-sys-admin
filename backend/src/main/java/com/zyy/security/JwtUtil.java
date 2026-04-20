package com.zyy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT工具类
 * 支持签发、解析、验证Token
 * 
 * 配置来源：sys.config.security.jwt-secret（通过 application.yml 从环境变量 JWT_SECRET 注入）
 */
@Slf4j
@Component
public class JwtUtil implements InitializingBean {

    @Autowired
    private com.zyy.config.AppProperties appProperties;

    private SecretKey secretKey;
    private long expiration; // 秒，默认2小时
    private long refreshExpiration; // 秒，7天

    @Override
    public void afterPropertiesSet() {
        String secret = appProperties.getSecurity().getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable must be set! " +
                "Production requires a strong secret key (minimum 32 characters). " +
                "Generate one with: openssl rand -base64 64");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = appProperties.getSecurity().getJwtExpiration();
        this.refreshExpiration = appProperties.getSecurity().getJwtRefreshExpiration();
    }

    /**
     * 签发AccessToken
     */
    public String sign(Long userId, String username, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.SECONDS.toMillis(expiration));

        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256);

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /**
     * 签发RefreshToken（有效期更长）
     */
    public String signRefreshToken(Long userId, String username, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.SECONDS.toMillis(refreshExpiration));

        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256);

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /** 解析Token */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 验证Token */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT无效: {}", e.getMessage());
        }
        return false;
    }

    /** 获取用户ID */
    public Long getUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    /** 获取用户名 */
    public String getUsername(String token) {
        return parse(token).get("username", String.class);
    }

    /** 是否已过期 */
    public boolean isExpired(String token) {
        try {
            return parse(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /** 获取Token类型（access/refresh） */
    public String getTokenType(String token) {
        return parse(token).get("type", String.class);
    }

    /** 是否是RefreshToken */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    /** 刷新Token（使用新的过期时间） */
    public String refresh(Long userId, String username, Map<String, Object> extraClaims) {
        return sign(userId, username, extraClaims);
    }
}
