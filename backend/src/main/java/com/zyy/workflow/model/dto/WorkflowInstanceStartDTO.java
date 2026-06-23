package com.zyy.workflow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowInstanceStartDTO {

    @NotNull(message = "Definition ID is required")
    private Long definitionId;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @NotNull(message = "Business ID is required")
    private Long businessId;

    @NotBlank(message = "Title is required")
    private String title;
}
