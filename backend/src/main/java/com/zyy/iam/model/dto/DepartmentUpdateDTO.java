package com.zyy.iam.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating a department.
 */
@Data
public class DepartmentUpdateDTO {

    private Long id;

    @Size(max = 128, message = "Department name cannot exceed 128 characters")
    private String deptName;

    private Long parentId;

    private Long leaderId;

    private Integer sort;

    private Integer status;
}
