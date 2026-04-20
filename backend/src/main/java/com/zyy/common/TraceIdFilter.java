package com.zyy.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP request filter for distributed tracing identifier injection.
 * <p>
 * Generates a unique traceId (UUID) for each incoming request and injects
 * it into the MDC (Mapped Diagnostic Context) for log correlation.
 * The traceId is also propagated to the response via the X-TraceId header.
 * <p>
 * This filter executes at highest precedence to ensure all downstream
 * components (Controllers, Services, Mappers) have access to the traceId
 * via the SLF4J MDC.
 *
 * @author System Architect
 * @version 1.0.0
 * @see Result#getTraceId()
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-TraceId";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveOrGenerateTraceId(request);
        
        try {
            // Inject into MDC for log correlation
            org.slf4j.MDC.put(TRACE_ID_MDC_KEY, traceId);
            
            // Propagate to response headers
            response.setHeader(TRACE_ID_HEADER, traceId);
            
            // Also set as request attribute for access in controllers
            request.setAttribute("traceId", traceId);
            
            log.debug("Request traceId set: {}", traceId);
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent thread pool contamination
            org.slf4j.MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * Extract traceId from incoming request header, or generate a new UUID.
     *
     * @param request the HTTP request
     * @return traceId string
     */
    private String resolveOrGenerateTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(incoming)) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
