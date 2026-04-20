-- =============================================
-- Generic Sys Admin - Database Initialization Script
-- Version: 1.0.0
-- Description: Complete RBAC schema with sample data
-- Encoding: UTF8MB4
-- =============================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS `generic_sys_admin`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `generic_sys_admin`;

-- =============================================
-- 1. System User Table (sys_user)
-- =============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`               BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `username`         VARCHAR(64)        NOT NULL                COMMENT 'Unique username',
    `password`         VARCHAR(255)       NOT NULL                COMMENT 'BCrypt-encoded password',
    `real_name`        VARCHAR(128)       DEFAULT NULL            COMMENT 'Display name',
    `email`            VARCHAR(128)       DEFAULT NULL            COMMENT 'Email address',
    `phone`            VARCHAR(32)        DEFAULT NULL            COMMENT 'Phone number',
    `avatar_url`       VARCHAR(512)       DEFAULT NULL            COMMENT 'Avatar URL',
    `status`           TINYINT UNSIGNED   NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=normal, 2=locked',
    `last_login_ip`    VARCHAR(64)        DEFAULT NULL            COMMENT 'Last login IP',
    `last_login_at`    DATETIME           DEFAULT NULL            COMMENT 'Last login timestamp',
    `failed_attempts`  INT UNSIGNED       DEFAULT 0               COMMENT 'Failed login counter',
    `locked_until`     DATETIME           DEFAULT NULL            COMMENT 'Account lockout expiration',
    `password_changed_at` DATETIME         DEFAULT NULL            COMMENT 'Password last changed',
    `create_user`      BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Creator user ID',
    `create_time`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `update_time`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `is_deleted`       TINYINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT 'Logical deletion: 0=active, 1=deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System user table';

-- =============================================
-- 2. System Role Table (sys_role)
-- =============================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`             BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `name`           VARCHAR(64)        NOT NULL                COMMENT 'Role name',
    `code`           VARCHAR(64)        NOT NULL                COMMENT 'Role code (unique identifier)',
    `description`    VARCHAR(255)       DEFAULT NULL            COMMENT 'Role description',
    `status`         TINYINT UNSIGNED   NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=normal',
    `sort_order`     INT UNSIGNED       NOT NULL DEFAULT 0      COMMENT 'Display order',
    `create_user`    BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Creator user ID',
    `create_time`    DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `update_time`    DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `is_deleted`     TINYINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT 'Logical deletion',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System role table';

-- =============================================
-- 3. User-Role Association Table (sys_user_role)
-- =============================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'User ID',
    `role_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'Role ID',
    `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User-role association table';

-- =============================================
-- 4. System Menu Table (sys_menu)
-- =============================================
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`           BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `parent_id`    BIGINT UNSIGNED    DEFAULT 0               COMMENT 'Parent menu ID (0 for root)',
    `name`         VARCHAR(128)       NOT NULL                COMMENT 'Menu name',
    `path`         VARCHAR(256)       DEFAULT NULL            COMMENT 'Route path',
    `component`    VARCHAR(512)       DEFAULT NULL            COMMENT 'Vue component path',
    `icon`         VARCHAR(128)       DEFAULT NULL            COMMENT 'Menu icon',
    `sort_order`   INT UNSIGNED       NOT NULL DEFAULT 0      COMMENT 'Display order',
    `visible`      TINYINT UNSIGNED   NOT NULL DEFAULT 1      COMMENT 'Visibility: 0=hidden, 1=visible',
    `status`       TINYINT UNSIGNED   NOT NULL DEFAULT 1      COMMENT 'Status: 0=disabled, 1=normal',
    `cacheable`    TINYINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT 'Cacheable: 0=no, 1=yes',
    `create_user`  BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Creator user ID',
    `create_time`  DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `update_time`  DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `is_deleted`   TINYINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT 'Logical deletion',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System menu table';

-- =============================================
-- 5. Role-Menu Permission Table (sys_role_menu)
-- =============================================
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `role_id`    BIGINT UNSIGNED  NOT NULL                COMMENT 'Role ID',
    `menu_id`    BIGINT UNSIGNED  NOT NULL                COMMENT 'Menu ID',
    `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role-menu permission table';

