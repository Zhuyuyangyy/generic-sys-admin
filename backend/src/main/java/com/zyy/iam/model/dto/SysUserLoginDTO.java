package com.zyy.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Data Transfer Object for user authentication (login).
 * <p>
 * Receives credentials from the presentation layer and validates
 * format requirements before processing by the authentication service.
 *
 * @author System Architect
 * @version 1.0.0
 */
@Data
public class SysUserLoginDTO {

    /**
     * Username or email for authentication.
     */
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_@.-]*$", message = "Invalid username format")
    private String username;

    /**
     * Plain-text password (will be verified against BCrypt hash).
     */
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional CAPTCHA verification code.
     */
    private String captchaCode;

    /**
     * CAPTCHA session/token identifier.
     */
    private String captchaKey;
}
