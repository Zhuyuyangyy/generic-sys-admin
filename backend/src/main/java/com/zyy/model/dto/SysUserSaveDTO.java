package com.zyy.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for user registration.
 * <p>
 * Receives input from the presentation layer (Controller) and validates
 * all fields using Jakarta Bean Validation (JSR-380) annotations.
 * This DTO is converted to {@link com.zyy.model.entity.SysUserEntity} by the service layer.
 *
 * @author System Architect
 * @version 1.0.0
 */
@Data
public class SysUserSaveDTO {

    /**
     * Username for the new account.
     * Must be unique within the system.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64, message = "Username length must be between 3 and 64 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Username must start with a letter and contain only alphanumeric characters")
    private String username;

    /**
     * Initial password for the account.
     * Will be encoded using BCrypt before persistence.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password length must be between 6 and 128 characters")
    private String password;

    /**
     * User's real name (display name).
     */
    @NotBlank(message = "Real name is required")
    @Size(max = 128, message = "Real name cannot exceed 128 characters")
    private String realName;

    /**
     * Contact email address.
     * Must conform to RFC 5322 email format.
     */
    @Email(message = "Invalid email format")
    @Size(max = 128, message = "Email cannot exceed 128 characters")
    private String email;

    /**
     * Contact phone number.
     * Supports international format with optional + prefix.
     */
    @Pattern(regexp = "^$|^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Size(max = 32, message = "Phone number cannot exceed 32 characters")
    private String phone;

    /**
     * Profile avatar URL (optional).
     * If provided, must be a valid HTTP(S) URL.
     */
    @Size(max = 512, message = "Avatar URL cannot exceed 512 characters")
    private String avatarUrl;
}
