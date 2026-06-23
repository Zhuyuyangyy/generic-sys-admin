package com.zyy.nl;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RiskAssessor {

    private static final Set<String> LOW_RISK_INTENTS = new HashSet<>(Arrays.asList("QUERY"));
    private static final Set<String> MEDIUM_RISK_INTENTS = new HashSet<>(Arrays.asList("CREATE", "UPDATE"));
    private static final Set<String> HIGH_RISK_INTENTS = new HashSet<>(Arrays.asList("DELETE"));

    private static final Set<String> HIGH_RISK_ENTITY_TYPES = new HashSet<>(Arrays.asList("EQUIPMENT"));

    private static final Set<String> MEDIUM_RISK_ENTITY_TYPES = new HashSet<>(Arrays.asList("CONSUMABLE"));

    private static final Map<String, String> ENTITY_TABLE_MAP = new HashMap<>();

    static {
        ENTITY_TABLE_MAP.put("EQUIPMENT", "sys_equipment");
        ENTITY_TABLE_MAP.put("CONSUMABLE", "sys_consumable");
        ENTITY_TABLE_MAP.put("USER", "sys_user");
        ENTITY_TABLE_MAP.put("INVENTORY", "sys_inventory_record");
    }

    public String assessRisk(String intent, String entityType) {
        if (intent == null) {
            return "LOW";
        }

        if (LOW_RISK_INTENTS.contains(intent)) {
            return "LOW";
        }

        if (HIGH_RISK_INTENTS.contains(intent) && HIGH_RISK_ENTITY_TYPES.contains(entityType)) {
            return "HIGH";
        }

        if (MEDIUM_RISK_INTENTS.contains(intent) && MEDIUM_RISK_ENTITY_TYPES.contains(entityType)) {
            return "MEDIUM";
        }

        if (HIGH_RISK_INTENTS.contains(intent)) {
            return "HIGH";
        }

        if (MEDIUM_RISK_INTENTS.contains(intent)) {
            return "MEDIUM";
        }

        return "LOW";
    }

    public String assessRisk(String intent, String entityType, boolean isBulk) {
        if (isBulk) {
            return "CRITICAL";
        }
        return assessRisk(intent, entityType);
    }

    public boolean requiresApproval(String riskLevel) {
        return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }

    public boolean confirmRequired(String riskLevel) {
        return !"LOW".equals(riskLevel);
    }

    public List<String> getAffectedTables(String entityType) {
        List<String> tables = new ArrayList<>();
        if (entityType != null && ENTITY_TABLE_MAP.containsKey(entityType)) {
            tables.add(ENTITY_TABLE_MAP.get(entityType));
        }
        return tables;
    }

    public String describeExpectedChanges(String intent, String entityType) {
        if (intent == null || entityType == null) {
            return "Unknown operation";
        }

        String entityLabel = entityType;
        switch (intent) {
            case "QUERY":
                return "Read " + entityLabel + " data (no modifications)";
            case "CREATE":
                return "Create a new " + entityLabel + " record";
            case "UPDATE":
                return "Modify existing " + entityLabel + " record(s)";
            case "DELETE":
                return "Delete " + entityLabel + " record(s) — this action may be irreversible";
            default:
                return "Perform " + intent + " on " + entityLabel;
        }
    }
}
