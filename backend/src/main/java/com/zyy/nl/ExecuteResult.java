package com.zyy.nl;

import lombok.Data;

@Data
public class ExecuteResult {
    private String intent;
    private String entityId;
    private boolean causalCheckPerformed;
    private Integer impactedNodesCount;
    private boolean hasHighImpact;
    private long latencyMs;
    private String rawResponse;
}
