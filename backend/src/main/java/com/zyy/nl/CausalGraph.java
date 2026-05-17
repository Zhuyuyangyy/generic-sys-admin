package com.zyy.nl;

import lombok.Data;
import java.util.List;

@Data
public class CausalGraph {
    private String nodeId;
    private String nodeType;
    private String entityId;
    private List<String> impactedNodes;
    private boolean hasHighImpact;
}
