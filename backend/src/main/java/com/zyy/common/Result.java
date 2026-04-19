package com.zyy.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回包装类
 * 所有接口返回值统一封装为此结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：200=成功，4xx=客户端错误，5xx=服务端错误 */
    private int code;

    /** 状态描述 */
    private String message;

    /** 返回数据（泛型） */
    private T data;

    /** 时间戳 */
    private long timestamp;

    /** traceId（用于日志追踪） */
    private String traceId;

    // ==================== 快速构建方法 ====================

    public static <T> Result<T> ok() {
        return ok(null, "操作成功");
    }

    public static <T> Result<T> ok(T data) {
        return ok(data, "操作成功");
    }

    public static <T> Result<T> ok(T data, String message) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .traceId(getTraceId())
                .build();
    }

    public static <T> Result<T> fail() {
        return fail(500, "服务器内部错误");
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .traceId(getTraceId())
                .build();
    }

    public static <T> Result<T> build(int code, String message, T data) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .traceId(getTraceId())
                .build();
    }

    private static String getTraceId() {
        return org.slf4j.MDC.get("traceId");
    }
}
