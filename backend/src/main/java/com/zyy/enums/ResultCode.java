package com.zyy.enums;

import lombok.Getter;

/**
 * 业务错误码枚举
 * 统一管理所有业务层面的错误码，便于追踪和国际化
 */
@Getter
public enum ResultCode {

    // ========== 成功 ==========
    SUCCESS(200, "操作成功"),

    // ========== 客户端错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    // ========== 服务器错误 5xx ==========
    INTERNAL_SERVER_ERROR(500, "服务器内部错误，请联系管理员"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用，请稍后再试"),

    // ========== 业务错误（B开头） ==========
    BUSINESS_ERROR(1000, "业务处理失败"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    USER_LOCKED(1003, "用户已被锁定，请稍后再试"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(1005, "用户名或密码错误"),
    PASSWORD_EXPIRED(1006, "密码已过期，请修改密码"),
    PASSWORD_ERROR_LIMIT(1007, "密码错误次数过多，账户已锁定"),

    // 角色相关 2xxx
    ROLE_NOT_FOUND(2001, "角色不存在"),
    ROLE_DISABLED(2002, "角色已被禁用"),
    ROLE_IN_USE(2003, "角色正在使用中，无法删除"),

    // 菜单相关 3xxx
    MENU_NOT_FOUND(3001, "菜单不存在"),
    MENU_HAS_CHILDREN(3002, "该菜单存在子菜单，无法删除"),
    MENU_ROOT_NOT_DELETE(3003, "根菜单无法删除"),

    // 设备相关 4xxx
    EQUIPMENT_NOT_FOUND(4001, "设备不存在"),
    EQUIPMENT_CODE_EXISTS(4002, "设备编号已存在"),
    EQUIPMENT_STATUS_ERROR(4003, "设备状态不允许此操作"),

    // 耗材相关 5xxx
    CONSUMABLE_NOT_FOUND(5001, "耗材不存在"),
    CONSUMABLE_CODE_EXISTS(5002, "耗材编号已存在"),
    CONSUMABLE_STOCK_NOT_ENOUGH(5003, "耗材库存不足"),
    CONSUMABLE_EXPIRED(5004, "耗材已过期"),

    // AI服务相关 6xxx
    AI_SERVICE_ERROR(6001, "AI服务调用失败"),
    AI_SERVICE_TIMEOUT(6002, "AI服务响应超时"),

    // 文件相关 7xxx
    FILE_UPLOAD_ERROR(7001, "文件上传失败"),
    FILE_NOT_FOUND(7002, "文件不存在"),
    FILE_TYPE_NOT_ALLOWED(7003, "文件类型不允许"),
    FILE_SIZE_EXCEED(7004, "文件大小超出限制"),

    // 验证码相关 8xxx
    CAPTCHA_ERROR(8001, "验证码错误"),
    CAPTCHA_EXPIRED(8002, "验证码已过期"),
    CAPTCHA_REQUIRED(8003, "请先获取验证码");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
