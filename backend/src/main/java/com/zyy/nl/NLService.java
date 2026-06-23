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
        INTENT_PATTERNS.put("查询", "QUERY");
        INTENT_PATTERNS.put("查看", "QUERY");
        INTENT_PATTERNS.put("状态", "QUERY");

        ENTITY_TYPE_MAP.put("设备", "EQUIPMENT");
        ENTITY_TYPE_MAP.put("耗材", "CONSUMABLE");
        ENTITY_TYPE_MAP.put("用户", "USER");
        ENTITY_TYPE_MAP.put("库存", "INVENTORY");
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
        String riskLevel = riskAssessor.assessRisk(intent, entityType, isBulk);
        boolean requiresApproval = riskAssessor.requiresApproval(riskLevel);
        boolean confirmRequired = riskAssessor.confirmRequired(riskLevel);

        Map<String, Object> entities = new LinkedHashMap<>();
        entities.put("entityId", entityId);
        entities.put("entityType", entityType);

        if (parseResult.getCausalGraph() != null) {
            entities.put("impactedNodes", parseResult.getCausalGraph().getImpactedNodes());
            entities.put("hasHighImpact", parseResult.getCausalGraph().isHasHighImpact());
        }

        List<String> affectedTables = riskAssessor.getAffectedTables(entityType);
        String expectedChanges = riskAssessor.describeExpectedChanges(intent, entityType);

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
                .build();

        confirmationStore.put(dryRunResult);

        log.info("Dry run completed - confirmationId={}, intent={}, riskLevel={}, requiresApproval={}",
                confirmationId, intent, riskLevel, requiresApproval);

        return dryRunResult;
    }

    public ExecuteResult executeConfirmed(String input, String confirmationId) {
        log.info("Execute confirmed requested - confirmationId={}", confirmationId);

        DryRunResult dryRunResult = confirmationStore.consume(confirmationId);
        if (dryRunResult == null) {
            throw new com.zyy.exception.BusinessException(
                    "Invalid or expired confirmation ID: " + confirmationId + ". Please run dry-run first.");
        }

        NLParseResult parseResult = parse(input);
        if (!dryRunResult.getIntent().equals(parseResult.getIntent())) {
            throw new com.zyy.exception.BusinessException(
                    "Intent mismatch: dry-run intent was '" + dryRunResult.getIntent()
                    + "' but current intent is '" + parseResult.getIntent()
                    + "'. Please run dry-run again.");
        }

        log.info("Confirmation validated - executing intent={}, riskLevel={}",
                dryRunResult.getIntent(), dryRunResult.getRiskLevel());

        return executeWithCausalCheck(input);
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
