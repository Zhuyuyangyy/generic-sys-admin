package com.zyy.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyy.enums.BusinessType;
import com.zyy.model.entity.SysOperationLogEntity;
import com.zyy.service.SysOperationLogService;
import com.zyy.websocket.WebSocketService;
import lombok.extern.slf4j.Slf4j;
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
 * 操作日志切面
 * 自动拦截所有REST接口方法，异步记录操作日志到数据库
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final ObjectMapper objectMapper;
    private final SysOperationLogService operationLogService;
    private final WebSocketService webSocketService;

    public OperationLogAspect(ObjectMapper objectMapper,
                              SysOperationLogService operationLogService,
                              WebSocketService webSocketService) {
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
        this.webSocketService = webSocketService;
    }

    /** 切面：所有 Controller 方法 */
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

        // 读取 @Log 注解
        Log logAnnotation = method.getAnnotation(Log.class);
        String module = (logAnnotation != null && hasText(logAnnotation.module()))
                ? logAnnotation.module()
                : inferModule(signature.getDeclaringTypeName());
        String operation = (logAnnotation != null && logAnnotation.operation() != null)
                ? logAnnotation.operation().name()
                : inferOperationType(method.getName());
        String description = (logAnnotation != null) ? logAnnotation.description() : "";

        // 构建日志实体
        SysOperationLogEntity logEntity = new SysOperationLogEntity();
        logEntity.setModule(module);
        logEntity.setOperation(operation);
        logEntity.setMethodName(signature.getDeclaringTypeName() + "." + method.getName());
        logEntity.setRequestMethod(request.getMethod());
        logEntity.setRequestUrl(request.getRequestURI());
        logEntity.setIpAddress(getClientIp(request));
        logEntity.setUserAgent(request.getHeader("User-Agent"));
        logEntity.setOperationTime(LocalDateTime.now());

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.zyy.security.LoginUser loginUser) {
            logEntity.setUserId(loginUser.getUserId());
            logEntity.setUsername(loginUser.getUsername());
        }

        // 请求参数
        try {
            logEntity.setRequestParams(serializeArgs(joinPoint.getArgs()));
        } catch (Exception e) {
            logEntity.setRequestParams("参数序列化失败");
        }

        Object result = null;
        int resultStatus = 1; // 1=成功

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            resultStatus = 0; // 失败
            logEntity.setErrorDetail(truncate(e.getMessage(), 500));
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setDurationMs(costTime);
            logEntity.setResultStatus(resultStatus);

            // 异步持久化到数据库
            try {
                operationLogService.saveLog(logEntity);
            } catch (Exception e) {
                log.error("操作日志保存失败: {}", e.getMessage());
            }

            // WebSocket实时推送（静默失败不影响主流程）
            try {
                String operator = logEntity.getUsername() != null ? logEntity.getUsername() : "unknown";
                String action = module + "-" + operation;
                webSocketService.pushOperationLog(operator, action);
            } catch (Exception e) {
                log.warn("WebSocket推送操作日志失败: {}", e.getMessage());
            }

            // 同时打印到日志（方便调试）
            if (log.isDebugEnabled()) {
                log.debug("操作日志 | {}-{} | {} {} | {}ms | user={}",
                        module, operation, request.getMethod(), request.getRequestURI(),
                        costTime, logEntity.getUsername());
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (!isBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String serializeArgs(Object[] args) {
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
            String name = className.substring(className.lastIndexOf(".") + 1);
            return name.substring(0, name.indexOf("Controller"));
        }
        return "未知模块";
    }

    private String inferOperationType(String methodName) {
        if (methodName.startsWith("add") || methodName.startsWith("create") || methodName.startsWith("save")) {
            return BusinessType.INSERT.name();
        }
        if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return BusinessType.UPDATE.name();
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return BusinessType.DELETE.name();
        }
        if (methodName.startsWith("login")) {
            return BusinessType.LOGIN.name();
        }
        if (methodName.startsWith("export")) {
            return BusinessType.EXPORT.name();
        }
        if (methodName.startsWith("import")) {
            return BusinessType.IMPORT.name();
        }
        return BusinessType.OTHER.name();
    }

    /** 判断字符串是否有文本 */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /** 判断字符串是否为空白 */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /** 截断字符串到指定长度 */
    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen);
    }
}