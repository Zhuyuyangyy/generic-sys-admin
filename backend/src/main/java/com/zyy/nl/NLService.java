package com.zyy.nl;

import com.zyy.causal.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NL自然语言业务编排服务 v2（支持因果预测）。
 * <p>
 * 核心方法 {@link #executeWithCausalCheck(String)} 在执行 DELETE/UPDATE 操作前，
 * 自动调用因果传播引擎预测影响范围，消除"操作后才知道影响"的盲区。
 * </p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Service
public class NLService {

    private final HybridNLParser hybridParser;
    private final NLRuleEngine ruleEngine;
    private final NLExecutor executor;
    private final CausalDAGService causalDAGService;

    public NLService(HybridNLParser hybridParser,
                     NLRuleEngine ruleEngine,
                     NLExecutor executor,
                     CausalDAGService causalDAGService) {
        this.hybridParser = hybridParser;
        this.ruleEngine = ruleEngine;
        this.executor = executor;
        this.causalDAGService = causalDAGService;
    }

    /** 因果检查开关（默认开启）。消融实验用：关闭后 executeWithCausalCheck 退化为普通执行。 */
    private volatile boolean enableCausal = true;

    public void setEnableCausal(boolean enable) {
        this.enableCausal = enable;
    }

    public boolean isEnableCausal() {
        return enableCausal;
    }

    /**
     * 标准执行（无因果预测）。
     */
    public String execute(String naturalLanguageInput) {
        NLParser.ParseResult result = hybridParser.parse(naturalLanguageInput);
        if (result.getIntent() == null) {
            return "无法识别的意图，请检查输入格式";
        }
        String method = ruleEngine.resolveService(result);
        if (method == null) {
            return "未找到对应的Service方法";
        }
        String entityId = result.getEntityIds().isEmpty()
                ? null : result.getEntityIds().get(0);
        Object output = executor.dispatch(method, entityId);
        return output != null ? output.toString() : "执行完成，无返回数据";
    }

    /**
     * 执行自然语言指令，并进行因果影响预测。
     * <p>
     * 核心证据方法——串联NL解析与因果传播引擎，支撑专利权利要求书核心主张。
     * </p>
     *
     * <h3>流程：</h3>
     * <ol>
     *   <li>NLParser 解析输入 → 识别意图（intent）+ 实体（entityId）+ 类型（entityType）</li>
     *   <li>若是 DELETE/UPDATE 意图，从 CausalDAGService 获取实体对应节点</li>
     *   <li>CausalPropagationEngine.propagateImpact() 计算下游影响节点及权重</li>
     *   <li>返回"执行结果 + 影响范围"的组合响应</li>
     * </ol>
     *
     * <h3>专利对应：</h3>
     * 权利要求书中"自然语言因果业务流"的核心实现——
     * NL解析结果触发因果传播，实现了"意图识别→因果推断→影响评估"的完整闭环。
     *
     * @param naturalLanguageInput 用户自然语言输入
     * @return 因果预测报告，包含执行结果和影响范围
     */
    public CausalCheckResult executeWithCausalCheck(String naturalLanguageInput) {
        // ── Step 1：NL解析 ──────────────────────────────────────
        NLParser.ParseResult result = hybridParser.parse(naturalLanguageInput);
        if (result.getIntent() == null) {
            return new CausalCheckResult(false, "无法识别的意图", null, Map.of());
        }

        NLIntent intent = result.getIntent();
        String entityType = result.getEntityType();
        String entityId = result.getEntityIds().isEmpty()
                ? null : result.getEntityIds().get(0);

        // ── Step 2：因果预测（仅 DELETE/UPDATE 且因果开关开启时触发）──
        Map<String, Double> impactedNodes = Collections.emptyMap();
        boolean causalCheckPerformed = false;

        if (enableCausal && (intent == NLIntent.DELETE || intent == NLIntent.UPDATE) && entityId != null) {
            causalCheckPerformed = true;
            impactedNodes = causalDAGService.predictImpact(entityType, entityId);
        }

        // ── Step 3：执行实际业务操作 ─────────────────────────────
        String method = ruleEngine.resolveService(result);
        String executionResult;
        if (method == null) {
            executionResult = "未找到对应的Service方法";
        } else {
            Object output = executor.dispatch(method, entityId);
            executionResult = output != null ? output.toString() : "执行完成，无返回数据";
        }

        // ── Step 4：构造因果报告 ──────────────────────────────────
        return new CausalCheckResult(
                causalCheckPerformed,
                executionResult,
                entityId,
                impactedNodes
        );
    }

    // ════════════════════════════════════════════════════════════
    // 内部类：因果检查结果
    // ════════════════════════════════════════════════════════════

    /**
     * 因果检查结果。
     * <p>返回给调用方（Controller/WebSocket），用于展示影响范围或触发人工确认。</p>
     */
    public static class CausalCheckResult {
        /** 是否执行了因果预测 */
        private final boolean causalCheckPerformed;
        /** 实际业务操作的执行结果 */
        private final String executionResult;
        /** 本次操作的实体ID */
        private final String entityId;
        /** 影响节点Map：nodeId → 影响权重（权重越小影响越弱，&lt;0.01停止传播） */
        private final Map<String, Double> impactedNodes;

        public CausalCheckResult(boolean causalCheckPerformed,
                                  String executionResult,
                                  String entityId,
                                  Map<String, Double> impactedNodes) {
            this.causalCheckPerformed = causalCheckPerformed;
            this.executionResult = executionResult;
            this.entityId = entityId;
            this.impactedNodes = impactedNodes != null ? impactedNodes : Collections.emptyMap();
        }

        public boolean isCausalCheckPerformed()   { return causalCheckPerformed; }
        public String getExecutionResult()          { return executionResult; }
        public String getEntityId()                 { return entityId; }
        public Map<String, Double> getImpactedNodes() { return impactedNodes; }

        /** 影响节点数量 */
        public int impactedCount() { return impactedNodes.size(); }

        /**
         * 是否存在高权重影响节点（权重 ≥ 0.5）。
         * 可用于前端"高危操作"红色警示。
         */
        public boolean hasHighImpact() {
            return impactedNodes.values().stream().anyMatch(w -> w >= 0.5);
        }

        /**
         * 影响摘要（用于日志/推送）。
         */
        public String summary() {
            if (!causalCheckPerformed) {
                return "[因果预测未触发] " + executionResult;
            }
            if (impactedNodes.isEmpty()) {
                return "[无下游影响] " + executionResult;
            }
            return String.format("[因果预测] 影响%d个节点，最严重: %s | 操作结果: %s",
                    impactedNodes.size(),
                    highestImpactEntry(),
                    executionResult);
        }

        private String highestImpactEntry() {
            return impactedNodes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (权重=" + String.format("%.2f", e.getValue()) + ")")
                    .orElse("无");
        }
    }
}