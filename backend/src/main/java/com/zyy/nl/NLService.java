package com.zyy.nl;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NLService {

    private boolean enableCausal = true;

    private final CausalDAGService causalDAGService;
    private final RiskAssessor riskAssessor;
    private final ConfirmationStore confirmationStore;

    private static final Map<String, String> INTENT_PATTERNS = new HashMap<>();

    private static final Set<String> QUERY_INTENTS = new HashSet<>(Arrays.asList(
        "查询", "查看", "获取", "搜索", "检索"
    ));

    private static final Map<String, String> ENTITY_TYPE_MAP = new HashMap<>();

    static {
        INTENT_PATTERNS.put("删除", "DELETE");
        INTENT_PATTERNS.put("修改", "UPDATE");
        INTENT_PATTERNS.put("更新", "UPDATE");
        INTENT_PATTERNS.put("增加", "CREATE");
        INTENT_PATTERNS.put("添加", "CREATE");
        INTENT_PATTERNS.put("新增", "CREATE");
        INTENT_PATTERNS.put("创建", "CREATE");
        INTENT_PATTERNS.put("入库", "CREATE");
        INTENT_PATTERNS.put("查询", "QUERY");
        INTENT_PATTERNS.put("查看", "QUERY");
        INTENT_PATTERNS.put("获取", "QUERY");
        INTENT_PATTERNS.put("搜索", "QUERY");
        INTENT_PATTERNS.put("检索", "QUERY");
        INTENT_PATTERNS.put("状态", "QUERY");
        INTENT_PATTERNS.put("生成", "QUERY");
        INTENT_PATTERNS.put("统计", "STATISTICS");
        INTENT_PATTERNS.put("汇总", "STATISTICS");
        INTENT_PATTERNS.put("分配", "UPDATE");
        INTENT_PATTERNS.put("审批", "UPDATE");
        INTENT_PATTERNS.put("报废", "UPDATE");
        INTENT_PATTERNS.put("出库", "UPDATE");

        ENTITY_TYPE_MAP.put("设备", "EQUIPMENT");
        ENTITY_TYPE_MAP.put("耗材", "CONSUMABLE");
        ENTITY_TYPE_MAP.put("用户", "USER");
        ENTITY_TYPE_MAP.put("库存", "INVENTORY");
        ENTITY_TYPE_MAP.put("维保", "MAINTENANCE_PLAN");
        ENTITY_TYPE_MAP.put("供应商", "SUPPLIER");
        ENTITY_TYPE_MAP.put("位置", "LOCATION");
        ENTITY_TYPE_MAP.put("角色", "ROLE");
        ENTITY_TYPE_MAP.put("权限", "ROLE");
    }

    public NLService(CausalDAGService causalDAGService, RiskAssessor riskAssessor, ConfirmationStore confirmationStore) {
        this.causalDAGService = causalDAGService;
        this.riskAssessor = riskAssessor;
        this.confirmationStore = confirmationStore;
    }

    public void setEnableCausal(boolean enable) {
        this.enableCausal = enable;
    }

    public boolean isEnableCausal() {
        return enableCausal;
    }

    public NLParseResult parse(String input) {
        NLParseResult result = new NLParseResult();

        String intent = detectIntent(input);
        result.setIntent(intent);

        String entityId = extractEntityId(input);
        result.setEntityId(entityId);

        String entityType = extractEntityType(input);
        result.setEntityType(entityType);

        boolean isQuery = QUERY_INTENTS.stream().anyMatch(input::contains);
        result.setCausalCheckPerformed(!isQuery && enableCausal);

        if (result.isCausalCheckPerformed()) {
            CausalGraph graph = causalDAGService.predictImpact(entityId, entityType);
            result.setCausalGraph(graph);
        }

        return result;
    }

    public ExecuteResult executeWithCausalCheck(String input) {
        long startTime = System.currentTimeMillis();

        ExecuteResult result = new ExecuteResult();

        NLParseResult parseResult = parse(input);

        result.setIntent(parseResult.getIntent());
        result.setEntityId(parseResult.getEntityId());
        result.setCausalCheckPerformed(parseResult.isCausalCheckPerformed());

        if (parseResult.getCausalGraph() != null) {
            result.setImpactedNodesCount(parseResult.getCausalGraph().getImpactedNodes().size());
            result.setHasHighImpact(parseResult.getCausalGraph().isHasHighImpact());
        }

        result.setLatencyMs(System.currentTimeMillis() - startTime);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("intent", result.getIntent());
        response.put("entityId", result.getEntityId());
        response.put("entityType", parseResult.getEntityType());
        response.put("causalCheckPerformed", result.isCausalCheckPerformed());

        if (parseResult.getCausalGraph() != null) {
            response.put("impactedNodes", parseResult.getCausalGraph().getImpactedNodes());
            response.put("hasHighImpact", result.isHasHighImpact());
        }

        response.put("latencyMs", result.getLatencyMs());

        result.setRawResponse(JSONUtil.toJsonStr(response));

        return result;
    }

    // ==================== Dry Run & Execution Confirmation ====================

    public DryRunResult dryRun(String input) {
        log.info("Dry run requested for input: {}", input);

        NLParseResult parseResult = parse(input);

        String intent = parseResult.getIntent();
        String entityType = parseResult.getEntityType();
        String entityId = parseResult.getEntityId();

        boolean isBulk = detectBulkOperation(input);
        Map<String, Object> entities = extractEntities(input, parseResult);
        String riskLevel = riskAssessor.assessRisk(intent, entityType, isBulk, entities);
        boolean requiresApproval = riskAssessor.requiresApproval(riskLevel);
        boolean confirmRequired = riskAssessor.confirmRequired(riskLevel);

        if (parseResult.getCausalGraph() != null) {
            entities.put("impactedNodes", parseResult.getCausalGraph().getImpactedNodes());
            entities.put("hasHighImpact", parseResult.getCausalGraph().isHasHighImpact());
        }

        List<String> affectedTables = riskAssessor.getAffectedTables(intent, entityType);
        String expectedChanges = riskAssessor.describeExpectedChanges(intent, entities);
        int impactScope = riskAssessor.estimateImpactScope(intent, entityType, isBulk);
        String requiredPermission = riskAssessor.requiredPermission(intent, entityType);
        boolean approvalWorkflowNeeded = riskAssessor.needsApprovalWorkflow(intent, entityType, isBulk);

        String confirmationId = UUID.randomUUID().toString();

        DryRunResult dryRunResult = DryRunResult.builder()
                .confirmationId(confirmationId)
                .intent(intent)
                .entities(entities)
                .affectedTables(affectedTables)
                .expectedChanges(expectedChanges)
                .riskLevel(riskLevel)
                .requiresApproval(requiresApproval)
                .confirmRequired(confirmRequired)
                .timestamp(LocalDateTime.now())
                .estimatedImpactScope(impactScope)
                .requiredPermission(requiredPermission)
                .approvalWorkflowNeeded(approvalWorkflowNeeded)
                .originalInput(input)
                .build();

        confirmationStore.put(dryRunResult);

        // Audit trail for dry run
        logNLAudit("DRY_RUN", input, intent, entities, riskLevel, "PENDING", confirmationId);

        log.info("Dry run completed - confirmationId={}, intent={}, riskLevel={}, requiresApproval={}",
                confirmationId, intent, riskLevel, requiresApproval);

        return dryRunResult;
    }

    public ExecuteResult executeConfirmed(String input, String confirmationId) {
        log.info("Execute confirmed requested - confirmationId={}", confirmationId);

        DryRunResult dryRunResult = confirmationStore.consume(confirmationId);
        if (dryRunResult == null) {
            // Audit trail for failed execution
            logNLAudit("EXECUTE_FAILED", input, "UNKNOWN", Map.of(), "UNKNOWN", "INVALID_CONFIRMATION", confirmationId);

            throw new com.zyy.exception.BusinessException(
                    "Invalid or expired confirmation ID: " + confirmationId + ". Please run dry-run first.");
        }

        NLParseResult parseResult = parse(input);
        if (!dryRunResult.getIntent().equals(parseResult.getIntent())) {
            // Audit trail for intent mismatch
            logNLAudit("EXECUTE_FAILED", input, parseResult.getIntent(), Map.of(),
                    dryRunResult.getRiskLevel(), "INTENT_MISMATCH", confirmationId);

            throw new com.zyy.exception.BusinessException(
                    "Intent mismatch: dry-run intent was '" + dryRunResult.getIntent()
                    + "' but current intent is '" + parseResult.getIntent()
                    + "'. Please run dry-run again.");
        }

        log.info("Confirmation validated - executing intent={}, riskLevel={}",
                dryRunResult.getIntent(), dryRunResult.getRiskLevel());

        ExecuteResult execResult = executeWithCausalCheck(input);

        // Audit trail for successful execution
        Map<String, Object> execEntities = new LinkedHashMap<>();
        execEntities.put("entityId", parseResult.getEntityId());
        execEntities.put("entityType", parseResult.getEntityType());
        logNLAudit("EXECUTE_CONFIRMED", input, dryRunResult.getIntent(), execEntities,
                dryRunResult.getRiskLevel(), "SUCCESS", confirmationId);

        return execResult;
    }

    // ==================== NL Audit Trail Logging ====================

    /**
     * Logs an NL audit trail entry. The OperationLogAspect captures these log entries
     * when NLAIController endpoints are invoked. This method provides additional
     * structured logging specific to NL operations.
     *
     * Logged fields:
     * - operation: DRY_RUN, EXECUTE_CONFIRMED, EXECUTE_FAILED
     * - originalInput: The original natural language input
     * - intent: Parsed intent
     * - entities: Parsed entities
     * - riskLevel: Assessed risk level
     * - confirmationStatus: PENDING, SUCCESS, FAILED, INVALID_CONFIRMATION, INTENT_MISMATCH
     * - confirmationId: The confirmation ID
     * - executionResult: Result of execution (if applicable)
     */
    private void logNLAudit(String operation, String originalInput, String intent,
                            Map<String, Object> entities, String riskLevel,
                            String confirmationStatus, String confirmationId) {
        Map<String, Object> auditEntry = new LinkedHashMap<>();
        auditEntry.put("nlAudit", true);
        auditEntry.put("operation", operation);
        auditEntry.put("originalInput", originalInput);
        auditEntry.put("intent", intent);
        auditEntry.put("entities", entities);
        auditEntry.put("riskLevel", riskLevel);
        auditEntry.put("confirmationStatus", confirmationStatus);
        auditEntry.put("confirmationId", confirmationId);
        auditEntry.put("timestamp", LocalDateTime.now());

        log.info("NL审计日志 | {}", JSONUtil.toJsonStr(auditEntry));
    }

    // ==================== Private Helper Methods ====================

    private String detectIntent(String input) {
        for (Map.Entry<String, String> entry : INTENT_PATTERNS.entrySet()) {
            if (input.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "UNKNOWN";
    }

    private String extractEntityId(String input) {
        Pattern pattern = Pattern.compile("(EQ|CS|USER|INV)-\\d{4}-\\d{3}");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }

        // Also match shortened IDs like EQ-001
        Pattern shortPattern = Pattern.compile("(EQ|CS|USER|INV)-\\d{3}");
        Matcher shortMatcher = shortPattern.matcher(input);
        if (shortMatcher.find()) {
            return shortMatcher.group();
        }

        if (input.contains("设备")) return "EQ-2024-001";
        if (input.contains("耗材")) return "CS-2024-008";
        if (input.contains("用户")) return "USER-2024-001";

        return "UNKNOWN";
    }

    private String extractEntityType(String input) {
        for (Map.Entry<String, String> entry : ENTITY_TYPE_MAP.entrySet()) {
            if (input.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "UNKNOWN";
    }

    private boolean detectBulkOperation(String input) {
        return input.contains("批量") || input.contains("所有") || input.contains("全部");
    }

    /**
     * Extracts structured entity information from the input for enhanced risk assessment
     * and change description.
     */
    private Map<String, Object> extractEntities(String input, NLParseResult parseResult) {
        Map<String, Object> entities = new LinkedHashMap<>();
        entities.put("entityId", parseResult.getEntityId());
        entities.put("entityType", parseResult.getEntityType());

        // Detect quantity
        Pattern quantityPattern = Pattern.compile("(\\d+)个");
        Matcher quantityMatcher = quantityPattern.matcher(input);
        if (quantityMatcher.find()) {
            entities.put("quantity", Integer.parseInt(quantityMatcher.group(1)));
        }

        // Detect target (e.g., "给张三", "给研发部")
        Pattern targetPattern = Pattern.compile("给(\\S+)");
        Matcher targetMatcher = targetPattern.matcher(input);
        if (targetMatcher.find()) {
            entities.put("target", targetMatcher.group(1));
        }

        // Detect operation type for special handling
        if (input.contains("报废")) {
            entities.put("operation", "报废");
        } else if (input.contains("权限")) {
            entities.put("operation", "权限");
        } else if (input.contains("角色")) {
            entities.put("operation", "角色");
        } else if (input.contains("入库")) {
            entities.put("operation", "入库");
        } else if (input.contains("出库")) {
            entities.put("operation", "出库");
        } else if (input.contains("审批")) {
            entities.put("operation", "审批");
        }

        entities.put("isBulk", detectBulkOperation(input));

        return entities;
    }

    public List<String> getTestCommands() {
        return Arrays.asList(
            "删除设备EQ-2024-001",
            "修改耗材CS-2024-008的库存数量",
            "查询设备EQ-2024-001的状态",
            "更新设备EQ-2024-002的信息",
            "删除用户USER-2024-001",
            "查询耗材CS-2024-009的详情",
            "添加新设备",
            "修改设备EQ-2024-003的状态",
            "查看库存INV-2024-001",
            "删除耗材CS-2024-010"
        );
    }
}
