package com.zyy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiting filter.
 * <p>
 * Enforces request rate limits per client IP:
 * <ul>
 *   <li>API endpoints: configurable requests per minute (default 100)</li>
 *   <li>Login endpoint: configurable attempts per minute (default 5)</li>
 * </ul>
 * Uses a sliding window approach with periodic cleanup of expired entries.
 * Can be enabled/disabled via configuration.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int apiMaxPerMinute;
    private final int loginMaxPerMinute;

    /** IP -> Counter with timestamp */
    private final Map<String, RateCounter> apiCounters = new ConcurrentHashMap<>();
    private final Map<String, RateCounter> loginCounters = new ConcurrentHashMap<>();

    /** Last cleanup timestamp */
    private volatile long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 120_000; // 2 minutes

    public RateLimitFilter(
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.api-max-per-minute:100}") int apiMaxPerMinute,
            @Value("${security.rate-limit.login-max-per-minute:5}") int loginMaxPerMinute) {
        this.enabled = enabled;
        this.apiMaxPerMinute = apiMaxPerMinute;
        this.loginMaxPerMinute = loginMaxPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Check login rate limit
        if (path.endsWith("/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            if (isRateLimited(loginCounters, clientIp, loginMaxPerMinute)) {
                log.warn("Login rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429);
                response.getWriter().write("{\"code\":429,\"message\":\"Too many login attempts. Please try again later.\"}");
                response.setContentType("application/json");
                return;
            }
        }

        // Check general API rate limit for /api/ paths
        if (path.startsWith("/api/")) {
            if (isRateLimited(apiCounters, clientIp, apiMaxPerMinute)) {
                log.warn("API rate limit exceeded for IP: {}", clientIp);
                response.setStatus(429);
                response.getWriter().write("{\"code\":429,\"message\":\"Too many requests. Please try again later.\"}");
                response.setContentType("application/json");
                return;
            }
        }

        // Periodic cleanup
        maybeCleanup();

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(Map<String, RateCounter> counters, String key, int maxPerMinute) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000; // 1 minute window

        RateCounter counter = counters.computeIfAbsent(key, k -> new RateCounter());
        synchronized (counter) {
            // Reset if outside the window
            if (counter.windowStart < windowStart) {
                counter.windowStart = now;
                counter.count.set(0);
            }
            return counter.count.incrementAndGet() > maxPerMinute;
        }
    }

    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            lastCleanup = now;
            long windowStart = now - 60_000;
            apiCounters.entrySet().removeIf(e -> e.getValue().windowStart < windowStart);
            loginCounters.entrySet().removeIf(e -> e.getValue().windowStart < windowStart);
            log.debug("Rate limit cleanup: apiCounters={}, loginCounters={}",
                    apiCounters.size(), loginCounters.size());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple counter with a sliding window start timestamp.
     */
    private static class RateCounter {
        volatile long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }
}