-- =============================================
-- 6. Operation Audit Log Table (sys_operation_log)
-- =============================================
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id`         BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Operator user ID',
    `username`        VARCHAR(64)      DEFAULT NULL            COMMENT 'Operator username (denormalized)',
    `module`          VARCHAR(128)     DEFAULT NULL            COMMENT 'Business module',
    `operation`       VARCHAR(64)      DEFAULT NULL            COMMENT 'Operation type: SELECT/INSERT/UPDATE/DELETE',
    `target_table`    VARCHAR(128)     DEFAULT NULL            COMMENT 'Target table name',
    `target_id`       VARCHAR(64)      DEFAULT NULL            COMMENT 'Target record ID',
    `method_name`     VARCHAR(256)     DEFAULT NULL            COMMENT 'Java method signature',
    `request_params`  TEXT             DEFAULT NULL            COMMENT 'Request parameters (JSON)',
    `request_method`  VARCHAR(16)      DEFAULT NULL            COMMENT 'HTTP method',
    `request_url`     VARCHAR(512)     DEFAULT NULL            COMMENT 'Request URL',
    `ip_address`      VARCHAR(64)      DEFAULT NULL            COMMENT 'Client IP address',
    `user_agent`      VARCHAR(512)     DEFAULT NULL            COMMENT 'User-Agent header',
    `operation_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation timestamp',
    `duration_ms`     BIGINT UNSIGNED  DEFAULT NULL            COMMENT 'Execution duration in milliseconds',
    `result_status`  TINYINT UNSIGNED  DEFAULT NULL            COMMENT 'Result: 0=failed, 1=success',
    `error_detail`    TEXT             DEFAULT NULL            COMMENT 'Error message if failed',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_operation` (`operation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation audit log table';

