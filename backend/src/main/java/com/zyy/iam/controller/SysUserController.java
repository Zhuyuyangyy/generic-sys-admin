package com.zyy.iam.controller;

import com.zyy.common.PageParam;
import com.zyy.common.PageVO;
import com.zyy.common.Result;
import com.zyy.iam.model.dto.SysUserLoginDTO;
import com.zyy.iam.model.dto.SysUserPasswordDTO;
import com.zyy.iam.model.dto.SysUserSaveDTO;
import com.zyy.iam.model.dto.SysUserUpdateDTO;
import com.zyy.iam.model.vo.SysUserLoginVO;
import com.zyy.iam.model.vo.SysUserVO;
import com.zyy.iam.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful controller for user management operations.
 * <p>
 * Implements strict RESTful API standards with plural noun endpoints
 * and proper HTTP method semantics. All responses are wrapped in
 * the unified {@link Result} container.
 * <p>
 * API Design Principles:
 * <ul>
 *   <li>Resource-oriented URLs (nouns, plural form)</li>
 *   <li>Correct HTTP method semantics (GET=query, POST=create, PUT=update, DELETE=remove)</li>
 *   <li>Stateless authentication via Bearer tokens</li>
 *   <li>Consistent error response format</li>
 * </ul>
 *
 * @author System Architect
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "System user authentication and profile management APIs")
public class SysUserController {

    private final SysUserService userService;

    // ==================== Authentication Endpoints ====================

    /**
     * Authenticate user and issue JWT token.
     *
     * @param loginDTO Login credentials
     * @param request  HTTP request for client IP extraction
     * @return Authentication result with JWT token
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user credentials and receive JWT access token")
    public Result<SysUserLoginVO> login(
            @Valid @RequestBody SysUserLoginDTO loginDTO,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        log.debug("Login attempt - username={}, ip={}", loginDTO.getUsername(), clientIp);

        SysUserLoginVO result = userService.login(loginDTO, clientIp);
        return Result.ok(result, "Login successful");
    }

    /**
     * Register a new user account.
     *
     * @param saveDTO Registration data
     * @param request HTTP request for operator context
     * @return Created user profile
     */
    @PostMapping
    @Operation(summary = "Register user", description = "Create a new user account with encoded password")
    public Result<SysUserVO> register(
            @Valid @RequestBody SysUserSaveDTO saveDTO,
            HttpServletRequest request) {

        Long operatorId = getCurrentUserId(request);
        SysUserVO user = userService.register(saveDTO, operatorId);
        return Result.ok(user, "User registered successfully");
    }

    // ==================== Profile Endpoints ====================

    /**
     * Get current authenticated user's profile.
     *
     * @param request HTTP request
     * @return Current user profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieve profile of the currently authenticated user")
    public Result<SysUserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        SysUserVO user = userService.getById(userId);
        return Result.ok(user);
    }

    /**
     * Update current user's profile.
     *
     * @param updateDTO Updated profile data
     * @param request   HTTP request
     * @return Updated profile
     */
    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update profile information for authenticated user")
    public Result<SysUserVO> updateCurrentUser(
            @Valid @RequestBody SysUserUpdateDTO updateDTO,
            HttpServletRequest request) {

        Long userId = getCurrentUserId(request);
        updateDTO.setId(userId);
        SysUserVO user = userService.updateProfile(updateDTO, userId);
        return Result.ok(user, "Profile updated successfully");
    }

    /**
     * Change current user's password.
     *
     * @param passwordDTO Password change request
     * @param request     HTTP request
     * @return Operation result
     */
    @PutMapping("/me/password")
    @Operation(summary = "Change password", description = "Update password for authenticated user")
    public Result<Void> changePassword(
            @Valid @RequestBody SysUserPasswordDTO passwordDTO,
            HttpServletRequest request) {

        Long userId = getCurrentUserId(request);
        passwordDTO.setUserId(userId);
        userService.changePassword(passwordDTO, userId);
        return Result.ok(null, "Password changed successfully");
    }

    // ==================== User Query Endpoints ====================

