-- =============================================
-- V4: Add missing columns and indexes
-- =============================================

-- Add failed login tracking columns to sys_user
ALTER TABLE `sys_user`
  ADD COLUMN IF NOT EXISTS `failed_attempts` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Failed login attempts',
  ADD COLUMN IF NOT EXISTS `locked_until` DATETIME DEFAULT NULL COMMENT 'Lockout expiration time',
  ADD COLUMN IF NOT EXISTS `password_changed_at` DATETIME DEFAULT NULL COMMENT 'Last password change time';

-- Add unique index on operation_log for performance
ALTER TABLE `sys_operation_log`
  ADD INDEX IF NOT EXISTS `idx_target_table` (`target_table`);

-- Add audit fields to sys_role
ALTER TABLE `sys_role`
  ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Soft delete: 0=active, 1=deleted',
  ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at';

-- Add status column to sys_menu if missing
ALTER TABLE `sys_menu`
  ADD COLUMN IF NOT EXISTS `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Status: 0=disabled, 1=enabled';
