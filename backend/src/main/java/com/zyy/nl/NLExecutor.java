package com.zyy.nl;

import com.zyy.service.ConsumableService;
import com.zyy.service.EquipmentService;
import com.zyy.service.InventoryRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自然语言执行器。
 *
 * <p><b>安全设计</b></p>
 * 原始实现通过 {@code svc.getClass().getMethod(name)} 反射调用，方法名与实体ID
 * 都来自自然语言解析结果，等于是把用户可控字符串直接交给反射层。
 * 现改为：
 * <ul>
 *   <li>显式白名单表 —— 只有列在此处的 (method, service, action, permission, needsId)
 *       组合可执行，其余一律拒绝；</li>
 *   <li>实体ID 走 {@code PREFIX-数字} 严格解析，拒绝 "EQ-../1" 之类可致
 *       NumberFormatException 或路径注入的输入；</li>
 *   <li>每个条目声明执行所需权限，未登录或权限不足时直接拒绝，不进入 Service 层。</li>
 * </ul>
 *
 * @author System Architect
 */
@Slf4j
@Component
public class NLExecutor {

    /** 支持的 NL 命令白名单条目。 */
    public record Command(
            String service,
            String method,
            String action,
            String permission,
            boolean needsId
    ) {}

    /**
     * NL 命令白名单，键为 (service, method)。不同 service 存在同名方法
     * （getById / delete / getPage），单靠方法名无法唯一定位。
     *
     * method 必须与对应 Service 接口的真实方法名一致，否则反射调用会抛
     * NoSuchMethodException 并被吞成"执行失败"——那正是本表要防的静默失效。
     */
    private static final Map<String, Command> ALLOWED = Map.ofEntries(
            // ---- Equipment ----
            Map.entry("equipment|getById", new Command("equipment", "getById", "READ", "equipment:list", true)),
            Map.entry("equipment|getPage", new Command("equipment", "getPage", "READ", "equipment:list", false)),
            Map.entry("equipment|delete",  new Command("equipment", "delete",  "DELETE", "equipment:del", true)),
            // ---- Consumable ----
            Map.entry("consumable|getById",     new Command("consumable", "getById",     "READ",   "consumable:list", true)),
            Map.entry("consumable|getPage",     new Command("consumable", "getPage",     "READ",   "consumable:list", false)),
            Map.entry("consumable|delete",      new Command("consumable", "delete",      "DELETE", "consumable:del",  true)),
            Map.entry("consumable|inbound",     new Command("consumable", "inbound",     "WRITE",  "consumable:in",   true)),
            Map.entry("consumable|outbound",    new Command("consumable", "outbound",    "WRITE",  "consumable:out",  true)),
            Map.entry("consumable|adjustStock", new Command("consumable", "adjustStock", "WRITE",  "consumable:edit", true)),
            // ---- InventoryRecord ----
            Map.entry("inspection|getById", new Command("inspection", "getById", "READ",   "inventory:list", true)),
            Map.entry("inspection|delete",  new Command("inspection", "delete",  "DELETE", "inventory:del",  true))
    );


    /** 非法 entityId 的统一错误信息（避免把原始输入回显给用户）。 */
    private static final String INVALID_ID = "无法识别的实体ID";

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ConsumableService consumableService;

    @Autowired
    private InventoryRecordService inspectionService;

    /**
     * 判断 NL 命令是否在白名单内。
     *
     * @param service  service 标识（equipment / consumable / inspection）
     * @param method   Service 接口的真实方法名
     */
    public boolean isSupported(String service, String method) {
        return ALLOWED.containsKey(key(service, method));
    }

    /**
     * 查询白名单条目。
     *
     * @return descriptor，未登记时返回 null
     */
    public Command describe(String service, String method) {
        return ALLOWED.get(key(service, method));
    }

    private static String key(String service, String method) {
        return service + "|" + method;
    }

