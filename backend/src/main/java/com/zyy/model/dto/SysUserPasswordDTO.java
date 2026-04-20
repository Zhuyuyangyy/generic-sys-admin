package com.zyy.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for password change operations.
 *
 * @author System Architect
 * @version 1.0.0
 */
@Data
public class SysUserPasswordDTO {

    /** Target user ID */
    private Long userId;

    /** Current password for verification */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /** New password to be set */
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 128, message = "Password length must be between 6 and 128 characters")
    private String newPassword;

    /** Confirmation of new password (must match newPassword) */
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
