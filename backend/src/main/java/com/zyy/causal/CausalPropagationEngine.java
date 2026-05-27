package com.zyy.causal;

import java.util.*;

/**
 * 因果影响传播引擎。
 * <p>基于BFS遍历因果DAG，计算从源节点出发的加权影响权重，
 * 每跳衰减系数默认为0.8。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
public class CausalPropagationEngine {

    /** 每跳衰减系数 */
    private final double decayFactor;

    public CausalPropagationEngine() {
        this(0.8);
    }

    public CausalPropagationEngine(double decayFactor) {
        if (decayFactor <= 0 || decayFactor > 1) {
            throw new IllegalArgumentException("decayFactor must be in (0, 1]");
        }
        this.decayFactor = decayFactor;
    }

    /**
     * 传播影响。
     *
     * @param dag          因果DAG
     * @param sourceNodeId 起始节点ID
     * @return Map：节点ID → 影响权重（权重随跳数递减）
     */
    public Map<String, Double> propagateImpact(CausalDAG dag, String sourceNodeId) {
        Map<String, Double> impact = new LinkedHashMap<>();
        if (dag == null || sourceNodeId == null || !dag.containsNode(sourceNodeId)) {
            return impact;
        }

        Queue<NodeWeight> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(new NodeWeight(sourceNodeId, 1.0));
        visited.add(sourceNodeId);
        impact.put(sourceNodeId, 1.0);

        while (!queue.isEmpty()) {
            NodeWeight current = queue.poll();

            for (CausalEdge edge : dag.getOutgoingEdges(current.nodeId)) {
                String nextId = edge.getToId();
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    double nextWeight = current.weight * decayFactor;
                    if (nextWeight < 0.01) {
                        // 权重过小时停止继续传播，避免无穷小值
                        continue;
                    }
                    impact.put(nextId, nextWeight);
                    queue.offer(new NodeWeight(nextId, nextWeight));
                }
            }
        }

        return impact;
    }

    /** (节点ID, 当前权重) */
    private static class NodeWeight {
        final String nodeId;
        final double weight;
        NodeWeight(String nodeId, double weight) {
            this.nodeId = nodeId;
            this.weight = weight;
        }
    }
}