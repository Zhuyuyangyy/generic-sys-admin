package com.zyy.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统操作日志切面
 * 自动拦截所有REST接口方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.zyy..*Controller.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String module = inferModule(signature.getDeclaringTypeName());
        String operationType = inferOperationType(method.getName());

        // 构建日志对象
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("module", module);
        logEntry.put("operation", operationType);
        logEntry.put("methodName", signature.getDeclaringTypeName() + "." + method.getName());
        logEntry.put("requestMethod", request.getMethod());
        logEntry.put("requestUrl", request.getRequestURI());
        logEntry.put("ipAddress", getClientIp(request));
        logEntry.put("userAgent", request.getHeader("User-Agent"));
        logEntry.put("operationTime", LocalDateTime.now());

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            logEntry.put("userId", loginUser.getUserId());
            logEntry.put("username", loginUser.getUsername());
        }

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            logEntry.put("requestParams", filterSensitiveParams(args));
        }

        Object result = null;
        int resultStatus = 1;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            resultStatus = 0;
            logEntry.put("errorDetail", StringUtils.abbreviate(e.getMessage(), 500));
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logEntry.put("durationMs", duration);
            logEntry.put("resultStatus", resultStatus);
            log.info("操作日志 | {}", logEntry);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return StringUtils.split(ip, ",")[0].trim();
    }

    private String filterSensitiveParams(Object[] args) {
        try {
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof HttpServletRequest 
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile) {
                    continue;
                }
                params.put("arg[" + i + "]", arg);
            }
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "参数序列化失败";
        }
    }

    private String inferModule(String className) {
        if (className.contains("Controller")) {
            return className.substring(className.lastIndexOf(".") + 1, className.indexOf("Controller"));
        }
        return "未知模块";
    }

    private String inferOperationType(String methodName) {
        if (methodName.startsWith("add") || methodName.startsWith("create") || methodName.startsWith("save")) {
            return "INSERT";
        }
        if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        }
        if (methodName.startsWith("get") || methodName.startsWith("query") || methodName.startsWith("find")) {
            return "SELECT";
        }
        if (methodName.startsWith("login")) {
            return "LOGIN";
        }
        if (methodName.startsWith("export")) {
            return "EXPORT";
        }
        if (methodName.startsWith("import")) {
            return "IMPORT";
        }
        return "OTHER";
    }

    // 内部类：日志对象结构
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LoginUser {
        private Long userId;
        private String username;
    }
}
