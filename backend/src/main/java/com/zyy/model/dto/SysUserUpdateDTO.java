package com.zyy.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for user profile updates.
 * <p>
 * Receives partial update data from the presentation layer.
 * All fields are optional; only non-null values will be applied.
 *
 * @author System Architect
 * @version 1.0.0
 */
@Data
public class SysUserUpdateDTO {

    /** Target user ID for update */
    private Long id;

    /**
     * User's updated real name.
     */
    @Size(max = 128, message = "Real name cannot exceed 128 characters")
    private String realName;

    /**
     * Updated email address.
     */
    @Email(message = "Invalid email format")
    @Size(max = 128, message = "Email cannot exceed 128 characters")
    private String email;

    /**
     * Updated phone number.
     */
    @Pattern(regexp = "^$|^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Size(max = 32, message = "Phone number cannot exceed 32 characters")
    private String phone;

    /**
     * Updated avatar URL.
     */
    @Size(max = 512, message = "Avatar URL cannot exceed 512 characters")
    private String avatarUrl;

    /**
     * Updated account status.
     * 0 = disabled, 1 = normal, 2 = locked
     */
    @Pattern(regexp = "^[012]$", message = "Status must be 0, 1, or 2")
    private String status;
}
