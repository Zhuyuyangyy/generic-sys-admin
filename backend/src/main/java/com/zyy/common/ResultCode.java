package com.zyy.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一状态码枚举
 * 遵循阿里规约：1xxx=系统级，2xxx=认证授权，3xxx=业务参数，4xxx=业务逻辑
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 系统级（1xxx）
    SUCCESS(200, "操作成功"),
    FAIL(500, "服务器内部错误"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问该资源"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // 认证授权（2xxx）
    USERNAME_PASSWORD_MISMATCH(2001, "用户名或密码错误"),
    TOKEN_EXPIRED(2002, "Token已过期，请重新登录"),
    TOKEN_INVALID(2003, "Token无效，请重新登录"),
    ACCOUNT_DISABLED(2004, "账号已被禁用"),
    ACCOUNT_LOCKED(2005, "账号已被锁定"),

    // 业务参数（3xxx）
    PARAM_NOT_NULL(3001, "参数不能为空"),
    PARAM_FORMAT_ERROR(3002, "参数格式错误"),
    PARAM_OUT_OF_RANGE(3003, "参数超出范围"),

    // 业务逻辑（4xxx）
    DATA_NOT_EXIST(4001, "数据不存在"),
    DATA_ALREADY_EXIST(4002, "数据已存在"),
    DATA_CONFLICT(4003, "数据冲突"),
    OPERATION_FAILED(4004, "操作失败");

    private final int code;
    private final String message;
}
