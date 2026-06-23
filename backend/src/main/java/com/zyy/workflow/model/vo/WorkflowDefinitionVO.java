package com.zyy.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowDefinitionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String definitionName;
    private String definitionCode;
    private String description;
    private String workflowType;
    private String steps;
    private Integer status;
    private String statusText;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
