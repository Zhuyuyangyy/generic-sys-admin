-- =============================================
-- V1: Core RBAC Schema (from v1.0__init.sql)
-- Schema only - no seed data
-- =============================================

-- ----------------------------
-- 1. User table (sys_user)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`            BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'User ID, primary key',
  `username`      VARCHAR(64)        NOT NULL                COMMENT 'Username, unique identifier',
  `password`      VARCHAR(255)       NOT NULL                COMMENT 'BCrypt-encoded password',
  `real_name`     VARCHAR(128)       DEFAULT NULL            COMMENT 'Real name',
  `email`         VARCHAR(128)       DEFAULT NULL            COMMENT 'Email address',
  `phone`         VARCHAR(32)        DEFAULT NULL            COMMENT 'Phone number',
  `avatar_url`    VARCHAR(512)       DEFAULT NULL            COMMENT 'Avatar URL',
  `status`        TINYINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=normal, 2=locked',
  `last_login_ip` VARCHAR(64)       DEFAULT NULL            COMMENT 'Last login IP',
  `last_login_at` DATETIME          DEFAULT NULL            COMMENT 'Last login time',
  `create_user`   BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Creator user ID',
  `create_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`    TINYINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System user table';

-- ----------------------------
-- 2. Role table (sys_role)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`            BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'Role ID, primary key',
  `name`          VARCHAR(64)       NOT NULL                COMMENT 'Role name',
  `code`          VARCHAR(64)       NOT NULL                COMMENT 'Role code, unique identifier',
  `description`   VARCHAR(255)      DEFAULT NULL            COMMENT 'Role description',
  `status`        TINYINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=normal',
  `sort_order`    INT UNSIGNED      NOT NULL DEFAULT 0      COMMENT 'Sort order',
  `create_user`   BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Creator user ID',
  `create_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`   DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`    TINYINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System role table';

-- ----------------------------
-- 3. User-Role association table (sys_user_role)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'User ID',
  `role_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'Role ID',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `is_deleted`  TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role association table';

-- ----------------------------
-- 4. Operation log table (sys_operation_log)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Log ID, primary key',
  `user_id`        BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Operating user ID',
  `username`       VARCHAR(64)      DEFAULT NULL            COMMENT 'Operating username (denormalized)',
  `module`         VARCHAR(128)     DEFAULT NULL            COMMENT 'Operation module',
  `operation`      VARCHAR(64)      DEFAULT NULL            COMMENT 'Operation type: INSERT/DELETE/UPDATE/SELECT/LOGIN',
  `target_table`   VARCHAR(128)     DEFAULT NULL            COMMENT 'Target table name',
  `target_id`      VARCHAR(64)     DEFAULT NULL            COMMENT 'Target record ID',
  `method_name`    VARCHAR(256)     DEFAULT NULL            COMMENT 'Java method full qualified name',
  `request_params` TEXT            DEFAULT NULL            COMMENT 'Request parameters (JSON)',
  `request_method` VARCHAR(16)     DEFAULT NULL            COMMENT 'HTTP method',
  `request_url`    VARCHAR(512)     DEFAULT NULL            COMMENT 'Request URL',
  `ip_address`     VARCHAR(64)     DEFAULT NULL            COMMENT 'Client IP address',
  `user_agent`     VARCHAR(512)     DEFAULT NULL            COMMENT 'User-Agent',
  `operation_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
  `duration_ms`    BIGINT UNSIGNED DEFAULT NULL            COMMENT 'Duration in milliseconds',
  `result_status`  TINYINT UNSIGNED DEFAULT NULL            COMMENT 'Result: 0=failure, 1=success',
  `error_detail`   TEXT            DEFAULT NULL            COMMENT 'Error details',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation` (`operation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System operation log table';

-- ----------------------------
-- 5. Menu/permission table (sys_menu)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Menu ID, primary key',
  `parent_id`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT 'Parent menu ID, 0=top level',
  `name`        VARCHAR(64)      NOT NULL                COMMENT 'Menu name',
  `path`        VARCHAR(255)     DEFAULT NULL            COMMENT 'Route path',
  `component`   VARCHAR(255)     DEFAULT NULL            COMMENT 'Frontend component path',
  `icon`        VARCHAR(64)     DEFAULT NULL            COMMENT 'Menu icon',
  `sort_order`  INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT 'Sort order',
  `visible`     TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Visibility: 0=hidden, 1=visible',
  `is_external` TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'External link: 0=no, 1=yes',
  `permission`  VARCHAR(128)    DEFAULT NULL            COMMENT 'Permission identifier',
  `menu_type`   TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Menu type: 1=directory, 2=menu, 3=button',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=enabled',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`  TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  `create_user` BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Creator user ID',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System menu/permission table';

-- ----------------------------
-- 6. Role-Menu association table (sys_role_menu)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'Role ID',
  `menu_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'Menu ID',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `is_deleted`  TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  `create_user` BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Creator user ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-menu association table';

-- ----------------------------
-- 7. Operation log table (application-level, for AOP audit)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Log ID, primary key',
  `module`          VARCHAR(128)     DEFAULT NULL            COMMENT 'Module name',
  `operation`       VARCHAR(64)      DEFAULT NULL            COMMENT 'Operation type',
  `method`          VARCHAR(256)     DEFAULT NULL            COMMENT 'Java method name',
  `request_params`  TEXT             DEFAULT NULL            COMMENT 'Request parameters (JSON)',
  `response_result` TEXT             DEFAULT NULL            COMMENT 'Response result (JSON)',
  `operator_id`     BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Operator user ID',
  `operator_name`   VARCHAR(64)      DEFAULT NULL            COMMENT 'Operator username',
  `ip`              VARCHAR(64)      DEFAULT NULL            COMMENT 'Client IP',
  `duration`        BIGINT           DEFAULT NULL            COMMENT 'Duration in milliseconds',
  `status`          TINYINT          DEFAULT NULL            COMMENT 'Status: 1=success, 0=failure',
  `error_message`   TEXT             DEFAULT NULL            COMMENT 'Error message',
  `create_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Application operation log table';
