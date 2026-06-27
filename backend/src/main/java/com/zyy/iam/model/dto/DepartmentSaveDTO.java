package com.zyy.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating a new department.
 */
@Data
public class DepartmentSaveDTO {

    @NotBlank(message = "Department name is required")
    @Size(max = 128, message = "Department name cannot exceed 128 characters")
    private String deptName;

    private Long parentId = 0L;

    private Long leaderId;

    private Integer sort = 0;

    private Integer status = 1;
}
