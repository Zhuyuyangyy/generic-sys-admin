package com.zyy.iam.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for updating an existing menu.
 */
@Data
public class SysMenuUpdateDTO {

    @NotNull(message = "Menu ID is required")
    private Long id;

    @Size(max = 128, message = "Menu name cannot exceed 128 characters")
    private String menuName;

    private String menuType;

    @Size(max = 256, message = "Path cannot exceed 256 characters")
    private String path;

    @Size(max = 128, message = "Icon cannot exceed 128 characters")
    private String icon;

    @Size(max = 128, message = "Permission cannot exceed 128 characters")
    private String permission;

    private Integer sortOrder;

    private Integer status;
}
