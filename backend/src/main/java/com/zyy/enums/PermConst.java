package com.zyy.enums;

/**
 * 权限常量枚举
 * 定义菜单/按钮/API三级权限体系
 *
 * 权限结构：
 * - 系统管理（system:*）
 *   - 用户管理（system:user:*）
 *     - 用户查询（system:user:list）
 *     - 用户新增（system:user:add）
 *     - 用户修改（system:user:edit）
 *     - 用户删除（system:user:del）

 *   - 角色管理（system:role:*）
 *   - 菜单管理（system:menu:*）
 *   - 操作日志（system:log:*）
 * - 设备管理（equipment:*）
 *   - 设备查询（equipment:list）
 *   - 设备新增（equipment:add）
 *   - 设备修改（equipment:edit）
 *   - 设备删除（equipment:del）
 *   - 设备导出（equipment:export）
 * - 耗材管理（consumable:*）
 *   - 耗材查询（consumable:list）
 *   - 耗材新增（consumable:add）
 *   - 耗材修改（consumable:edit）
 *   - 耗材删除（consumable:del）
 *   - 入库（consumable:in）
 *   - 出库（consumable:out）
 * - AI工作室（ai:*）
 */
public enum PermConst {

    // ========== 系统管理 ==========
    SYSTEM("system", "系统管理"),

    // 用户管理
    USER_LIST("system:user:list", "用户查询"),
    USER_ADD("system:user:add", "用户新增"),
    USER_EDIT("system:user:edit", "用户修改"),
    USER_DEL("system:user:del", "用户删除"),
    USER_RESET_PWD("system:user:resetPwd", "重置密码"),
    USER_ASSIGN_ROLE("system:user:assignRole", "分配角色"),

    // 角色管理
    ROLE_LIST("system:role:list", "角色查询"),
    ROLE_ADD("system:role:add", "角色新增"),
    ROLE_EDIT("system:role:edit", "角色修改"),
    ROLE_DEL("system:role:del", "角色删除"),
    ROLE_GRANT("system:role:grant", "角色授权"),

    // 菜单管理
    // 注意：只有 MENU_LIST 真实使用。MenuController 仅暴露读取端点，
    // 前端 menu.ts 也只有 getCurrentUserMenus。add/edit/del 曾是 seed 中的
    // 幽灵权限（无 Controller、无 Service 用例、无前端调用），已删除——
    // 不为了"权限表看起来完整"而保留无消费者的常量。
    MENU_LIST("system:menu:list", "菜单查询"),

    // 操作日志
    LOG_LIST("system:log:list", "日志查询"),
    LOG_EXPORT("system:log:export", "日志导出"),

    // ========== 设备管理 ==========
    EQUIPMENT("equipment", "设备管理"),
    EQUIPMENT_LIST("equipment:list", "设备查询"),
    EQUIPMENT_ADD("equipment:add", "设备新增"),
    EQUIPMENT_EDIT("equipment:edit", "设备修改"),
    EQUIPMENT_DEL("equipment:del", "设备删除"),
    EQUIPMENT_EXPORT("equipment:export", "设备导出"),
    EQUIPMENT_DETAIL("equipment:detail", "设备详情"),

    // ========== 耗材管理 ==========
    CONSUMABLE("consumable", "耗材管理"),
    CONSUMABLE_LIST("consumable:list", "耗材查询"),
    CONSUMABLE_DETAIL("consumable:detail", "耗材详情"),
    CONSUMABLE_ADD("consumable:add", "耗材新增"),
    CONSUMABLE_EDIT("consumable:edit", "耗材修改"),
    CONSUMABLE_DEL("consumable:del", "耗材删除"),
    CONSUMABLE_IN("consumable:in", "耗材入库"),
    CONSUMABLE_OUT("consumable:out", "耗材出库"),
    CONSUMABLE_EXPORT("consumable:export", "耗材导出"),

    // ========== 巡检记录 ==========
    INVENTORY_LIST("inventory:list", "巡检记录查询"),
    INVENTORY_ADD("inventory:add", "巡检记录新增"),
    INVENTORY_EDIT("inventory:edit", "巡检记录修改"),
    INVENTORY_DEL("inventory:del", "巡检记录删除"),

    // ========== AI工作室 ==========
    AI("ai", "AI工作室"),
    AI_CHAT("ai:chat", "AI对话"),
    AI_TTS("ai:tts", "语音合成"),
    AI_IMAGE("ai:image", "图片生成"),
    AI_MUSIC("ai:music", "音乐生成"),
    AI_VIDEO("ai:video", "视频生成"),

    // ========== 文件存储 ==========
    FILE_UPLOAD("file:upload", "文件上传"),
    FILE_DEL("file:del", "文件删除"),

    // ========== 自然语言业务流 ==========
    NL_EXECUTE("nl:execute", "自然语言指令执行"),

    // ========== 仪表盘 ==========
    DASHBOARD("dashboard", "仪表盘"),
    DASHBOARD_VIEW("dashboard:view", "查看仪表盘");

    private final String code;
    private final String description;

    PermConst(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
