package com.zyy.nl;

import com.zyy.causal.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 因果DAG服务（系统级单例）。
 * <p>
 * 在系统初始化时构建企业资源管理的因果图谱：
 * <ul>
 *   <li>设备(EQUIPMENT) ↔ 耗材(CONSUMABLE)  — 外键依赖（设备使用耗材）</li>
 *   <li>耗材(CONSUMABLE) → 库存预警 — 质量传播（库存低于阈值触发告警）</li>
 *   <li>设备(EQUIPMENT) → 巡检记录(INSPECTION) — 外键依赖</li>
 *   <li>设备(EQUIPMENT) → 操作日志 — 观测依赖</li>
 * </ul>
 * </p>
 *
 * <p>
 * 专利对应：§4.2 NL2CDAG算法步骤5-6，因果图谱构建与置信度赋值。
 * </p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Service
public class CausalDAGService {

    private final CausalDAG systemDAG = new CausalDAG();
    private final CausalPropagationEngine propagationEngine = new CausalPropagationEngine(0.8);

    // ── 节点ID常量（与实体ID前缀对齐）────────────────────────
    private static final String NODE_EQ_PREFIX   = "EQ:";
    private static final String NODE_CS_PREFIX   = "CS:";
    private static final String NODE_IN_PREFIX   = "IN:";
    private static final String NODE_LOG         = "SYS:OPERATION_LOG";
    private static final String NODE_ALERT       = "SYS:CONSUMABLE_ALERT";
    private static final String NODE_EQUIPMENT_MGMT = "SYS:EQUIPMENT_MGMT";
    private static final String NODE_CONSUMABLE_MGMT = "SYS:CONSUMABLE_MGMT";

    // ── 系统初始化 ─────────────────────────────────────────

    @PostConstruct
    public void init() {
        buildSystemDAG();
    }

    /**
     * 构建系统级因果图谱。
     *
     * <pre>
     * 设备节点 ──(FOREIGN_KEY)──→ 耗材节点
     *   │                          │
     *   │                          ▼
     *   └─(FOREIGN_KEY)→ 巡检记录 ←┘
     *   │
     *   └─(QUALITY_PROPAGATION)→ 耗材告警
     *
     * 设备/耗材操作 ──(SCHEMA_DEPENDENCY)→ 操作日志
     * </pre>
     */
    private void buildSystemDAG() {
        // ── 系统管理节点 ────────────────────────────────────
        addNode(NODE_EQUIPMENT_MGMT, "SYSTEM_MODULE");
        addNode(NODE_CONSUMABLE_MGMT, "SYSTEM_MODULE");
        addNode(NODE_LOG, "LOG");
        addNode(NODE_ALERT, "ALERT");

        // ── 设备 → 耗材（FOREIGN_KEY：设备关联耗材库存）───────
        //    例：删除设备EQ-1 → 其关联的耗材消耗记录受影响
        addEdge(NODE_EQUIPMENT_MGMT, NODE_CONSUMABLE_MGMT, CausalMechanism.FOREIGN_KEY);
        addEdge(NODE_EQUIPMENT_MGMT, NODE_LOG, CausalMechanism.SCHEMA_DEPENDENCY);
        addEdge(NODE_CONSUMABLE_MGMT, NODE_ALERT, CausalMechanism.QUALITY_PROPAGATION);
        addEdge(NODE_CONSUMABLE_MGMT, NODE_LOG, CausalMechanism.SCHEMA_DEPENDENCY);

        // ── 巡检节点依赖设备 ─────────────────────────────────
        addEdge(NODE_EQUIPMENT_MGMT, NODE_IN_PREFIX + "DEFAULT", CausalMechanism.FOREIGN_KEY);
        addEdge(NODE_IN_PREFIX + "DEFAULT", NODE_LOG, CausalMechanism.SCHEMA_DEPENDENCY);
    }

    private void addNode(String nodeId, String nodeType) {
        CausalNode node = new CausalNode(nodeId, nodeType);
        node.putProperty("system", true);
        systemDAG.addNode(node);
    }

    private void addEdge(String fromId, String toId, CausalMechanism mechanism) {
        if (!systemDAG.containsNode(fromId)) {
            systemDAG.addNode(new CausalNode(fromId, "IMPLICIT"));
        }
        if (!systemDAG.containsNode(toId)) {
            systemDAG.addNode(new CausalNode(toId, "IMPLICIT"));
        }
        systemDAG.addEdge(fromId, toId, mechanism);
    }

    // ── 公开接口 ─────────────────────────────────────────────

    /**
     * 预测对指定实体的操作将影响哪些下游节点。
     *
     * <h3>专利对应：§3.1 核心创新点 + §4.3 因果传导的数学模型</h3>
     * 权利要求书核心证据——将NL解析结果传入因果传播引擎，
     * 计算操作对系统其他模块的加权影响范围。
     *
     * @param entityType 实体类型（EQUIPMENT / CONSUMABLE / INSPECTION）
     * @param entityId   实体ID（如 "EQ-1"，含前缀）
     * @return nodeId → 影响权重；空Map表示无下游影响
     */
    public Map<String, Double> predictImpact(String entityType, String entityId) {
        if (entityType == null) {
            return Collections.emptyMap();
        }

        // 映射实体类型 → 因果图节点
        String sourceNodeId = resolveNodeId(entityType, entityId);
        if (sourceNodeId == null || !systemDAG.containsNode(sourceNodeId)) {
            // 没有精确节点，退化到系统模块级别
            sourceNodeId = entityType.equals("EQUIPMENT") ? NODE_EQUIPMENT_MGMT : NODE_CONSUMABLE_MGMT;
            if (!systemDAG.containsNode(sourceNodeId)) {
                return Collections.emptyMap();
            }
        }

        // BFS传播，收集下游节点+权重
        return propagationEngine.propagateImpact(systemDAG, sourceNodeId);
    }

    /**
     * 将NL实体类型+ID映射为因果图节点ID。
     * 支持通配符节点（如 IN:* → IN:DEFAULT）。
     */
    private String resolveNodeId(String entityType, String entityId) {
        if (entityId == null) {
            return null;
        }
        String normalized = entityId.trim().toUpperCase();
        if (normalized.startsWith("EQ-")) {
            return NODE_EQ_PREFIX + normalized;
        }
        if (normalized.startsWith("CS-")) {
            return NODE_CS_PREFIX + normalized;
        }
        if (normalized.startsWith("IN-")) {
            return NODE_IN_PREFIX + normalized;
        }
        // fallback按类型
        return switch (entityType) {
            case "EQUIPMENT" -> NODE_EQ_PREFIX + entityId.toUpperCase().replace("-", "_");
            case "CONSUMABLE" -> NODE_CS_PREFIX + entityId.toUpperCase().replace("-", "_");
            case "INSPECTION" -> NODE_IN_PREFIX + entityId.toUpperCase().replace("-", "_");
            default -> null;
        };
    }

    // ── 诊断接口（供测试用）──────────────────────────────────

    /** 返回系统因果图的拓扑排序（验证DAG无环） */
    public List<String> getTopologicalOrder() {
        return systemDAG.topologicalSort();
    }

    /** 返回系统因果图节点总数 */
    public int getNodeCount() {
        return systemDAG.nodeCount();
    }

    /** 获取指定节点的直接下游节点 */
    public Set<String> getDirectSuccessors(String nodeId) {
        Set<String> successors = new HashSet<>();
        for (CausalEdge edge : systemDAG.getOutgoingEdges(nodeId)) {
            successors.add(edge.getToId());
        }
        return successors;
    }
}