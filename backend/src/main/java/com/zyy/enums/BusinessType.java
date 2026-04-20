package com.zyy.enums;

/**
 * 业务操作类型枚举
 * 用于操作日志记录
 */
public enum BusinessType {
    OTHER("其他"),
    INSERT("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    GRANT("授权"),
    REVOKE("撤销"),
    EXPORT("导出"),
    IMPORT("导入"),
    LOGIN("登录"),
    LOGOUT("登出"),
    REGISTER("注册"),
    RESET_PWD("重置密码"),
    CHANGE_PWD("修改密码"),
    ASSIGN_ROLE("分配角色");

    private final String description;

    BusinessType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
