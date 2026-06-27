package com.zyy.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.config.AppProperties;
import com.zyy.exception.BusinessException;
import com.zyy.exception.UnauthorizedException;
import com.zyy.iam.mapper.SysUserMapper;
import com.zyy.iam.model.dto.SysUserLoginDTO;
import com.zyy.iam.model.dto.SysUserPasswordDTO;
import com.zyy.iam.model.dto.SysUserSaveDTO;
import com.zyy.iam.model.dto.SysUserUpdateDTO;
import com.zyy.iam.model.entity.SysUserEntity;
import com.zyy.common.PageVO;
import com.zyy.iam.model.vo.SysUserLoginVO;
import com.zyy.iam.model.vo.SysUserVO;
import com.zyy.security.JwtUtil;
import com.zyy.iam.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of user management business service.
 * <p>
 * Handles all user-related business operations including authentication,
 * registration, profile management, and account status transitions.
 * Implements account lockout policy after repeated failed authentication attempts.
 * <p>
 * Security mechanisms:
 * <ul>
 *   <li>BCrypt password encoding with configurable cost factor</li>
 *   <li>Account lockout after maxFailedAttempts failures</li>
 *   <li>Audit trail for sensitive operations</li>
 * </ul>
 *
 * @author System Architect
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final AppProperties appProperties;
    private final JwtUtil jwtUtil;

    /** BCrypt password encoder with configured cost factor */
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Initialize password encoder with configured cost factor.
     * Uses lazy initialization to ensure AppProperties is fully wired.
     */
    private BCryptPasswordEncoder getPasswordEncoder() {
        if (passwordEncoder == null) {
            passwordEncoder = new BCryptPasswordEncoder(
                    appProperties.getSecurity().getBcryptCostFactor()
            );
        }
        return passwordEncoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserLoginVO login(SysUserLoginDTO loginDTO, String clientIp) {
        // Query user by username
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getUsername, loginDTO.getUsername())
                        .eq(SysUserEntity::getIsDeleted, 0)
        );

        // Validate user existence
        if (user == null) {
            log.warn("Authentication failed: user not found - username={}, ip={}",
                    maskUsername(loginDTO.getUsername()), clientIp);
            throw new UnauthorizedException("Invalid username or password");
        }

        // Check account status
        if (user.getStatus() == 0) {
            log.warn("Authentication failed: account disabled - userId={}, ip={}",
                    user.getId(), clientIp);
            throw new UnauthorizedException("Account is disabled");
        }

        // Check account lockout
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long remainingMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil());
            log.warn("Authentication failed: account locked - userId={}, ip={}, remainingMinutes={}",
                    user.getId(), clientIp, remainingMinutes);
            throw new UnauthorizedException("Account is locked. Please try again after " + remainingMinutes + " minutes");
        }

        // Verify password
        if (!getPasswordEncoder().matches(loginDTO.getPassword(), user.getPassword())) {
            handleFailedLoginAttempt(user, clientIp);
            throw new UnauthorizedException("Invalid username or password");
        }

        // Successful login - update metadata
        userMapper.updateLoginMetadata(
                user.getId(),
                clientIp,
                Timestamp.valueOf(LocalDateTime.now())
        );

        log.info("User authenticated successfully - userId={}, username={}, ip={}",
                user.getId(), user.getUsername(), clientIp);

        // Generate JWT tokens using centralized JwtUtil
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", user.getTenantId() != null ? user.getTenantId() : 1L);
        claims.put("departmentId", user.getDepartmentId());
        String accessToken = jwtUtil.sign(user.getId(), user.getUsername(), claims);
        String refreshToken = jwtUtil.refresh(user.getId(), user.getUsername(), claims);

        return SysUserLoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(appProperties.getSecurity().getJwtExpiration())
                .tokenType("Bearer")
                .user(entityToVO(user))
                .loginTime(LocalDateTime.now())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO register(SysUserSaveDTO saveDTO, Long operatorId) {
        // Check username uniqueness
        long existingCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getUsername, saveDTO.getUsername())
                        .eq(SysUserEntity::getIsDeleted, 0)
        );

        if (existingCount > 0) {
            throw new BusinessException("Username already exists: " + saveDTO.getUsername());
        }

        // Build new user entity
        SysUserEntity user = new SysUserEntity();
        user.setUsername(saveDTO.getUsername());
        user.setPassword(getPasswordEncoder().encode(saveDTO.getPassword()));
        user.setRealName(saveDTO.getRealName());
        user.setEmail(saveDTO.getEmail());
        user.setPhone(saveDTO.getPhone());
        user.setAvatarUrl(saveDTO.getAvatarUrl());
        user.setStatus(1); // Normal status
        user.setCreateUser(operatorId);
        user.setFailedAttempts(0);

        // Persist to database
        userMapper.insert(user);

        log.info("User registered - userId={}, username={}, operatorId={}",
                user.getId(), user.getUsername(), operatorId);

        return entityToVO(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO updateProfile(SysUserUpdateDTO updateDTO, Long operatorId) {
        SysUserEntity user = userMapper.selectById(updateDTO.getId());
        if (user == null) {
            throw new BusinessException("User not found: " + updateDTO.getId());
        }

        // Apply non-null updates
        if (updateDTO.getRealName() != null) {
            user.setRealName(updateDTO.getRealName());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(updateDTO.getAvatarUrl());
        }
        if (updateDTO.getStatus() != null) {
            user.setStatus(Integer.parseInt(updateDTO.getStatus()));
        }

        userMapper.updateById(user);

        log.info("User profile updated - userId={}, operatorId={}", user.getId(), operatorId);

        return entityToVO(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(SysUserPasswordDTO passwordDTO, Long operatorId) {
        SysUserEntity user = userMapper.selectById(passwordDTO.getUserId());
        if (user == null) {
            throw new BusinessException("User not found: " + passwordDTO.getUserId());
        }

        // Verify current password
        if (!getPasswordEncoder().matches(passwordDTO.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Verify password confirmation
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new BusinessException("Password confirmation does not match");
        }

        // Check password reuse (basic check)
        if (getPasswordEncoder().matches(passwordDTO.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password cannot be the same as the current password");
        }

        // Update password
        String encodedPassword = getPasswordEncoder().encode(passwordDTO.getNewPassword());
        userMapper.resetPassword(passwordDTO.getUserId(), encodedPassword);

        log.info("Password changed - userId={}, operatorId={}", passwordDTO.getUserId(), operatorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysUserVO getById(Long userId) {
        SysUserEntity user = userMapper.selectById(userId);
        return user != null ? entityToVO(user) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysUserVO getByUsername(String username) {
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getUsername, username)
                        .eq(SysUserEntity::getIsDeleted, 0)
        );
        return user != null ? entityToVO(user) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageVO<SysUserVO> getPage(Long pageNum, Long pageSize, String username, Integer status) {
        Page<SysUserEntity> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysUserEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserEntity::getIsDeleted, 0);

        if (username != null && !username.isBlank()) {
            queryWrapper.like(SysUserEntity::getUsername, username);
        }
        if (status != null) {
            queryWrapper.eq(SysUserEntity::getStatus, status);
        }

        queryWrapper.orderByDesc(SysUserEntity::getCreateTime);

        Page<SysUserEntity> result = userMapper.selectPage(page, queryWrapper);

        List<SysUserVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageVO.<SysUserVO>builder()
                .items(voList)
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .isFirst(result.getCurrent() == 1)
                .isLast(result.getCurrent() >= result.getPages())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long userId, boolean enabled, Long operatorId) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found: " + userId);
        }

        user.setStatus(enabled ? 1 : 0);
        userMapper.updateById(user);

        log.info("User status changed - userId={}, enabled={}, operatorId={}",
                userId, enabled, operatorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockAccount(Long userId, Long operatorId) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found: " + userId);
        }

        user.setStatus(2); // Locked
        userMapper.updateById(user);

        log.info("User account locked - userId={}, operatorId={}", userId, operatorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockAccount(Long userId, Long operatorId) {
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found: " + userId);
        }

        user.setStatus(1); // Normal
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);

        log.info("User account unlocked - userId={}, operatorId={}", userId, operatorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long operatorId) {
        // Soft delete
        userMapper.deleteById(userId);
        log.info("User deleted (soft) - userId={}, operatorId={}", userId, operatorId);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Handle failed login attempt with account lockout logic.
     */
    private void handleFailedLoginAttempt(SysUserEntity user, String clientIp) {
        int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
        int maxAttempts = appProperties.getSecurity().getMaxFailedAttempts();

        Timestamp lockedUntil = null;
        if (attempts >= maxAttempts) {
            // Calculate lockout expiration
            int lockoutMinutes = appProperties.getSecurity().getLockoutMinutes();
            lockedUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(lockoutMinutes));
            log.warn("Account locked due to max failed attempts - userId={}, attempts={}, ip={}",
                    user.getId(), attempts, clientIp);
        }

        userMapper.updateFailedAttempts(user.getId(), attempts, lockedUntil);

        log.warn("Failed login attempt - userId={}, attempts={}/{}/max={}, ip={}",
                user.getId(), attempts, attempts >= maxAttempts ? maxAttempts : attempts,
                maxAttempts, clientIp);
    }



    /**
     * Convert entity to view object with data masking.
     */
    private SysUserVO entityToVO(SysUserEntity entity) {
        if (entity == null) {
            return null;
        }

        Integer passwordAgeDays = null;
        if (entity.getPasswordChangedAt() != null) {
            passwordAgeDays = (int) ChronoUnit.DAYS.between(entity.getPasswordChangedAt(), LocalDateTime.now());
        }

        String statusText = switch (entity.getStatus()) {
            case 0 -> "Disabled";
            case 1 -> "Normal";
            case 2 -> "Locked";
            default -> "Unknown";
        };

        return SysUserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .realName(entity.getRealName())
                .email(entity.getEmail())
                .phone(maskPhone(entity.getPhone()))
                .avatarUrl(entity.getAvatarUrl())
                .status(entity.getStatus())
                .statusText(statusText)
                .lastLoginIp(entity.getLastLoginIp())
                .lastLoginAt(entity.getLastLoginAt())
                .createTime(entity.getCreateTime())
                .passwordChangedAt(entity.getPasswordChangedAt())
                .passwordAgeDays(passwordAgeDays)
                .build();
    }

    /**
     * Mask phone number for privacy (show last 4 digits).
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    /**
     * Mask username for log privacy (show first 2 and last 1 character).
     */
    private String maskUsername(String username) {
        if (username == null || username.length() < 3) {
            return username;
        }
        if (username.length() <= 4) {
            return username.charAt(0) + "*".repeat(username.length() - 1);
        }
        return username.substring(0, 2) + "*".repeat(username.length() - 3) + username.charAt(username.length() - 1);
    }
}
