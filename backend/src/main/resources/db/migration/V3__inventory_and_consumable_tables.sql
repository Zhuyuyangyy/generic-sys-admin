-- =============================================
-- V3: Inventory and Consumable Tables
-- =============================================

-- Consumable table
CREATE TABLE IF NOT EXISTS `consumable` (
  `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Consumable ID',
  `product_code`     VARCHAR(64)      NOT NULL                COMMENT 'Product code, unique',
  `name`             VARCHAR(128)     NOT NULL                COMMENT 'Product name',
  `category`         VARCHAR(64)      DEFAULT NULL            COMMENT 'Category',
  `unit`             VARCHAR(32)      NOT NULL DEFAULT 'piece' COMMENT 'Unit of measure',
  `stock_quantity`   INT              NOT NULL DEFAULT 0      COMMENT 'Current stock quantity',
  `min_stock_level`  INT              DEFAULT 0               COMMENT 'Minimum stock level (alert threshold)',
  `max_stock_level`  INT              DEFAULT NULL            COMMENT 'Maximum stock level',
  `unit_cost`        DECIMAL(12,2)    DEFAULT NULL            COMMENT 'Unit cost',
  `expiration_date`  DATE             DEFAULT NULL            COMMENT 'Expiration date',
  `supplier`         VARCHAR(128)     DEFAULT NULL            COMMENT 'Supplier name',
  `storage_location` VARCHAR(255)     DEFAULT NULL            COMMENT 'Storage location',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Status: 0=unavailable, 1=available',
  `reorder_point`    INT              DEFAULT NULL            COMMENT 'Reorder point',
  `last_check_date`  DATE             DEFAULT NULL            COMMENT 'Last inventory check date',
  `remarks`          TEXT             DEFAULT NULL            COMMENT 'Remarks',
  `create_user`      BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`       TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_stock_quantity` (`stock_quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Consumable inventory';

-- Inventory record (equipment maintenance/inspection records)
CREATE TABLE IF NOT EXISTS `inventory_record` (
  `id`                   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Record ID',
  `equipment_id`         BIGINT UNSIGNED  NOT NULL                COMMENT 'Equipment ID',
  `record_type`          VARCHAR(32)      NOT NULL DEFAULT 'ROUTINE' COMMENT 'Type: ROUTINE/MAINTENANCE/CALIBRATION/INSPECTION',
  `record_date`          DATE             NOT NULL                COMMENT 'Record date',
  `inspector_id`         BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Inspector user ID',
  `inspector_name`       VARCHAR(64)      DEFAULT NULL            COMMENT 'Inspector name',
  `result`               VARCHAR(32)      DEFAULT NULL            COMMENT 'Result: PASS/FAIL/CONDITIONAL',
  `status`               TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Status: 0=pending, 1=completed, 2=anomaly',
  `equipment_condition`  VARCHAR(32)      DEFAULT NULL            COMMENT 'Equipment condition: GOOD/FAIR/POOR',
  `description`          TEXT             DEFAULT NULL            COMMENT 'Description',
  `materials_used`       TEXT             DEFAULT NULL            COMMENT 'Materials used (JSON)',
  `man_hours`            DECIMAL(6,1)     DEFAULT NULL            COMMENT 'Man hours spent',
  `next_date`            DATE             DEFAULT NULL            COMMENT 'Next inspection/maintenance date',
  `signature`            VARCHAR(512)     DEFAULT NULL            COMMENT 'Digital signature URL',
  `create_user`          BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`           TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_record_type` (`record_type`),
  KEY `idx_record_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Equipment maintenance and inspection records';

-- Inventory transaction log
CREATE TABLE IF NOT EXISTS `inventory_transaction` (
  `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Transaction ID',
  `consumable_id`    BIGINT UNSIGNED  NOT NULL                COMMENT 'Consumable ID',
  `transaction_type` VARCHAR(32)      NOT NULL                COMMENT 'Type: INBOUND/OUTBOUND/ADJUSTMENT',
  `quantity`         INT              NOT NULL                COMMENT 'Quantity change (positive=in, negative=out)',
  `balance_after`    INT              NOT NULL                COMMENT 'Balance after transaction',
  `reference_no`     VARCHAR(64)      DEFAULT NULL            COMMENT 'Reference number (PO, ticket, etc.)',
  `operator_id`      BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Operator user ID',
  `remarks`          VARCHAR(512)     DEFAULT NULL            COMMENT 'Remarks',
  `create_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (`id`),
  KEY `idx_consumable_id` (`consumable_id`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inventory transaction log';

-- Supplier table
CREATE TABLE IF NOT EXISTS `supplier` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Supplier ID',
  `supplier_code`  VARCHAR(64)      NOT NULL                COMMENT 'Supplier code, unique',
  `supplier_name`  VARCHAR(128)     NOT NULL                COMMENT 'Supplier name',
  `contact_person` VARCHAR(64)      DEFAULT NULL            COMMENT 'Contact person',
  `contact_phone`  VARCHAR(32)      DEFAULT NULL            COMMENT 'Contact phone',
  `email`          VARCHAR(128)     DEFAULT NULL            COMMENT 'Email',
  `address`        VARCHAR(512)     DEFAULT NULL            COMMENT 'Address',
  `description`    TEXT             DEFAULT NULL            COMMENT 'Description',
  `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1      COMMENT 'Status: 0=inactive, 1=active',
  `create_user`    BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Created by',
  `create_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Suppliers';

-- Stock alert table
CREATE TABLE IF NOT EXISTS `stock_alert` (
  `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Alert ID',
  `consumable_id`    BIGINT UNSIGNED  NOT NULL                COMMENT 'Consumable ID',
  `alert_type`       VARCHAR(32)      NOT NULL                COMMENT 'Type: LOW_STOCK/EXPIRING/OVERSTOCK',
  `message`          VARCHAR(512)     NOT NULL                COMMENT 'Alert message',
  `threshold`        INT              DEFAULT NULL            COMMENT 'Threshold value',
  `current_value`    INT              DEFAULT NULL            COMMENT 'Current value',
  `acknowledged`     TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Acknowledged: 0=no, 1=yes',
  `acknowledged_by`  BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Acknowledged by user ID',
  `acknowledged_at`  DATETIME         DEFAULT NULL            COMMENT 'Acknowledged at',
  `create_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `is_deleted`       TINYINT UNSIGNED NOT NULL DEFAULT 0      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  KEY `idx_consumable_id` (`consumable_id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_acknowledged` (`acknowledged`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stock alerts';
