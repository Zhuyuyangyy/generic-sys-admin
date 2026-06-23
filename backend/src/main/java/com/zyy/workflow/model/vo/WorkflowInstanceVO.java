package com.zyy.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowInstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long definitionId;
    private String definitionName;
    private String businessType;
    private Long businessId;
    private String title;
    private Long initiatorId;
    private Integer currentStep;
    private Integer totalSteps;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<WorkflowTaskVO> tasks;
}
