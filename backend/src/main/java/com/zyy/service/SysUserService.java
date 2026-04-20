package com.zyy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.model.dto.SysUserLoginDTO;
import com.zyy.model.dto.SysUserPasswordDTO;
import com.zyy.model.dto.SysUserSaveDTO;
import com.zyy.model.dto.SysUserUpdateDTO;
import com.zyy.model.entity.SysUserEntity;
import com.zyy.model.vo.PageVO;
import com.zyy.model.vo.SysUserLoginVO;
import com.zyy.model.vo.SysUserVO;

/**
 * System user business service interface.
 * <p>
 * Defines core user management operations including authentication,
 * CRUD operations, and password management. Implementations must
 * handle all business logic and transaction management.
 *
 * @author System Architect
 * @version 1.0.0
 */
public interface SysUserService {

    /**
     * Authenticate user credentials and issue JWT token.
     *
     * @param loginDTO Login credentials (username/password)
     * @param clientIp Client IP address for audit logging
     * @return Authentication response containing JWT token and user profile
     * @throws com.zyy.exception.BusinessException if credentials are invalid
     * @throws com.zyy.exception.UnauthorizedException if account is locked/disabled
     */
    SysUserLoginVO login(SysUserLoginDTO loginDTO, String clientIp);

    /**
     * 使用 refreshToken 换取新的 accessToken
     */
    SysUserLoginVO refreshToken(String refreshToken);

    /**
     * Register a new user account.
     *
     * @param saveDTO User registration data
     * @param operatorId ID of the operator creating the account (for audit)
     * @return Created user view object
     * @throws com.zyy.exception.BusinessException if username already exists
     */
    SysUserVO register(SysUserSaveDTO saveDTO, Long operatorId);

    /**
     * Update user profile information.
     *
     * @param updateDTO Updated profile data
     * @param operatorId ID of the operator performing the update
     * @return Updated user view object
     */
    SysUserVO updateProfile(SysUserUpdateDTO updateDTO, Long operatorId);

    /**
     * Change user password.
     *
     * @param passwordDTO Password change request
     * @param operatorId ID of the operator performing the change
     * @throws com.zyy.exception.BusinessException if current password is incorrect
     */
    void changePassword(SysUserPasswordDTO passwordDTO, Long operatorId);

    /**
     * Query user by unique identifier.
     *
     * @param userId User identifier
     * @return User view object, or null if not found
     */
    SysUserVO getById(Long userId);

    /**
     * Query user by username.
     *
     * @param username Username to search
     * @return User view object, or null if not found
     */
    SysUserVO getByUsername(String username);

    /**
     * Retrieve paginated user list with filtering and sorting.
     *
     * @param pageNum   Page number (0-indexed)
     * @param pageSize  Number of items per page
     * @param username  Optional username filter (partial match)
     * @param status    Optional status filter
     * @return Paginated user list
     */
    PageVO<SysUserVO> getPage(Long pageNum, Long pageSize, String username, Integer status);

    /**
     * Enable or disable a user account.
     *
     * @param userId     Target user identifier
     * @param enabled    True to enable, false to disable
     * @param operatorId ID of the operator performing the action
     */
    void setStatus(Long userId, boolean enabled, Long operatorId);

    /**
     * Lock a user account due to policy violation or security concern.
     *
     * @param userId     Target user identifier
     * @param operatorId ID of the operator performing the action
     */
    void lockAccount(Long userId, Long operatorId);

    /**
     * Unlock a previously locked user account.
     *
     * @param userId     Target user identifier
     * @param operatorId ID of the operator performing the action
     */
    void unlockAccount(Long userId, Long operatorId);

    /**
     * Delete a user account (logical deletion).
     *
     * @param userId     Target user identifier
     * @param operatorId ID of the operator performing the deletion
     */
    void delete(Long userId, Long operatorId);
}