    /**
     * Get user by unique identifier.
     *
     * @param id User identifier
     * @return User profile
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve user profile by unique identifier")
    public Result<SysUserVO> getById(
            @Parameter(description = "User ID") @PathVariable Long id) {

        SysUserVO user = userService.getById(id);
        return Result.ok(user);
    }

    /**
     * Get paginated user list with optional filters.
     *
     * @param pageParam Pagination parameters
     * @param username  Optional username filter (partial match)
     * @param status    Optional status filter
     * @return Paginated user list
     */
    @GetMapping
    @Operation(summary = "List users", description = "Retrieve paginated list of users with optional filters")
    public Result<PageVO<SysUserVO>> getPage(
            @Valid PageParam pageParam,
            @Parameter(description = "Username filter (partial match)") @RequestParam(required = false) String username,
            @Parameter(description = "Status filter: 0=disabled, 1=normal, 2=locked") @RequestParam(required = false) Integer status) {

        PageVO<SysUserVO> page = userService.getPage(
                pageParam.getPageNum(),
                pageParam.getPageSize(),
                username,
                status
        );
        return Result.ok(page);
    }

    // ==================== User Management Endpoints ====================

    /**
     * Update user profile (admin operation).
     *
     * @param id      Target user ID
     * @param updateDTO Updated profile data
     * @param request HTTP request
     * @return Updated profile
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user profile (admin operation)")
    public Result<SysUserVO> update(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody SysUserUpdateDTO updateDTO,
            HttpServletRequest request) {

        Long operatorId = getCurrentUserId(request);
        updateDTO.setId(id);
        SysUserVO user = userService.updateProfile(updateDTO, operatorId);
        return Result.ok(user, "User updated successfully");
    }

    /**
     * Enable or disable user account.
     *
     * @param id      Target user ID
     * @param enabled  True to enable, false to disable
     * @param request HTTP request
     * @return Operation result
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Set user status", description = "Enable or disable user account")
    public Result<Void> setStatus(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "Enabled status") @RequestParam boolean enabled,
            HttpServletRequest request) {

        Long operatorId = getCurrentUserId(request);
        userService.setStatus(id, enabled, operatorId);
        return Result.ok(null, enabled ? "User enabled" : "User disabled");
    }

    /**
     * Lock user account.
     *
     * @param id      Target user ID
     * @param request HTTP request
     * @return Operation result
     */
    @PostMapping("/{id}/lock")
    @Operation(summary = "Lock user account", description = "Temporarily lock user account due to policy violation")
    public Result<Void> lock(
            @Parameter(description = "User ID") @PathVariable Long id,
            HttpServletRequest request) {

        Long operatorId = getCurrentUserId(request);
        userService.lockAccount(id, operatorId);
        return Result.ok(null, "User account locked");
    }

    /**
     * Unlock user account.
     *
     * @param id      Target user ID
     * @param request HTTP request
     * @return Operation result
     */
    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock user account", description = "Remove account lockout and reset failed attempts")
    public Result<Void> unlock(
            @Parameter(description = "User ID") @PathVariable Long id,
            HttpServletRequest request) {

        Long operatorId = getCurrentUserId(request);
        userService.unlockAccount(id, operatorId);
        return Result.ok(null, "User account unlocked");
    }

    /**
     * Delete user (soft delete).
     *
     * @param id      Target user ID
     * @param request HTTP request
     * @return Operation result
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft delete user account (logical removal)")
    public Result<Void> delete(
            @Parameter(description = "User ID") @PathVariable Long id,
            HttpServletRequest request) {

        Long operatorId = getCurrentUserId(request);
        userService.delete(id, operatorId);
        return Result.ok(null, "User deleted successfully");
    }

    // ==================== Private Helper Methods ====================

    /**
     * Extract client IP address from request headers.
     * Handles proxied requests (X-Forwarded-For, X-Real-IP).
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Extract current user ID from security context.
     * Placeholder implementation - integrate with Spring Security for production.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // In production, extract from Security Context
        // return ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        Object attribute = request.getAttribute("userId");
        if (attribute instanceof Long) {
            return (Long) attribute;
        }
        return 1L; // Default for development
    }
}
