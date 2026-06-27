package com.zyy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * 每次请求都检查Header中的Authorization: Bearer <token>
 * Also checks token blacklist before accepting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validate(token)) {
                // Check token blacklist
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("Token is blacklisted, rejecting request");
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);

                List<String> permissions = jwtUtil.parse(token)
                        .get("permissions", List.class);

                // Extract tenantId and departmentId from JWT claims
                Long tenantId = null;
                Object tenantIdObj = jwtUtil.parse(token).get("tenantId");
                if (tenantIdObj != null) {
                    tenantId = ((Number) tenantIdObj).longValue();
                }
                Long departmentId = null;
                Object departmentIdObj = jwtUtil.parse(token).get("departmentId");
                if (departmentIdObj != null) {
                    departmentId = ((Number) departmentIdObj).longValue();
                }

                List<SimpleGrantedAuthority> authorities = permissions == null
                        ? List.of()
                        : permissions.stream()
                              .map(p -> new SimpleGrantedAuthority(p.toString()))
                              .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser(userId, username, tenantId, departmentId, authorities),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT认证成功 | userId:{} | username:{}", userId, username);
            }
        } catch (Exception e) {
            log.warn("JWT认证异常: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
