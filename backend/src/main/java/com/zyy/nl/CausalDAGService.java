package com.zyy.nl;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CausalDAGService {

    private static final Map<String, List<String>> CAUSAL_DAG = new HashMap<>();
    
    private static final Set<String> HIGH_IMPACT_TYPES = new HashSet<>(Arrays.asList(
        "EQUIPMENT", "SERVER", "DATABASE"
    ));

    static {
        CAUSAL_DAG.put("EQ-2024-001", Arrays.asList("EQ-2024-002", "CS-2024-008", "INV-2024-001"));
        CAUSAL_DAG.put("EQ-2024-002", Arrays.asList("CS-2024-009", "INV-2024-002"));
        CAUSAL_DAG.put("EQ-2024-003", Arrays.asList("CS-2024-010"));
        
        CAUSAL_DAG.put("CS-2024-008", Arrays.asList("INV-2024-001"));
        CAUSAL_DAG.put("CS-2024-009", Arrays.asList("INV-2024-002"));
        CAUSAL_DAG.put("CS-2024-010", Arrays.asList("INV-2024-003"));
        
        CAUSAL_DAG.put("USER-2024-001", Arrays.asList("EQ-2024-001"));
        CAUSAL_DAG.put("USER-2024-002", Arrays.asList("EQ-2024-002", "CS-2024-008"));
    }

    public CausalGraph predictImpact(String entityId, String entityType) {
        CausalGraph graph = new CausalGraph();
        graph.setNodeId(entityId);
        graph.setNodeType(entityType);
        graph.setEntityId(entityId);
        
        List<String> impacted = CAUSAL_DAG.getOrDefault(entityId, Collections.emptyList());
        graph.setImpactedNodes(impacted);
        graph.setHasHighImpact(isHighImpact(entityType, impacted.size()));
        
        return graph;
    }

    public CausalGraph predictImpactDirect(String entityType) {
        return predictImpact("DIRECT-" + entityType, entityType);
    }

    private boolean isHighImpact(String entityType, int impactedCount) {
        if (HIGH_IMPACT_TYPES.contains(entityType)) {
            return impactedCount >= 2;
        }
        return impactedCount >= 3;
    }

    public boolean isHighImpactType(String entityType) {
        return HIGH_IMPACT_TYPES.contains(entityType);
    }
}
