package com.zyy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyy.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 让 Security 边界遵守 HTTP 语义，同时保持项目统一的 {@link Result} 响应体。
 *
 * <p>Spring Security 默认行为对未认证请求返回 403（把"你是谁"和"你能干什么"
 * 混在一起），且 body 是空或 HTML。这里显式区分：</p>
 *
 * <ul>
 *   <li>未认证（anonymous / token 缺失或过期）→ <b>401</b></li>
 *   <li>已认证但权限不足 → <b>403</b></li>
 * </ul>
 *
 * <p>两者都返回 {@code Result} JSON，带 traceId，和业务异常走同一套约定。</p>
 */
public class SecurityErrorHandlers {

    /**
     * 未认证 → 401。在 Security filter 已经开始写响应之前触发。
     */
    public static class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

        private final ObjectMapper objectMapper;

        public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "未认证或凭证已失效，请重新登录");
        }

        void write(HttpServletResponse response, int status, String message) throws IOException {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    objectMapper.writeValueAsString(Result.fail(message)));
        }
    }

    /**
     * 已认证但权限不足 → 403。
     */
    public static class RestAccessDeniedHandler implements AccessDeniedHandler {

        private final ObjectMapper objectMapper;

        public RestAccessDeniedHandler(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException accessDeniedException) throws IOException {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    objectMapper.writeValueAsString(Result.fail("没有执行该操作的权限")));
        }
    }

    private SecurityErrorHandlers() {}
}
