package com.zyy.security;

import com.zyy.common.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tenant context filter.
 * Extracts tenant_id from JWT claims and sets TenantContext for the request.
 * Clears TenantContext after request completion to prevent thread pool leakage.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validate(token)) {
                Claims claims = jwtUtil.parse(token);
                Object tenantIdObj = claims.get("tenantId");
                if (tenantIdObj != null) {
                    Long tenantId = ((Number) tenantIdObj).longValue();
                    TenantContext.setTenantId(tenantId);
                    log.debug("Tenant context set: tenantId={}", tenantId);
                } else {
                    // Default tenant for backward compatibility
                    TenantContext.setTenantId(1L);
                }
            } else {
                // Default tenant for unauthenticated/public endpoints
                TenantContext.setTenantId(1L);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
