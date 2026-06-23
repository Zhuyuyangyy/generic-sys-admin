package com.zyy.workflow.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowDefinitionUpdateDTO {

    private Long id;

    private String definitionName;

    private String description;

    private String workflowType;

    private String steps;

    private Integer status;

    private Integer version;
}
