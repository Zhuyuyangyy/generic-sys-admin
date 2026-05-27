package com.zyy.nl;

import org.springframework.stereotype.Component;

/**
 * 自然语言规则引擎。
 * <p>将解析结果映射为Service层方法名。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Component
public class NLRuleEngine {

    /** intent × entityType → serviceMethod */
    private static final String[][] SERVICE_MAP = {
        // {intent,        entityType,     method}
        {"QUERY",       "EQUIPMENT",    "getEquipmentById"},
        {"QUERY",       "CONSUMABLE",   "getConsumableById"},
        {"QUERY",       "INSPECTION",   "getInspectionById"},
        {"QUERY",       "EQUIPMENT",    "listEquipments"},
        {"CREATE",      "EQUIPMENT",    "createEquipment"},
        {"CREATE",      "CONSUMABLE",   "createConsumable"},
        {"CREATE",      "INSPECTION",   "createInspection"},
        {"UPDATE",      "EQUIPMENT",    "updateEquipment"},
        {"UPDATE",      "CONSUMABLE",   "updateConsumable"},
        {"UPDATE",      "INSPECTION",   "updateInspection"},
        {"DELETE",      "EQUIPMENT",    "deleteEquipment"},
        {"DELETE",      "CONSUMABLE",   "deleteConsumable"},
        {"DELETE",      "INSPECTION",   "deleteInspection"},
        {"STATISTICS",  "EQUIPMENT",    "countEquipments"},
        {"STATISTICS",  "CONSUMABLE",   "countConsumables"},
        {"STATISTICS",  "INSPECTION",   "countInspections"},
    };

    /**
     * 根据解析结果解析出Service方法名。
     *
     * @param result NLParser的解析结果
     * @return Service方法名字符串；无法映射时返回null
     */
    public String resolveService(NLParser.ParseResult result) {
        if (result == null || result.getIntent() == null) {
            return null;
        }

        String intent = result.getIntent().name();
        String entityType = result.getEntityType();

        // 先精确匹配
        for (String[] row : SERVICE_MAP) {
            if (row[0].equals(intent) && row[1].equals(entityType)) {
                return row[2];
            }
        }

        // 有实体但无精确匹配时，尝试按intent+泛型
        if (entityType != null) {
            for (String[] row : SERVICE_MAP) {
                if (row[0].equals(intent) && row[1].equals(entityType)) {
                    return row[2];
                }
            }
        }

        // 有实体无匹配时按intent默认
        return intent + "Entity";
    }

    /**
     * 快捷重载。
     */
    public String resolveService(NLIntent intent, String entityType) {
        return resolveService(new NLParser.ParseResult(intent, null, entityType));
    }
}