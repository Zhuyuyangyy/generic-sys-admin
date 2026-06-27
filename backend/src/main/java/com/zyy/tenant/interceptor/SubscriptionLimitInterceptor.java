package com.zyy.tenant.interceptor;

import com.zyy.common.TenantContext;
import com.zyy.tenant.service.SaaSAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that checks tenant subscription limits before allowing
 * resource creation operations.
 *
 * Checks:
 * - POST /api/users: check maxUsers limit
 * - POST /api/equipment: check maxAssets limit
 * - File upload requests: check storage limit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionLimitInterceptor implements HandlerInterceptor {

    private final SaaSAdminService saaSAdminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only check for POST (creation) requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true; // No tenant context, allow through
        }

        String requestURI = request.getRequestURI();

        try {
            // Check user creation limit
            if (requestURI.startsWith("/api/users") && !requestURI.contains("/login")
                    && !requestURI.contains("/logout") && !requestURI.contains("/refresh-token")) {
                saaSAdminService.checkUserLimit(tenantId);
            }

            // Check asset creation limit
            if (requestURI.startsWith("/api/equipment") || requestURI.startsWith("/api/consumables")) {
                saaSAdminService.checkAssetLimit(tenantId);
            }

            // Check storage limit for file uploads
            if (requestURI.contains("/upload") || requestURI.contains("/file")) {
                long contentLength = request.getContentLengthLong();
                if (contentLength > 0) {
                    saaSAdminService.checkStorageLimit(tenantId, contentLength);
                }
            }
        } catch (Exception e) {
            log.warn("Subscription limit check failed - tenantId={}, uri={}, error={}",
                    tenantId, requestURI, e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"" + e.getMessage() + "\",\"data\":null}");
            return false;
        }

        return true;
    }
}
