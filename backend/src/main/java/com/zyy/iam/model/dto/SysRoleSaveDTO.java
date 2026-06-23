package com.zyy.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for creating a new role.
 */
@Data
public class SysRoleSaveDTO {

    @NotBlank(message = "Role code is required")
    @Size(max = 64, message = "Role code cannot exceed 64 characters")
    private String roleCode;

    @NotBlank(message = "Role name is required")
    @Size(max = 128, message = "Role name cannot exceed 128 characters")
    private String roleName;

    @Size(max = 512, message = "Description cannot exceed 512 characters")
    private String description;

    private Integer status;
}
