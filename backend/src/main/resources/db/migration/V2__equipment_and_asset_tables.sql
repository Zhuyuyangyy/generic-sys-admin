-- =============================================
-- V2: Equipment and Asset Tables
-- =============================================

-- Equipment table
CREATE TABLE IF NOT EXISTS `equipment` (
  `id`                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Equipment ID',
  `equipment_code`        VARCHAR(64)      NOT NULL                COMMENT 'Equipment code, unique',
  `name`                  VARCHAR(128)     NOT NULL                COMMENT 'Equipment name',
  `category`              VARCHAR(64)      DEFAULT NULL            COMMENT 'Category',
  `model`                 VARCHAR(128)     DEFAULT NULL            COMMENT 'Model',
  `manufacturer`          VARCHAR(128)     DEFAULT NULL            COMMENT 'Manufacturer',
  `purchase_date`         DATE             DEFAULT NULL            COMMENT 'Purchase date',
  `warranty_expiry`       DATE             DEFAULT NULL            COMMENT 'Warranty expiry date',
  `status`                TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Status: 0=maintenance, 1=normal, 2=scrapped',
  `location`              VARCHAR(255)     DEFAULT NULL            COMMENT 'Location',
  `maintenance_cycle_days` INT             DEFAULT NULL            COMMENT 'Maintenance cycle in days',
  `next_maintenance_date` DATE             DEFAULT NULL            COMMENT 'Next scheduled maintenance date',
  `remarks`               TEXT             DEFAULT NULL            COMMENT 'Remarks',
  `create_user`           BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by user ID',
  `create_time`           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`            TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_code` (`equipment_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Equipment assets';

-- Asset location table
CREATE TABLE IF NOT EXISTS `asset_location` (
  `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Location ID',
  `location_code` VARCHAR(64)      NOT NULL                COMMENT 'Location code, unique',
  `location_name` VARCHAR(128)     NOT NULL                COMMENT 'Location name',
  `parent_id`     BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT 'Parent location ID, 0=root',
  `address`       VARCHAR(512)     DEFAULT NULL            COMMENT 'Address',
  `description`   VARCHAR(512)     DEFAULT NULL            COMMENT 'Description',
  `status`        TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=active',
  `create_user`   BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_location_code` (`location_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Asset locations';

-- Maintenance plan table
CREATE TABLE IF NOT EXISTS `maintenance_plan` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Plan ID',
  `equipment_id`    BIGINT UNSIGNED  NOT NULL                COMMENT 'Equipment ID',
  `plan_name`       VARCHAR(128)     NOT NULL                COMMENT 'Plan name',
  `plan_type`       VARCHAR(32)      NOT NULL DEFAULT 'PREVENTIVE' COMMENT 'Type: PREVENTIVE/CORRECTIVE/EMERGENCY',
  `description`     TEXT             DEFAULT NULL            COMMENT 'Description',
  `scheduled_date`  DATE             DEFAULT NULL            COMMENT 'Scheduled date',
  `completed_date`  DATE             DEFAULT NULL            COMMENT 'Completed date',
  `assigned_to`     BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Assigned to user ID',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Status: 0=pending, 1=in_progress, 2=completed, 3=cancelled',
  `priority`        VARCHAR(16)      NOT NULL DEFAULT 'MEDIUM' COMMENT 'Priority: LOW/MEDIUM/HIGH/CRITICAL',
  `cost`            DECIMAL(12,2)    DEFAULT NULL            COMMENT 'Estimated cost',
  `remarks`         TEXT             DEFAULT NULL            COMMENT 'Remarks',
  `create_user`     BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_status` (`status`),
  KEY `idx_scheduled_date` (`scheduled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Maintenance plans';
