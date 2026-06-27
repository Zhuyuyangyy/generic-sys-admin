package com.zyy.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token blacklist service using Redis.
 * <p>
 * On logout, the JWT is added to a Redis set with TTL equal to the
 * token's remaining expiration. JwtAuthFilter checks this blacklist
 * before accepting any token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.token-blacklist.enabled:true}")
    private boolean enabled;

    /**
     * Add a token to the blacklist.
     *
     * @param token          the JWT token string
     * @param remainingSeconds remaining lifetime of the token in seconds
     */
    public void blacklist(String token, long remainingSeconds) {
        if (!enabled) {
            log.debug("Token blacklist is disabled, skipping");
            return;
        }
        if (remainingSeconds <= 0) {
            log.debug("Token already expired, no need to blacklist");
            return;
        }
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", remainingSeconds, TimeUnit.SECONDS);
        log.info("Token blacklisted for {} seconds", remainingSeconds);
    }

    /**
     * Check whether a token has been blacklisted.
     *
     * @param token the JWT token string
     * @return true if the token is in the blacklist
     */
    public boolean isBlacklisted(String token) {
        if (!enabled) {
            return false;
        }
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
