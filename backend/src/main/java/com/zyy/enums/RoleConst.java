package com.zyy.enums;

/**
 * 角色常量枚举
 * 定义系统所有角色编码，统一管理
 */
public enum RoleConst {

    ADMIN("ROLE_ADMIN", "管理员", "拥有系统全部权限"),
    OPERATOR("ROLE_OPERATOR", "操作员", "日常运营操作权限"),
    VIEWER("ROLE_VIEWER", "查看者", "仅可查看数据，无修改权限");

    private final String code;
    private final String name;
    private final String description;

    RoleConst(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** 根据code找到枚举 */
    public static RoleConst fromCode(String code) {
        for (RoleConst r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }
}
