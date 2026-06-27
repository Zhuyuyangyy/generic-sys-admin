package com.zyy.nl;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DryRunResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String confirmationId;

    private String intent;

    private Map<String, Object> entities;

    private List<String> affectedTables;

    private String expectedChanges;

    private String riskLevel;

    private boolean requiresApproval;

    private boolean confirmRequired;

    private LocalDateTime timestamp;

    // ==================== Enhanced Fields ====================

    /** Estimated number of records that would be affected */
    private Integer estimatedImpactScope;

    /** Required permission for this operation, null if none needed */
    private String requiredPermission;

    /** Whether an approval workflow is needed before execution */
    private boolean approvalWorkflowNeeded;

    /** Original natural language input */
    private String originalInput;
}
