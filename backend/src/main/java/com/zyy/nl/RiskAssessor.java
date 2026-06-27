package com.zyy.nl;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RiskAssessor {

    // ==================== Risk Level Constants ====================
    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";
    public static final String RISK_CRITICAL = "CRITICAL";

    // ==================== Intent-based Risk Classification ====================

    private static final Set<String> LOW_RISK_INTENTS = new HashSet<>(Arrays.asList("QUERY", "STATISTICS"));
    private static final Set<String> MEDIUM_RISK_INTENTS = new HashSet<>(Arrays.asList("CREATE", "UPDATE"));
    private static final Set<String> HIGH_RISK_INTENTS = new HashSet<>(Arrays.asList("DELETE"));

    // ==================== Entity Type Risk Modifiers ====================

    private static final Set<String> HIGH_RISK_ENTITY_TYPES = new HashSet<>(Arrays.asList("EQUIPMENT"));

    private static final Set<String> MEDIUM_RISK_ENTITY_TYPES = new HashSet<>(Arrays.asList("CONSUMABLE"));

    // ==================== Critical Operation Keywords ====================

    private static final Set<String> CRITICAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "批量", "所有", "全部", "权限", "角色", "审批", "管理员"
    ));

    private static final Set<String> HIGH_RISK_OPERATION_KEYWORDS = new HashSet<>(Arrays.asList(
            "报废", "删除", "移除"
    ));

    // ==================== Table Mapping ====================

    private static final Map<String, List<String>> ENTITY_TABLE_MAP = new HashMap<>();

    static {
        ENTITY_TABLE_MAP.put("EQUIPMENT", Arrays.asList("sys_equipment", "sys_assignment"));
        ENTITY_TABLE_MAP.put("CONSUMABLE", Arrays.asList("sys_consumable", "sys_inventory_record", "sys_batch"));
        ENTITY_TABLE_MAP.put("USER", Arrays.asList("sys_user", "sys_user_role"));
        ENTITY_TABLE_MAP.put("INVENTORY", Arrays.asList("sys_inventory_record", "sys_inventory_transaction"));
        ENTITY_TABLE_MAP.put("MAINTENANCE_PLAN", Arrays.asList("sys_maintenance_plan"));
        ENTITY_TABLE_MAP.put("SUPPLIER", Arrays.asList("sys_supplier"));
        ENTITY_TABLE_MAP.put("LOCATION", Arrays.asList("sys_location"));
        ENTITY_TABLE_MAP.put("ROLE", Arrays.asList("sys_role", "sys_role_menu"));
    }

    // ==================== Intent × EntityType → Affected Tables ====================

    private static final Map<String, List<String>> INTENT_EXTRA_TABLES = new HashMap<>();

    static {
        INTENT_EXTRA_TABLES.put("DELETE:EQUIPMENT", Arrays.asList("sys_assignment", "sys_inspection"));
        INTENT_EXTRA_TABLES.put("DELETE:CONSUMABLE", Arrays.asList("sys_inventory_record", "sys_batch"));
        INTENT_EXTRA_TABLES.put("UPDATE:ROLE", Arrays.asList("sys_role_menu", "sys_user_role"));
        INTENT_EXTRA_TABLES.put("CREATE:CONSUMABLE", Arrays.asList("sys_inventory_record", "sys_batch"));
    }

    // ==================== Core Risk Assessment ====================

    /**
     * Basic risk assessment based on intent and entity type.
     * Risk levels:
     * - LOW: Query operations, report generation
     * - MEDIUM: Single create/update, normal inbound/outbound
     * - HIGH: Delete operations, equipment scrapping, large stock adjustments (>50 units)
     * - CRITICAL: Batch operations, privilege changes, bypassing approval, bulk outbound
     */
    public String assessRisk(String intent, String entityType) {
        if (intent == null) {
            return RISK_LOW;
        }

        if (LOW_RISK_INTENTS.contains(intent)) {
            return RISK_LOW;
        }

        if (HIGH_RISK_INTENTS.contains(intent) && HIGH_RISK_ENTITY_TYPES.contains(entityType)) {
            return RISK_HIGH;
        }

        if (MEDIUM_RISK_INTENTS.contains(intent) && MEDIUM_RISK_ENTITY_TYPES.contains(entityType)) {
            return RISK_MEDIUM;
        }

        if (HIGH_RISK_INTENTS.contains(intent)) {
            return RISK_HIGH;
        }

        if (MEDIUM_RISK_INTENTS.contains(intent)) {
            return RISK_MEDIUM;
        }

        return RISK_LOW;
    }

    /**
     * Enhanced risk assessment with bulk detection and entity details.
     */
    public String assessRisk(String intent, String entityType, boolean isBulk) {
        if (isBulk && !LOW_RISK_INTENTS.contains(intent)) {
            return RISK_CRITICAL;
        }
        return assessRisk(intent, entityType);
    }

    /**
     * Full risk assessment with entity details for fine-grained rules.
     * Handles:
     * - Equipment scrapping → HIGH
     * - Large stock adjustments (>50 units) → HIGH
     * - Privilege/role changes → CRITICAL
     * - Bulk outbound → CRITICAL
     * - Bulk read operations remain LOW
     */
    public String assessRisk(String intent, String entityType, boolean isBulk, Map<String, Object> entities) {
        // Bulk read operations (QUERY/STATISTICS) remain LOW risk
        if (isBulk && LOW_RISK_INTENTS.contains(intent)) {
            return RISK_LOW;
        }

        if (isBulk) {
            return RISK_CRITICAL;
        }

        // Check for privilege/role changes
        if ("ROLE".equals(entityType) || isPrivilegeChange(entities)) {
            return RISK_CRITICAL;
        }

        // Check for equipment scrapping
        if (isEquipmentScrapping(intent, entities)) {
            return RISK_HIGH;
        }

        // Check for large stock adjustments (>50 units)
        if (isLargeStockAdjustment(entities)) {
            return RISK_HIGH;
        }

        return assessRisk(intent, entityType);
    }

    public boolean requiresApproval(String riskLevel) {
        return RISK_HIGH.equals(riskLevel) || RISK_CRITICAL.equals(riskLevel);
    }

    public boolean confirmRequired(String riskLevel) {
        return !RISK_LOW.equals(riskLevel);
    }

    // ==================== Affected Tables ====================

    /**
     * Returns database tables affected by the given entity type.
     */
    public List<String> getAffectedTables(String entityType) {
        List<String> tables = new ArrayList<>();
        if (entityType != null && ENTITY_TABLE_MAP.containsKey(entityType)) {
            tables.addAll(ENTITY_TABLE_MAP.get(entityType));
        }
        return tables;
    }

    /**
     * Returns database tables affected by the given intent and entity type.
     * Includes both base entity tables and any cascading tables based on the operation.
     */
    public List<String> getAffectedTables(String intent, String entityType) {
        List<String> tables = new ArrayList<>(getAffectedTables(entityType));

        String key = intent + ":" + entityType;
        if (INTENT_EXTRA_TABLES.containsKey(key)) {
            List<String> extra = INTENT_EXTRA_TABLES.get(key);
            for (String t : extra) {
                if (!tables.contains(t)) {
                    tables.add(t);
                }
            }
        }

        return tables;
    }

    // ==================== Expected Changes Description ====================

    /**
     * Returns human-readable description of expected changes (simple version).
     */
    public String describeExpectedChanges(String intent, String entityType) {
        if (intent == null || entityType == null) {
            return "Unknown operation";
        }

        String entityLabel = entityType;
        switch (intent) {
            case "QUERY":
                return "Read " + entityLabel + " data (no modifications)";
            case "STATISTICS":
                return "Generate statistics/aggregation on " + entityLabel + " data (no modifications)";
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

    /**
     * Returns human-readable description of expected changes with entity details.
     * Provides richer context including quantity, target entity, and operation specifics.
     */
    public String describeExpectedChanges(String intent, Map<String, Object> entities) {
        if (intent == null) {
            return "Unknown operation";
        }

        String entityType = entities != null ? (String) entities.get("entityType") : null;
        String entityId = entities != null ? (String) entities.get("entityId") : null;
        Object quantity = entities != null ? entities.get("quantity") : null;
        Object target = entities != null ? entities.get("target") : null;
        Object operation = entities != null ? entities.get("operation") : null;

        String entityLabel = entityType != null ? entityType : "entity";
        String idLabel = entityId != null ? " (" + entityId + ")" : "";

        StringBuilder desc = new StringBuilder();

        switch (intent) {
            case "QUERY":
                desc.append("Read ").append(entityLabel).append(idLabel).append(" data (no modifications)");
                break;
            case "STATISTICS":
                desc.append("Generate statistics on ").append(entityLabel).append(idLabel).append(" data (no modifications)");
                break;
            case "CREATE":
                desc.append("Create new ").append(entityLabel).append(idLabel).append(" record");
                if (quantity != null) {
                    desc.append(" with quantity: ").append(quantity);
                }
                if (target != null) {
                    desc.append(" for ").append(target);
                }
                break;
            case "UPDATE":
                desc.append("Modify ").append(entityLabel).append(idLabel).append(" record(s)");
                if (operation != null) {
                    desc.append(" — operation: ").append(operation);
                }
                if ("报废".equals(String.valueOf(operation))) {
                    desc.append(" — WARNING: Equipment scrapping is irreversible");
                }
                if (quantity != null) {
                    desc.append(", quantity change: ").append(quantity);
                }
                break;
            case "DELETE":
                desc.append("Delete ").append(entityLabel).append(idLabel)
                   .append(" record(s) — this action is irreversible");
                break;
            default:
                desc.append("Perform ").append(intent).append(" on ").append(entityLabel).append(idLabel);
        }

        if (entities != null && entities.containsKey("isBulk") && Boolean.TRUE.equals(entities.get("isBulk"))) {
            desc.append(" [BULK OPERATION]");
        }

        return desc.toString();
    }

    // ==================== Impact Scope Estimation ====================

    /**
     * Estimates how many records would be affected by the operation.
     */
    public int estimateImpactScope(String intent, String entityType, boolean isBulk) {
        if (RISK_LOW.equals(assessRisk(intent, entityType, isBulk))) {
            return 1; // Read-only, single record lookup
        }
        if (isBulk) {
            return 100; // Bulk operations estimated to affect many records
        }
        switch (intent) {
            case "DELETE":
                return 1; // Typically single record, but cascading may affect more
            case "CREATE":
                return 1; // Single new record
            case "UPDATE":
                return 1; // Single record update
            default:
                return 1;
        }
    }

    // ==================== Permission Check ====================

    /**
     * Checks if the given intent and entity type requires specific permissions.
     * Returns the required permission constant name, or null if no special permission needed.
     */
    public String requiredPermission(String intent, String entityType) {
        if (intent == null) return null;
        switch (intent) {
            case "CREATE":
                return "PERM_CREATE_" + (entityType != null ? entityType : "ENTITY");
            case "UPDATE":
                return "PERM_UPDATE_" + (entityType != null ? entityType : "ENTITY");
            case "DELETE":
                return "PERM_DELETE_" + (entityType != null ? entityType : "ENTITY");
            case "QUERY":
            case "STATISTICS":
                return null; // No special permission for read operations
            default:
                return null;
        }
    }

    /**
     * Determines whether an approval workflow is needed for the operation.
     */
    public boolean needsApprovalWorkflow(String intent, String entityType, boolean isBulk) {
        String risk = assessRisk(intent, entityType, isBulk);
        return requiresApproval(risk);
    }

    // ==================== Private Helper Methods ====================

    private boolean isPrivilegeChange(Map<String, Object> entities) {
        if (entities == null) return false;
        Object operation = entities.get("operation");
        if (operation != null) {
            String op = String.valueOf(operation);
            return op.contains("权限") || op.contains("角色") || op.contains("管理员");
        }
        return false;
    }

    private boolean isEquipmentScrapping(String intent, Map<String, Object> entities) {
        if (!"UPDATE".equals(intent) || entities == null) return false;
        Object operation = entities.get("operation");
        if (operation != null) {
            return "报废".equals(String.valueOf(operation));
        }
        return false;
    }

    private boolean isLargeStockAdjustment(Map<String, Object> entities) {
        if (entities == null) return false;
        Object quantity = entities.get("quantity");
        if (quantity instanceof Number) {
            return ((Number) quantity).intValue() > 50;
        }
        if (quantity != null) {
            try {
                return Integer.parseInt(String.valueOf(quantity)) > 50;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
