package com.zyy.causal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 因果图节点。
 * <p>表示因果图中的实体节点，可携带任意属性。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
public class CausalNode {

    private String nodeId;
    private String nodeType;
    private Map<String, Object> properties;

    public CausalNode() {
        this.properties = new HashMap<>();
    }

    public CausalNode(String nodeId, String nodeType) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.properties = new HashMap<>();
    }

    public CausalNode(String nodeId, String nodeType, Map<String, Object> properties) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.properties = properties != null ? properties : new HashMap<>();
    }

    // ── getters & setters ───────────────────────────────────────

    public String getNodeId() { return nodeId; }

    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeType() { return nodeType; }

    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public Map<String, Object> getProperties() { return properties; }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public void putProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    // ── Object ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CausalNode that = (CausalNode) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return "CausalNode{" +
                "nodeId='" + nodeId + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", properties=" + properties +
                '}';
    }
}
