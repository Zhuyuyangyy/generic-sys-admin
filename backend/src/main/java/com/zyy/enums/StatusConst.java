package com.zyy.enums;

/**
 * 状态常量枚举
 * 统一管理所有状态字段的取值，避免魔法数字
 */
public enum StatusConst {

    // ========== 通用状态 ==========
    DISABLED(0, "禁用"),
    NORMAL(1, "正常"),
    LOCKED(2, "锁定"),

    // ========== 设备状态 ==========
    EQUIPMENT_STOCK(0, "库存"),
    EQUIPMENT_IN_USE(1, "使用中"),
    EQUIPMENT_MAINTENANCE(2, "维护中"),
    EQUIPMENT_SCRAPPED(3, "已报废"),

    // ========== 耗材状态 ==========
    CONSUMABLE_UNAVAILABLE(0, "不可用"),
    CONSUMABLE_AVAILABLE(1, "可用"),

    // ========== 维护记录状态 ==========
    RECORD_PENDING(0, "待处理"),
    RECORD_COMPLETED(1, "已完成"),
    RECORD_ANOMALY(2, "发现异常"),

    // ========== 操作日志结果 ==========
    LOG_SUCCESS(1, "成功"),
    LOG_FAILED(0, "失败"),

    // ========== 菜单可见性 ==========
    MENU_HIDDEN(0, "隐藏"),
    MENU_VISIBLE(1, "显示"),

    // ========== 菜单缓存 ==========
    MENU_NOT_CACHEABLE(0, "不缓存"),
    MENU_CACHEABLE(1, "缓存");

    private final int value;
    private final String description;

    StatusConst(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /** 根据value找到枚举，不存在返回null */
    public static StatusConst fromValue(int value) {
        for (StatusConst s : values()) {
            if (s.value == value) return s;
        }
        return null;
    }
}