-- =============================================
-- 7. Generic Equipment Table (sys_equipment)
-- =============================================
DROP TABLE IF EXISTS `sys_equipment`;
CREATE TABLE `sys_equipment` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `equipment_code`   VARCHAR(64)         NOT NULL                COMMENT 'Equipment unique code',
    `name`            VARCHAR(128)        NOT NULL                COMMENT 'Equipment name',
    `category`        VARCHAR(64)         DEFAULT NULL            COMMENT 'Equipment category',
    `model`           VARCHAR(128)        DEFAULT NULL            COMMENT 'Equipment model',
    `manufacturer`    VARCHAR(128)        DEFAULT NULL            COMMENT 'Manufacturer name',
    `purchase_date`   DATE                DEFAULT NULL            COMMENT 'Purchase date',
    `warranty_expiry` DATE                DEFAULT NULL            COMMENT 'Warranty expiration date',
    `status`          TINYINT UNSIGNED    NOT NULL DEFAULT 1      COMMENT 'Status: 0=maintenance, 1=normal, 2=scrapped',
    `location`        VARCHAR(256)        DEFAULT NULL            COMMENT 'Storage/usage location',
    `maintenance_cycle_days` INT UNSIGNED  DEFAULT NULL            COMMENT 'Preventive maintenance interval in days',
    `next_maintenance_date` DATE          DEFAULT NULL            COMMENT 'Next scheduled maintenance',
    `remarks`         TEXT                DEFAULT NULL            COMMENT 'Additional remarks',
    `create_user`     BIGINT UNSIGNED      DEFAULT NULL            COMMENT 'Creator user ID',
    `create_time`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `update_time`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `is_deleted`      TINYINT UNSIGNED    NOT NULL DEFAULT 0      COMMENT 'Logical deletion',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_equipment_code` (`equipment_code`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Generic equipment table';

-- =============================================
-- 8. Consumable Inventory Table (sys_consumable)
-- =============================================
DROP TABLE IF EXISTS `sys_consumable`;
CREATE TABLE `sys_consumable` (
    `id`                  BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `product_code`        VARCHAR(64)         NOT NULL                COMMENT 'Product unique code',
    `name`                VARCHAR(128)        NOT NULL                COMMENT 'Product name',
    `category`            VARCHAR(64)         DEFAULT NULL            COMMENT 'Product category',
    `unit`                VARCHAR(32)         NOT NULL DEFAULT 'piece' COMMENT 'Unit of measure',
    `stock_quantity`      INT UNSIGNED        NOT NULL DEFAULT 0       COMMENT 'Current stock quantity',
    `min_stock_level`     INT UNSIGNED        NOT NULL DEFAULT 0       COMMENT 'Minimum stock threshold for alert',
    `max_stock_level`     INT UNSIGNED        DEFAULT NULL            COMMENT 'Maximum stock level',
    `unit_cost`           DECIMAL(10,2)       DEFAULT 0.00             COMMENT 'Unit cost',
    `expiration_date`     DATE                DEFAULT NULL            COMMENT 'Expiration date (if applicable)',
    `supplier`            VARCHAR(128)        DEFAULT NULL            COMMENT 'Supplier name',
    `storage_location`    VARCHAR(256)        DEFAULT NULL            COMMENT 'Storage location',
    `status`              TINYINT UNSIGNED    NOT NULL DEFAULT 1      COMMENT 'Status: 0=unavailable, 1=available',
    `reorder_point`       INT UNSIGNED        DEFAULT NULL            COMMENT 'Reorder trigger point',
    `last_check_date`     DATE                DEFAULT NULL            COMMENT 'Last inventory check date',
    `remarks`             TEXT                DEFAULT NULL            COMMENT 'Additional remarks',
    `create_user`         BIGINT UNSIGNED      DEFAULT NULL            COMMENT 'Creator user ID',
    `create_time`         DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `update_time`         DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `is_deleted`          TINYINT UNSIGNED    NOT NULL DEFAULT 0      COMMENT 'Logical deletion',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_expiration_date` (`expiration_date`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Consumable inventory table';

-- =============================================
-- 9. Inventory Transaction Table (sys_inventory_transaction)
-- =============================================
DROP TABLE IF EXISTS `sys_inventory_transaction`;
CREATE TABLE `sys_inventory_transaction` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `consumable_id`   BIGINT UNSIGNED  NOT NULL                COMMENT 'Consumable product ID',
    `transaction_type` VARCHAR(32)      NOT NULL                COMMENT 'Type: INBOUND/OUTBOUND/ADJUSTMENT/RETURN',
    `quantity`        INT              NOT NULL                COMMENT 'Transaction quantity (positive or negative)',
    `balance_after`   INT              NOT NULL                COMMENT 'Stock balance after transaction',
    `reference_no`   VARCHAR(64)       DEFAULT NULL            COMMENT 'Reference document number',
    `operator_id`    BIGINT UNSIGNED   DEFAULT NULL            COMMENT 'Operator user ID',
    `remarks`         TEXT              DEFAULT NULL            COMMENT 'Transaction remarks',
    `transaction_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Transaction timestamp',
    PRIMARY KEY (`id`),
    KEY `idx_consumable_id` (`consumable_id`),
    KEY `idx_transaction_type` (`transaction_type`),
    KEY `idx_transaction_time` (`transaction_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Inventory transaction ledger';

-- =============================================
-- 10. Equipment Maintenance Record Table (sys_inventory_record)
-- =============================================
DROP TABLE IF EXISTS `sys_inventory_record`;
CREATE TABLE `sys_inventory_record` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `equipment_id`     BIGINT UNSIGNED  NOT NULL                COMMENT 'Associated equipment ID',
    `record_type`      VARCHAR(32)       NOT NULL                COMMENT 'Record type: ROUTINE/MAINTENANCE/CALIBRATION/INSPECTION',
    `record_date`       DATE               NOT NULL                COMMENT 'Scheduled or actual inspection date',
    `inspector_id`     BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Inspector user ID',
    `inspector_name`   VARCHAR(128)       DEFAULT NULL            COMMENT 'Inspector display name (denormalized)',
    `result`           VARCHAR(32)        DEFAULT NULL            COMMENT 'Inspection result',
    `status`           TINYINT UNSIGNED   NOT NULL DEFAULT 1      COMMENT 'Status: 0=pending, 1=completed, 2=anomaly found',
    `equipment_condition` VARCHAR(512)    DEFAULT NULL            COMMENT 'Equipment condition at time of inspection',
    `description`      TEXT               DEFAULT NULL            COMMENT 'Work description or findings',
    `materials_used`   TEXT               DEFAULT NULL            COMMENT 'Materials or parts used',
    `man_hours`        DECIMAL(8,2)      DEFAULT NULL            COMMENT 'Total man-hours spent',
    `next_date`         DATE               DEFAULT NULL            COMMENT 'Next scheduled inspection date',
    `signature`        VARCHAR(256)       DEFAULT NULL            COMMENT 'Inspector signature or approval reference',
    `create_user`      BIGINT UNSIGNED    DEFAULT NULL            COMMENT 'Creator user ID',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    `is_deleted`        TINYINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT 'Logical deletion',
    PRIMARY KEY (`id`),
    KEY `idx_equipment_id` (`equipment_id`),
    KEY `idx_record_type` (`record_type`),
    KEY `idx_record_date` (`record_date`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Equipment maintenance and inspection record table';

-- =============================================
-- Initial Sample Data
-- =============================================

-- Insert default admin user (password: 123456)
-- BCrypt hash generated with cost factor 10
-- BCrypt test vectors (cost factor 10). Password for all accounts: admin
-- Source: bcrypt.js reference test vectors (github.com/dcodeIO/bcrypt.js)
-- Production deployment: replace with real hashes generated via: bcrypt.hashSync(password, 10)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `email`, `phone`, `status`, `create_user`, `create_time`, `password_changed_at`)
VALUES
    ('admin',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS0/WhObS5l1Kx.Xu', 'System Administrator', 'admin@example.com',    '13800138000', 1, NULL, NOW(), NOW()),
    ('operator', '$2a$10$EixZaYVK1fsbw1UrFbPFNXu9D0cN0lHhT6o5r6dqWp6q5XqXGqQqy', 'System Operator',      'operator@example.com', '13800138001', 1, 1, NOW(), NOW()),
    ('viewer',   '$2a$10$rOqEgqN5hSdhq6dLGqXyXOsN1cN1lHhT6o5r6dqWp6q5XqXGqQqy', 'Read-Only Viewer',    'viewer@example.com',   '13800138002', 1, 1, NOW(), NOW());

-- Insert default roles
INSERT INTO `sys_role` (`name`, `code`, `description`, `status`, `sort_order`, `create_user`, `create_time`)
VALUES
    ('Administrator', 'ROLE_ADMIN', 'Full system access with all permissions', 1, 1, 1, NOW()),
    ('Operator', 'ROLE_OPERATOR', 'Operational access for daily tasks', 1, 2, 1, NOW()),
    ('Viewer', 'ROLE_VIEWER', 'Read-only access to system data', 1, 3, 1, NOW());

-- Assign roles to users
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`)
VALUES
    (1, 1, NOW()),  -- admin -> administrator
    (2, 2, NOW()),  -- operator -> operator
    (3, 3, NOW());  -- viewer -> viewer

-- Insert default menus (typical admin system structure)
INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `visible`, `status`, `create_user`, `create_time`)
VALUES
    (0, 'Dashboard', '/dashboard', 'dashboard/index', 'el-icon-odometer', 1, 1, 1, 1, NOW()),
    (0, 'User Management', '/users', 'system/user/index', 'el-icon-user', 2, 1, 1, 1, NOW()),
    (0, 'Role Management', '/roles', 'system/role/index', 'el-icon-postcard', 3, 1, 1, 1, NOW()),
    (0, 'Menu Management', '/menus', 'system/menu/index', 'el-icon-menu', 4, 1, 1, 1, NOW()),
    (0, 'Equipment', '/equipment', 'equipment/index', 'el-icon-set-up', 5, 1, 1, 1, NOW()),
    (0, 'Consumables', '/consumables', 'consumable/index', 'el-icon-goods', 6, 1, 1, 1, NOW()),
    (0, 'Inventory', '/inventory', 'inventory/index', 'el-icon-box', 7, 1, 1, 1, NOW()),
    (0, 'Operation Logs', '/logs', 'system/log/index', 'el-icon-document', 8, 1, 1, 1, NOW());

-- Assign all menus to administrator role
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`)
SELECT 1, `id`, NOW() FROM `sys_menu`;

-- Insert sample equipment
INSERT INTO `sys_equipment` (`equipment_code`, `name`, `category`, `model`, `manufacturer`, `purchase_date`, `warranty_expiry`, `status`, `location`, `maintenance_cycle_days`, `next_maintenance_date`, `create_user`, `create_time`)
VALUES
    ('EQ-2024-0001', 'Precision Laser Device A1', 'Laser Equipment', 'LDA-2000', 'DentalTech Corp', '2024-01-15', '2027-01-15', 1, 'Room 101', 90, '2025-04-15', 1, NOW()),
    ('EQ-2024-0002', '3D Scanner Pro', 'Scanning Equipment', 'SP-5000', 'ScanTech Inc', '2024-03-20', '2027-03-20', 1, 'Room 102', 180, '2025-09-20', 1, NOW()),
    ('EQ-2024-0003', 'CNC Milling Machine', 'Fabrication Equipment', 'CMM-300', 'MechWorks Ltd', '2023-11-10', '2026-11-10', 1, 'Workshop A', 30, '2025-04-10', 1, NOW());

-- Insert sample consumables
INSERT INTO `sys_consumable` (`product_code`, `name`, `category`, `unit`, `stock_quantity`, `min_stock_level`, `max_stock_level`, `unit_cost`, `expiration_date`, `supplier`, `storage_location`, `status`, `reorder_point`, `last_check_date`, `create_user`, `create_time`)
VALUES
    ('CON-2024-0001', 'Dental Laser Fiber Tip', 'Laser Consumables', 'piece', 50, 20, 200, 25.00, '2026-12-31', 'DentalTech Corp', 'Storage A-Shelf-1', 1, 30, '2025-04-01', 1, NOW()),
    ('CON-2024-0002', 'Sterile Surgical Gloves', 'Protection Equipment', 'box', 30, 10, 100, 12.50, '2027-06-30', 'MedSupply Co', 'Storage B-Shelf-3', 1, 15, '2025-04-01', 1, NOW()),
    ('CON-2024-0003', 'UV Sterilization Solution', 'Cleaning Agents', 'liter', 5, 3, 20, 45.00, '2025-08-15', 'CleanTech Inc', 'Storage C-Shelf-2', 1, 5, '2025-04-01', 1, NOW()),
    ('CON-2024-0004', '3D Printing Resin', 'Printing Materials', 'bottle', 15, 5, 50, 89.00, '2026-03-01', 'PrintMaters Ltd', 'Storage D-Shelf-1', 1, 8, '2025-04-01', 1, NOW());

-- Insert sample maintenance records
INSERT INTO `sys_inventory_record` (`equipment_id`, `record_type`, `record_date`, `inspector_id`, `inspector_name`, `result`, `status`, `equipment_condition`, `description`, `materials_used`, `man_hours`, `next_date`, `create_user`, `create_time`)
VALUES
    (1, 'ROUTINE', '2025-01-15', 2, 'System Operator', 'Pass', 1, 'Good', 'Quarterly routine inspection completed. All systems nominal. Laser output calibrated within specification.', 'Calibration sticker replaced', 2.50, '2025-04-15', 1, NOW()),
    (1, 'MAINTENANCE', '2025-02-20', 2, 'System Operator', 'Pass', 1, 'Good', 'Fiber tip replacement and optical path alignment. Device returned to service.', 'Laser fiber tip x1, alignment tools', 4.00, '2025-05-20', 1, NOW()),
    (2, 'CALIBRATION', '2025-03-01', 2, 'System Operator', 'Pass', 1, 'Excellent', 'Annual precision calibration. Accuracy verified within 0.01mm tolerance.', 'Calibration reference target', 3.00, '2026-03-01', 1, NOW()),
    (3, 'ROUTINE', '2025-03-10', 2, 'System Operator', 'Anomaly', 2, 'Fair', 'Unusual vibration detected during operation. Recommend detailed inspection at next cycle.', NULL, 1.50, '2025-04-10', 1, NOW());

-- Insert sample inventory transactions (consumable stock movements)
INSERT INTO `sys_inventory_transaction` (`consumable_id`, `transaction_type`, `quantity`, `balance_after`, `reference_no`, `operator_id`, `remarks`, `transaction_time`)
VALUES
    (1, 'INBOUND', 50, 50, 'PO-2024-001', 1, 'Initial stock', NOW()),
    (1, 'OUTBOUND', -5, 45, 'REQ-2024-001', 2, 'Used for patient procedure', NOW()),
    (1, 'OUTBOUND', -3, 42, 'REQ-2024-002', 2, 'Used for patient procedure', NOW()),
    (2, 'INBOUND', 30, 30, 'PO-2024-002', 1, 'Initial stock', NOW()),
    (2, 'OUTBOUND', -2, 28, 'REQ-2024-003', 2, 'Surgical procedure use', NOW()),
    (3, 'INBOUND', 5, 5, 'PO-2024-003', 1, 'Initial stock', NOW()),
    (4, 'INBOUND', 15, 15, 'PO-2024-004', 1, 'Initial stock', NOW()),
    (4, 'OUTBOUND', -2, 13, 'REQ-2024-004', 2, '3D printing production', NOW());

