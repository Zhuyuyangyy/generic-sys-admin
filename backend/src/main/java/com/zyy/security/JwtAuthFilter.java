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
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * 每次请求都检查Header中的Authorization: Bearer <token>
 * 
 * 解析JWT中的 roles 和 permissions，注入到Spring Security上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validate(token)) {
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);

                Map<String, Object> claims = jwtUtil.parse(token);

                // 收集所有权限：roles + permissions
                Set<SimpleGrantedAuthority> authorities = new HashSet<>();

                // 1. 从 roles claim 添加（Spring Security标准格式）
                @SuppressWarnings("unchecked")
                Collection<String> roles = (Collection<String>) claims.get("roles");
                if (roles != null) {
                    roles.stream()
                            .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                            .forEach(authorities::add);
                }

                // 2. 从 permissions claim 添加
                @SuppressWarnings("unchecked")
                Collection<String> permissions = (Collection<String>) claims.get("permissions");
                if (permissions != null) {
                    permissions.stream()
                            .filter(p -> p != null && !p.isBlank())
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser(userId, username),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT认证成功 | userId:{} | username:{} | authorities:{}", 
                        userId, username, authorities);
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
