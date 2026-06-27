-- =============================================
-- V7: Business Closed Loop Tables
-- Equipment inspection, assignment, and consumable batch management
-- =============================================

-- Equipment inspection records
CREATE TABLE IF NOT EXISTS `asset_inspection` (
  `id`                   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Inspection ID',
  `equipment_id`         BIGINT UNSIGNED  NOT NULL                COMMENT 'Equipment ID',
  `inspection_type`      VARCHAR(32)      NOT NULL DEFAULT 'ROUTINE' COMMENT 'Type: ROUTINE/SPECIAL/FOLLOW_UP',
  `inspection_date`      DATE             NOT NULL                COMMENT 'Inspection date',
  `inspector_id`         BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Inspector user ID',
  `inspector_name`       VARCHAR(64)      DEFAULT NULL            COMMENT 'Inspector name',
  `result`               VARCHAR(32)      NOT NULL DEFAULT 'PASS' COMMENT 'Result: PASS/FAIL/CONDITIONAL',
  `findings`             TEXT             DEFAULT NULL            COMMENT 'Inspection findings',
  `corrective_action`    TEXT             DEFAULT NULL            COMMENT 'Corrective action taken or recommended',
  `next_inspection_date` DATE             DEFAULT NULL            COMMENT 'Next scheduled inspection date',
  `create_user`          BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`           TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_inspection_type` (`inspection_type`),
  KEY `idx_inspection_date` (`inspection_date`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Equipment inspection records';

-- Equipment assignments
CREATE TABLE IF NOT EXISTS `asset_assignment` (
  `id`                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Assignment ID',
  `equipment_id`        BIGINT UNSIGNED  NOT NULL                COMMENT 'Equipment ID',
  `assigned_to_user_id` BIGINT UNSIGNED  NOT NULL                COMMENT 'Assigned to user ID',
  `assigned_to_user_name` VARCHAR(64)    DEFAULT NULL            COMMENT 'Assigned to user name',
  `assigned_by_user_id` BIGINT UNSIGNED  NOT NULL                COMMENT 'Assigned by user ID',
  `assigned_date`       DATE             NOT NULL                COMMENT 'Assignment date',
  `returned_date`       DATE             DEFAULT NULL            COMMENT 'Return date',
  `assignment_type`     VARCHAR(32)      NOT NULL DEFAULT 'BORROW' COMMENT 'Type: BORROW/PERMANENT',
  `status`              VARCHAR(32)      NOT NULL DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE/RETURNED',
  `remarks`             TEXT             DEFAULT NULL            COMMENT 'Remarks',
  `create_user`         BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`          TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_assigned_to_user_id` (`assigned_to_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_assigned_date` (`assigned_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Equipment assignments';

-- Consumable batches
CREATE TABLE IF NOT EXISTS `consumable_batch` (
  `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Batch ID',
  `consumable_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'Consumable ID',
  `batch_no`          VARCHAR(64)      NOT NULL                COMMENT 'Batch number',
  `supplier_id`       BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Supplier ID',
  `quantity`          INT              NOT NULL DEFAULT 0       COMMENT 'Total quantity received',
  `remaining_quantity` INT             NOT NULL DEFAULT 0       COMMENT 'Remaining quantity',
  `unit_cost`         DECIMAL(12,2)    DEFAULT NULL            COMMENT 'Unit cost',
  `total_cost`        DECIMAL(12,2)    DEFAULT NULL            COMMENT 'Total cost (quantity * unit_cost)',
  `production_date`   DATE             DEFAULT NULL            COMMENT 'Production date',
  `expiration_date`   DATE             DEFAULT NULL            COMMENT 'Expiration date',
  `status`            VARCHAR(32)      NOT NULL DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE/EXPIRED/DEPLETED',
  `storage_location`  VARCHAR(255)     DEFAULT NULL            COMMENT 'Storage location',
  `create_user`       BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`        TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete: 0=active, 1=deleted',
  PRIMARY KEY (`id`),
  KEY `idx_consumable_id` (`consumable_id`),
  KEY `idx_batch_no` (`batch_no`),
  KEY `idx_status` (`status`),
  KEY `idx_expiration_date` (`expiration_date`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Consumable batches';

-- Add missing indexes for existing tables
ALTER TABLE `equipment` ADD INDEX IF NOT EXISTS `idx_next_maintenance_date` (`next_maintenance_date`);
ALTER TABLE `consumable` ADD INDEX IF NOT EXISTS `idx_expiration_date` (`expiration_date`);
ALTER TABLE `stock_alert` ADD INDEX IF NOT EXISTS `idx_create_time` (`create_time`);
