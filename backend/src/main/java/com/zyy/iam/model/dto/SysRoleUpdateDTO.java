package com.zyy.iam.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for updating an existing role.
 */
@Data
public class SysRoleUpdateDTO {

    @NotNull(message = "Role ID is required")
    private Long id;

    @Size(max = 128, message = "Role name cannot exceed 128 characters")
    private String roleName;

    @Size(max = 512, message = "Description cannot exceed 512 characters")
    private String description;

    private Integer status;
}
