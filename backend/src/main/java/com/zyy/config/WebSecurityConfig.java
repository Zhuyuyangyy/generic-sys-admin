package com.zyy.config;

import com.zyy.security.JwtAuthFilter;
import com.zyy.security.SecurityErrorHandlers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置
 *
 * 过滤器链顺序（从外到内）：
 * 1. CorsFilter          — 跨域处理
 * 2. JwtAuthFilter      — JWT认证（登录接口不经过）
 * 3. UsernamePasswordAuthenticationFilter — 表单登录（目前未使用）
 *
 * 安全策略：
 * - CSRF：禁用（前后端分离，JWT无状态）
 * - CORS：允许所有来源（生产环境应限制）
 * - Session：无状态（JWT）
 * - 密码：BCrypt 10轮
 * - 接口：除白名单外全部需要认证
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ========== 基础安全配置 ==========
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 401 未认证 / 403 权限不足，且都返回统一的 Result JSON
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new SecurityErrorHandlers
                            .RestAuthenticationEntryPoint(objectMapper))
                    .accessDeniedHandler(new SecurityErrorHandlers
                            .RestAccessDeniedHandler(objectMapper))
            )
            .authorizeHttpRequests(auth -> auth
                // ========== 公开接口（无需认证） ==========
                // 只放行登录与刷新 token。注册必须指明 HTTP 方法：
                // requestMatchers("/api/users") 会匹配该路径下的所有方法，
                // 导致 GET /api/users（用户列表）也能被匿名访问。
                .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/refresh-token").permitAll()

                // ========== Swagger / Knife4j 文档 ==========
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/doc.html",
                    "/webjars/**",
                    "/favicon.ico"
                ).permitAll()

                // ========== Actuator 健康检查 ==========
                // 仅 health/info 公开；metrics 等端点需管理员
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // ========== 所有其他接口需要认证 ==========
                // 具体接口再用 @PreAuthorize 做权限细化
                .anyRequest().authenticated()
            )

            // ========== 添加JWT过滤器（在UsernamePasswordAuthenticationFilter之前） ==========
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 生产环境应改为具体域名，如：http://localhost:5173
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 密码编码器
     * BCrypt，强度因子10（生产环境建议12）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
