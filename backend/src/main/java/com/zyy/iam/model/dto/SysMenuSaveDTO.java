package com.zyy.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for creating a new menu.
 */
@Data
public class SysMenuSaveDTO {

    private Long parentId;

    @NotBlank(message = "Menu name is required")
    @Size(max = 128, message = "Menu name cannot exceed 128 characters")
    private String menuName;

    @NotBlank(message = "Menu type is required")
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