    /**
     * 执行 NL 命令。
     *
     * <p>顺序：白名单 → 认证 → 授权 → 参数校验 → 调用 service。</p>
     *
     * @param service  service 标识（equipment / consumable / inspection）
     * @param method   Service 接口的真实方法名，必须在白名单内
     * @param entityId 实体ID（如 "EQ-1"），方法不需要 ID 时传 null
     * @return 方法返回值；不支持或参数非法时返回错误描述字符串（不抛异常，便于 NL 场景展示）
     */
    public Object dispatch(String service, String method, String entityId) {
        Command cmd = describe(service, method);
        if (cmd == null) {
            log.warn("NL dispatch rejected, command not whitelisted: {}|{}", service, method);
            return "该操作暂不支持通过自然语言执行";
        }

        // 未登录或缺少权限的调用者不进入实体 ID 解析阶段——否则匿名请求会得到
        // "无法识别的实体ID"，既泄露参数校验细节，也违背 fail-closed。
        String denied = checkAuthority(cmd);
        if (denied != null) {
            log.warn("NL dispatch forbidden | command={}|{} | required={} | user={}",
                    service, method, cmd.permission(), currentUsername());
            return denied;
        }

        try {
            Long numericId = parseEntityId(entityId);
            if (cmd.needsId() && numericId == null) {
                return INVALID_ID;
            }

            Object output = switch (cmd.service()) {
                case "equipment" ->
                        cmd.needsId()
                                ? reflect(equipmentService, cmd.method(), numericId)
                                : reflect(equipmentService, cmd.method());
                case "consumable" ->
                        cmd.needsId()
                                ? reflect(consumableService, cmd.method(), numericId)
                                : reflect(consumableService, cmd.method());
                case "inspection" ->
                        cmd.needsId()
                                ? reflect(inspectionService, cmd.method(), numericId)
                                : reflect(inspectionService, cmd.method());
                default -> throw new IllegalStateException("未知 service: " + cmd.service());
            };
            return output != null ? output.toString() : "执行完成，无返回数据";
        } catch (IllegalArgumentException e) {
            log.warn("NL dispatch rejected, malformed entity id for {}: {}", cmd.method(), entityId);
            return INVALID_ID;
        } catch (Exception e) {
            log.warn("NL dispatch failed for {}: {}", cmd.method(), e.getMessage());
            return "执行失败，请检查输入或稍后重试";
        }
    }

    /**
     * 严格解析 {@code <PREFIX>-<digits>} 形式的实体ID。
     *
     * @return 纯数字部分转成的 Long；输入不含 '-' 或数字部分非法时抛
     *         {@link IllegalArgumentException}
     */
    /**
     * 校验当前调用者是否持有命令声明的权限。
     *
     * @return null 表示放行；否则返回给用户看的拒绝消息
     */
    private String checkAuthority(Command cmd) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "未登录，无法执行该操作";
        }
        boolean allowed = auth.getAuthorities().stream()
                .anyMatch(a -> cmd.permission().equals(a.getAuthority()));
        if (!allowed) {
            return "没有执行该操作的权限：" + cmd.permission();
        }
        return null;
    }

    /** 当前登录用户名，仅用于安全审计日志。 */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return "anonymous";
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof com.zyy.security.LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return String.valueOf(principal);
    }

    private Long parseEntityId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        int dash = entityId.indexOf('-');
        if (dash < 0 || dash == entityId.length() - 1) {
            throw new IllegalArgumentException("missing numeric part: " + entityId);
        }
        String digits = entityId.substring(dash + 1);
        if (!digits.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("non-numeric id part: " + entityId);
        }
        try {
            return Long.valueOf(digits);
        } catch (NumberFormatException overflow) {
            throw new IllegalArgumentException("id out of range: " + entityId);
        }
    }

    /** 按精确方法名反射调用（单 Long 参）。 */
    private Object reflect(Object svc, String methodName, Long id) throws Exception {
        return svc.getClass().getMethod(methodName, Long.class).invoke(svc, id);
    }

    /** 按精确方法名反射调用（无参）。 */
    private Object reflect(Object svc, String methodName) throws Exception {
        return svc.getClass().getMethod(methodName).invoke(svc);
    }

    /** 供日志/审计使用的动作分类。 */
    public static String classify(String service, String method) {
        Command c = ALLOWED.get(service + "|" + method);
        return c == null ? "UNKNOWN" : c.action();
    }

    /** 白名单大小，供测试断言。 */
    public static int allowedSize() {
        return ALLOWED.size();
    }
}
