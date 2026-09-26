-- ===================================================================
-- V10: Make V4 / V8 / V9 installable on MySQL
-- ===================================================================
-- 背景（真实缺陷）
--
-- V4、V8、V9 使用了 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 共 26 处，
-- 以及 `ALTER TABLE ... ADD INDEX IF NOT EXISTS` 1 处。
--
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS —— 那是 MariaDB / PostgreSQL 的语法。
-- 在 MySQL 8.0 上执行会直接报：
--
--   ERROR 1064 (42000): You have an error in your SQL syntax ... near
--   'IF NOT EXISTS `tenant_id` BIGINT UNSIGNED ...'
--
-- 后果：V1–V9 无法从一个空库完整安装，Flyway 会在第一个这样的语句上失败，
-- 整个 schema 停在半成品状态。
--
-- V4 / V8 / V9 已经发布进 master 历史（11139eb），因此不修改已发布文件，
-- 由本 migration 补齐它们应当完成的工作。
--
-- 实现方式：MySQL 没有 ADD COLUMN IF NOT EXISTS，所以用 information_schema
-- 检查 + PREPARE/EXECUTE 做条件 DDL。重复执行本文件不会报 Duplicate column。
-- ===================================================================

-- -------------------------------------------------------------------
-- 辅助：给指定表加列（仅当该列不存在）
-- 用法：CALL 形式不可用于 migration，需要为每一对 (table, column) 展开。
-- 这里直接用统一的 SQL 片段，共 26 对。
-- -------------------------------------------------------------------

-- V4: sys_user 登录失败追踪
SET @t := 'sys_user', @c := 'failed_attempts';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `failed_attempts` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''Failed login attempts''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_user', @c := 'locked_until';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `locked_until` DATETIME DEFAULT NULL COMMENT ''Lockout expiration time''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_user', @c := 'password_changed_at';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `password_changed_at` DATETIME DEFAULT NULL COMMENT ''Last password change time''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V4: sys_role 审计字段
SET @t := 'sys_role', @c := 'is_deleted';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_role` ADD COLUMN `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''Soft delete: 0=active, 1=deleted''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_role', @c := 'update_time';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_role` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''Updated at''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V4: sys_menu 状态列
SET @t := 'sys_menu', @c := 'status';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_menu` ADD COLUMN `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Status: 0=disabled, 1=enabled''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V4: sys_operation_log 索引（MySQL 无 ADD INDEX IF NOT EXISTS）
SET @t := 'sys_operation_log', @i := 'idx_target_table';
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND INDEX_NAME = @i);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_operation_log` ADD INDEX `idx_target_table` (`target_table`)',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V9: sys_user 部门与租户
SET @t := 'sys_user', @c := 'department_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''Department ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V9: 各业务表 tenant_id
SET @t := 'equipment', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `equipment` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'consumable', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `consumable` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'inventory_record', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `inventory_record` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'inventory_transaction', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `inventory_transaction` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'maintenance_plan', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `maintenance_plan` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'stock_alert', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `stock_alert` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_menu', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_menu` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_operation_log', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_operation_log` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_role', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_role` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'supplier', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `supplier` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'sys_user', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'workflow_definition', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `workflow_definition` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'workflow_instance', @c := 'tenant_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `workflow_instance` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''Tenant ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V9: department_id（equipment / consumable）
SET @t := 'equipment', @c := 'department_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `equipment` ADD COLUMN `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''Department ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'consumable', @c := 'department_id';
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = @c);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `consumable` ADD COLUMN `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''Department ID''',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- V7: equipment / consumable / stock_alert 的索引
SET @t := 'equipment', @i := 'idx_next_maintenance_date';
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND INDEX_NAME = @i);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `equipment` ADD INDEX `idx_next_maintenance_date` (`next_maintenance_date`)',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'consumable', @i := 'idx_expiration_date';
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND INDEX_NAME = @i);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `consumable` ADD INDEX `idx_expiration_date` (`expiration_date`)',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := 'stock_alert', @i := 'idx_create_time';
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND INDEX_NAME = @i);
SET @ddl := IF(@has = 0,
    'ALTER TABLE `stock_alert` ADD INDEX `idx_create_time` (`create_time`)',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
