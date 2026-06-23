package com.zyy.workflow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkflowDefinitionSaveDTO {

    @NotBlank(message = "Definition name is required")
    @Size(max = 128, message = "Definition name cannot exceed 128 characters")
    private String definitionName;

    @NotBlank(message = "Definition code is required")
    @Size(max = 64, message = "Definition code cannot exceed 64 characters")
    private String definitionCode;

    @Size(max = 512, message = "Description cannot exceed 512 characters")
    private String description;

    @NotBlank(message = "Workflow type is required")
    private String workflowType;

    @NotBlank(message = "Steps configuration is required")
    private String steps;

    private Integer status = 1;

    private Integer version = 1;
}
