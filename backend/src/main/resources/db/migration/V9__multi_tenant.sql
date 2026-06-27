-- =============================================
-- V9: Multi-Tenant and Department Support
-- Add tenant_id and department_id columns to business tables
-- Create sys_tenant and sys_department tables
-- =============================================

-- Create sys_tenant table
CREATE TABLE IF NOT EXISTS `sys_tenant` (
  `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code`       VARCHAR(64)      NOT NULL                COMMENT 'Unique tenant code',
  `tenant_name`       VARCHAR(128)     NOT NULL                COMMENT 'Tenant display name',
  `contact_person`    VARCHAR(64)      DEFAULT NULL            COMMENT 'Contact person name',
  `contact_email`     VARCHAR(128)     DEFAULT NULL            COMMENT 'Contact email address',
  `contact_phone`     VARCHAR(32)      DEFAULT NULL            COMMENT 'Contact phone number',
  `domain`            VARCHAR(256)     DEFAULT NULL            COMMENT 'Custom domain',
  `logo_url`          VARCHAR(512)     DEFAULT NULL            COMMENT 'Tenant logo URL',
  `subscription_plan` VARCHAR(32)      NOT NULL DEFAULT 'FREE' COMMENT 'Subscription plan: FREE/BASIC/PROFESSIONAL/ENTERPRISE',
  `max_users`         INT              NOT NULL DEFAULT 5      COMMENT 'Maximum allowed users',
  `max_assets`        INT              NOT NULL DEFAULT 100    COMMENT 'Maximum allowed assets',
  `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '1=ACTIVE, 0=SUSPENDED, 2=TERMINATED',
  `expiry_date`       DATETIME         DEFAULT NULL            COMMENT 'Subscription expiry date',
  `create_time`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_user`       BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Creator user ID',
  `tenant_id`         BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT 'Tenant ID (always 1 for sys_tenant)',
  `is_deleted`        TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Logical deletion: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_status` (`status`),
  KEY `idx_subscription_plan` (`subscription_plan`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System tenant table';

-- Create sys_department table
CREATE TABLE IF NOT EXISTS `sys_department` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id`   BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT 'Tenant ID',
  `dept_name`   VARCHAR(128)     NOT NULL                COMMENT 'Department name',
  `parent_id`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT 'Parent department ID, 0=root',
  `leader_id`   BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Department leader user ID',
  `sort`        INT              NOT NULL DEFAULT 0      COMMENT 'Sort order',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT '1=active, 0=disabled',
  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_user` BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Creator user ID',
  `is_deleted`  TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Logical deletion: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_leader_id` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Department table for data scope';

-- Create data_scope table
CREATE TABLE IF NOT EXISTS `data_scope` (
  `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_code`    VARCHAR(64)      NOT NULL                COMMENT 'Role code',
  `scope_type`   VARCHAR(32)      NOT NULL DEFAULT 'SELF' COMMENT 'Scope type: SELF/DEPARTMENT/DEPARTMENT_AND_SUB/ALL/CUSTOM',
  `custom_scope` TEXT             DEFAULT NULL            COMMENT 'Custom scope (JSON array of department IDs)',
  `create_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_user`  BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Creator user ID',
  `tenant_id`    BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT 'Tenant ID',
  `is_deleted`   TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Logical deletion: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Data scope configuration per role';

-- Add tenant_id column to business tables
ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID' AFTER `is_deleted`;
ALTER TABLE `sys_role` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID' AFTER `sort_order`;
ALTER TABLE `sys_menu` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID' AFTER `sort_order`;

-- Add department_id to sys_user
ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Department ID' AFTER `tenant_id`;

-- Add tenant_id to business tables
ALTER TABLE `equipment` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `consumable` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `inventory_record` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `inventory_transaction` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `maintenance_plan` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `sys_supplier` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `stock_alert` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `sys_operation_log` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `workflow_definition` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';
ALTER TABLE `workflow_instance` ADD COLUMN IF NOT EXISTS `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Tenant ID';

-- Add department_id to relevant business tables
ALTER TABLE `equipment` ADD COLUMN IF NOT EXISTS `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Department ID' AFTER `tenant_id`;
ALTER TABLE `consumable` ADD COLUMN IF NOT EXISTS `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Department ID' AFTER `tenant_id`;

-- Add indexes for tenant_id on all tables
ALTER TABLE `sys_user` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_role` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_menu` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `equipment` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `consumable` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `inventory_record` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `inventory_transaction` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `maintenance_plan` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_supplier` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `stock_alert` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_operation_log` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `workflow_definition` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);
ALTER TABLE `workflow_instance` ADD INDEX IF NOT EXISTS `idx_tenant_id` (`tenant_id`);

-- Add indexes for department_id
ALTER TABLE `sys_user` ADD INDEX IF NOT EXISTS `idx_department_id` (`department_id`);
ALTER TABLE `equipment` ADD INDEX IF NOT EXISTS `idx_department_id` (`department_id`);
ALTER TABLE `consumable` ADD INDEX IF NOT EXISTS `idx_department_id` (`department_id`);

-- Seed default tenant
INSERT INTO `sys_tenant` (`id`, `tenant_code`, `tenant_name`, `subscription_plan`, `max_users`, `max_assets`, `status`)
VALUES (1, 'DEFAULT', '默认租户', 'ENTERPRISE', 1000, 10000, 1)
ON DUPLICATE KEY UPDATE `tenant_name` = VALUES(`tenant_name`);

-- Seed default departments for default tenant
INSERT INTO `sys_department` (`id`, `tenant_id`, `dept_name`, `parent_id`, `sort`, `status`)
VALUES (1, 1, '总公司', 0, 1, 1)
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`);

INSERT INTO `sys_department` (`id`, `tenant_id`, `dept_name`, `parent_id`, `sort`, `status`)
VALUES (2, 1, '技术部', 1, 1, 1)
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`);

INSERT INTO `sys_department` (`id`, `tenant_id`, `dept_name`, `parent_id`, `sort`, `status`)
VALUES (3, 1, '运营部', 1, 2, 1)
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`);
