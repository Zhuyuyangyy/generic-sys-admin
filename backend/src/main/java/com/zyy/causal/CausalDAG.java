package com.zyy.causal;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 因果有向无环图（DAG）。
 * <p>支持节点/边的增删、可达性查询、拓扑排序（Kahn算法）。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
public class CausalDAG {

    /** 节点集合：nodeId → CausalNode */
    private final Map<String, CausalNode> nodes = new HashMap<>();

    /** 邻接表：fromId → [toId列表] */
    private final Map<String, List<CausalEdge>> adjacency = new HashMap<>();

    // ════════════════════════════════════════════════════════════
    // 节点操作
    // ════════════════════════════════════════════════════════════

    /**
     * 添加节点。
     *
     * @param node 节点实例
     * @return true 添加成功；false 节点ID已存在
     */
    public boolean addNode(CausalNode node) {
        if (node == null || node.getNodeId() == null) {
            throw new IllegalArgumentException("node or nodeId cannot be null");
        }
        if (nodes.containsKey(node.getNodeId())) {
            return false;
        }
        nodes.put(node.getNodeId(), node);
        adjacency.putIfAbsent(node.getNodeId(), new ArrayList<>());
        return true;
    }

    public boolean containsNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    public CausalNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<CausalNode> getAllNodes() {
        return nodes.values();
    }

    // ════════════════════════════════════════════════════════════
    // 边操作
    // ════════════════════════════════════════════════════════════

    /**
     * 添加有向边。
     *
     * @param fromId     起点节点ID
     * @param toId       终点节点ID
     * @param mechanism  因果机制类型
     * @return true 添加成功；false 节点不存在或边已存在
     */
    public boolean addEdge(String fromId, String toId, CausalMechanism mechanism) {
        if (!nodes.containsKey(fromId) || !nodes.containsKey(toId)) {
            return false;
        }
        // 防止重复添加
        List<CausalEdge> edges = adjacency.get(fromId);
        if (edges.stream().anyMatch(e -> e.getToId().equals(toId))) {
            return false;
        }
        edges.add(new CausalEdge(fromId, toId, mechanism));
        return true;
    }

    /**
     * 获取从指定节点出发的所有边。
     */
    public List<CausalEdge> getOutgoingEdges(String nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 获取指向指定节点的所有边（反向遍历用）。
     */
    public List<CausalEdge> getIncomingEdges(String nodeId) {
        return adjacency.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getToId().equals(nodeId))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════
    // 可达性查询
    // ════════════════════════════════════════════════════════════

    /**
     * BFS获取从sourceNodeId可达的所有节点ID集合（包含自身）。
     *
     * @param sourceNodeId 起始节点ID
     * @return 可达节点ID集合
     */
    public Set<String> getReachableNodes(String sourceNodeId) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(sourceNodeId);
        visited.add(sourceNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (CausalEdge edge : adjacency.getOrDefault(current, Collections.emptyList())) {
                String next = edge.getToId();
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        return visited;
    }

    // ════════════════════════════════════════════════════════════
    // 拓扑排序（Kahn算法）
    // ════════════════════════════════════════════════════════════

    /**
     * Kahn算法拓扑排序。
     *
     * @return 排好序的节点ID列表；图中存在环时返回部分排序结果
     */
    public List<String> topologicalSort() {
        // 入度统计
        Map<String, Integer> inDegree = nodes.keySet().stream()
                .collect(Collectors.toMap(id -> id, id -> 0));

        for (List<CausalEdge> edges : adjacency.values()) {
            for (CausalEdge edge : edges) {
                inDegree.merge(edge.getToId(), 1, Integer::sum);
            }
        }

        // 入度为0的节点入队
        Queue<String> queue = inDegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));

        List<String> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(nodeId);

            for (CausalEdge edge : adjacency.getOrDefault(nodeId, Collections.emptyList())) {
                String toId = edge.getToId();
                int newDegree = inDegree.get(toId) - 1;
                inDegree.put(toId, newDegree);
                if (newDegree == 0) {
                    queue.offer(toId);
                }
            }
        }

        return result;
    }

    // ════════════════════════════════════════════════════════════
    // 辅助
    // ════════════════════════════════════════════════════════════

    public int nodeCount() { return nodes.size(); }

    public String toString() {
        return "CausalDAG{nodes=" + nodes.size() + ", edges=" +
                adjacency.values().stream().mapToInt(List::size).sum() + "}";
    }
}
