-- =============================================
-- V8: Observability and Security Enhancements
-- Add failed_attempts, locked_until columns if not present
-- Add rate_limit_log table for tracking
-- Add security-related indexes
-- =============================================

-- Add failed_attempts column to sys_user if not exists
ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `failed_attempts` INT NOT NULL DEFAULT 0 COMMENT 'Consecutive failed login attempts' AFTER `password_changed_at`;

-- Add locked_until column to sys_user if not exists
ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `locked_until` DATETIME DEFAULT NULL COMMENT 'Account lockout expiration timestamp' AFTER `failed_attempts`;

-- Add password_changed_at column to sys_user if not exists
ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `password_changed_at` DATETIME DEFAULT NULL COMMENT 'Password last change timestamp' AFTER `locked_until`;

-- Rate limit tracking table
CREATE TABLE IF NOT EXISTS `rate_limit_log` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Log ID',
  `ip_address`     VARCHAR(64)      NOT NULL                COMMENT 'Client IP address',
  `endpoint`       VARCHAR(255)     NOT NULL                COMMENT 'Requested endpoint',
  `request_method` VARCHAR(10)      NOT NULL                COMMENT 'HTTP method',
  `limit_type`     VARCHAR(32)      NOT NULL DEFAULT 'API'  COMMENT 'Rate limit type: API, LOGIN',
  `current_count`  INT              NOT NULL DEFAULT 0      COMMENT 'Request count in window',
  `limit_threshold` INT             NOT NULL DEFAULT 100    COMMENT 'Configured limit threshold',
  `blocked`        TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Whether request was blocked: 0=no, 1=yes',
  `user_agent`     VARCHAR(512)     DEFAULT NULL            COMMENT 'User-Agent header',
  `create_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Log timestamp',
  PRIMARY KEY (`id`),
  KEY `idx_ip_address` (`ip_address`),
  KEY `idx_limit_type` (`limit_type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_blocked` (`blocked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rate limit tracking log';

-- Token blacklist tracking table
CREATE TABLE IF NOT EXISTS `token_blacklist` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Record ID',
  `token_hash`     VARCHAR(128)     NOT NULL                COMMENT 'SHA-256 hash of blacklisted token',
  `user_id`        BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'User ID associated with token',
  `reason`         VARCHAR(64)      NOT NULL DEFAULT 'LOGOUT' COMMENT 'Blacklist reason: LOGOUT, SECURITY, ADMIN',
  `expires_at`     DATETIME         NOT NULL                COMMENT 'Token original expiration time',
  `create_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Blacklist entry timestamp',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token blacklist records';

-- Add index on sys_user for lockout checks
ALTER TABLE `sys_user` ADD INDEX IF NOT EXISTS `idx_locked_until` (`locked_until`);
ALTER TABLE `sys_user` ADD INDEX IF NOT EXISTS `idx_failed_attempts` (`failed_attempts`);

-- Add supplier lead time column for predictive restock
ALTER TABLE `sys_supplier`
    ADD COLUMN IF NOT EXISTS `lead_time_days` INT DEFAULT 7 COMMENT 'Supplier lead time in days' AFTER `description`;
