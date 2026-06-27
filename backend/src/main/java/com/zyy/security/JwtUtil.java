package com.zyy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT工具类
 * 支持签发、解析、验证Token
 */
@Slf4j
@Component
public class JwtUtil implements InitializingBean {

    @Value("${jwt.secret:your-256-bit-secret-key-here-must-be-at-least-32-chars!}")
    private String secret;

    @Value("${jwt.expiration:7200}")
    private long expiration; // 秒，默认2小时

    @Value("${jwt.refresh-expiration:604800}")
    private long refreshExpiration; // 秒，7天

    private SecretKey secretKey;

    @Override
    public void afterPropertiesSet() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发Token */
    public String sign(Long userId, String username, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.SECONDS.toMillis(expiration));

        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
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

    /** 刷新Token */
    public String refresh(Long userId, String username, Map<String, Object> extraClaims) {
        return signRefresh(userId, username, extraClaims);
    }

    /** 签发Refresh Token (longer expiration) */
    public String signRefresh(Long userId, String username, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.SECONDS.toMillis(refreshExpiration));

        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
                .claim("tokenType", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256);

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    /** Check if the token is a refresh token */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parse(token);
            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /** Get remaining expiration time in seconds */
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = parse(token);
            Date expiration = claims.getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return remainingMs > 0 ? TimeUnit.MILLISECONDS.toSeconds(remainingMs) : 0;
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }
}
