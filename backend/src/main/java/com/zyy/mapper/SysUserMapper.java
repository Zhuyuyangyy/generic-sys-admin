package com.zyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyy.model.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis-Plus Mapper interface for sys_user persistence operations.
 * <p>
 * Provides type-safe access to database operations for the sys_user table.
 * All queries use parameterized statements to prevent SQL injection.
 *
 * @author System Architect
 * @see com.zyy.model.entity.SysUserEntity
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    /**
     * Update login metadata for a user after successful authentication.
     *
     * @param userId    User identifier
     * @param loginIp   Client IP address
     * @param loginTime Login timestamp
     * @return Number of rows affected
     */
    @Update("UPDATE sys_user SET last_login_ip = #{loginIp}, last_login_at = #{loginTime}, " +
            "failed_attempts = 0, locked_until = NULL, update_time = NOW() WHERE id = #{userId}")
    int updateLoginMetadata(@Param("userId") Long userId,
                            @Param("loginIp") String loginIp,
                            @Param("loginTime") java.sql.Timestamp loginTime);

    /**
     * Increment failed login attempts counter and optionally set lockout.
     *
     * @param userId       User identifier
     * @param attempts     New failed attempts count
     * @param lockedUntil  Lockout expiration timestamp (nullable)
     * @return Number of rows affected
     */
    @Update("UPDATE sys_user SET failed_attempts = #{attempts}, locked_until = #{lockedUntil}, " +
            "update_time = NOW() WHERE id = #{userId}")
    int updateFailedAttempts(@Param("userId") Long userId,
                              @Param("attempts") Integer attempts,
                              @Param("lockedUntil") java.sql.Timestamp lockedUntil);

    /**
     * Reset password with BCrypt encoding.
     *
     * @param userId         User identifier
     * @param encodedPassword New BCrypt-encoded password
     * @return Number of rows affected
     */
    @Update("UPDATE sys_user SET password = #{encodedPassword}, password_changed_at = NOW(), " +
            "failed_attempts = 0, locked_until = NULL, update_time = NOW() WHERE id = #{userId}")
    int resetPassword(@Param("userId") Long userId, @Param("encodedPassword") String encodedPassword);
}
