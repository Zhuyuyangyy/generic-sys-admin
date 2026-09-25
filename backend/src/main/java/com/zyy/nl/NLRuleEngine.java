package com.zyy.nl;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自然语言规则引擎。
 * <p>将解析结果映射为Service层方法名。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Component
public class NLRuleEngine {

    /**
     * 解析结果：(service, method) 二元组。
     *
     * 历史上这里只返回一个拼造出来的方法名（如 "deleteEquipment"），而真实的
     * Service 接口方法叫 delete(Long, Long) —— 反射必然 NoSuchMethodException，
     * 又被 catch 吞成"执行失败"，于是整条 NL 链路静默失效。现在把 service 与
     * method 一起返回，method 必须与 Service 接口真实签名一致。
     */
    public record Resolved(String service, String method) {}

    /**
     * intent × entityType → (service, method)。
     * method 必须存在于对应 Service 接口，否则 NLExecutor 会拒绝执行。
     */
    private static final Map<String, Resolved> RESOLUTION_MAP = Map.ofEntries(
            // ---- Equipment ----
            Map.entry("QUERY|EQUIPMENT",  new Resolved("equipment", "getById")),
            Map.entry("DELETE|EQUIPMENT", new Resolved("equipment", "delete")),
            // ---- Consumable ----
            Map.entry("QUERY|CONSUMABLE",   new Resolved("consumable", "getById")),
            Map.entry("DELETE|CONSUMABLE",  new Resolved("consumable", "delete")),
            // ---- InventoryRecord ----
            Map.entry("QUERY|INSPECTION",  new Resolved("inspection", "getById")),
            Map.entry("DELETE|INSPECTION", new Resolved("inspection", "delete"))
    );

    /**
     * 把解析结果映射到 (service, method)。
     *
     * @return 已登记的 (service, method)；意图或实体类型不在表内时返回 null，
     *         由调用方给出可读提示。不再兜底拼造方法名。
     */
    public Resolved resolve(NLParser.ParseResult result) {
        if (result == null || result.getIntent() == null || result.getEntityType() == null) {
            return null;
        }
        return RESOLUTION_MAP.get(result.getIntent().name() + "|" + result.getEntityType());
    }

    /**
     * 快捷重载。
     */
    public Resolved resolve(NLIntent intent, String entityType) {
        return resolve(new NLParser.ParseResult(intent, null, entityType));
    }
}