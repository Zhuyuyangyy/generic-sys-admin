# ERMS Database Schema

## Overview

- **Database**: `generic_sys_admin`
- **Charset**: UTF8MB4 (`utf8mb4_unicode_ci`)
- **Engine**: InnoDB
- **Migration**: Flyway (V1–V5)

## ER Diagram Description

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   sys_user   │────────→│ sys_user_role │←────────│   sys_role   │
│              │  1:N    │              │   N:1   │              │
└──────┬───────┘         └──────────────┘         └──────┬───────┘
       │                                                 │
       │ (audit)                                  ┌──────┴───────┐
       │                                          │ sys_role_menu │
       ▼                                          └──────┬───────┘
┌──────────────────┐                                      │
│ sys_operation_log │                                      ▼
│                  │                               ┌──────────────┐
└──────────────────┘                               │   sys_menu    │
                                                   └──────────────┘

┌─────────────┐         ┌──────────────────────┐
│  equipment   │────────→│   inventory_record    │
│              │  1:N    │                      │
└──────┬───────┘         └──────────────────────┘
       │
       │ (location)
       ▼
┌──────────────┐
│ asset_location│
└──────────────┘

┌─────────────┐         ┌──────────────────────┐         ┌──────────────┐
│  consumable   │────────→│ inventory_transaction │         │   supplier   │
│              │  1:N    │                      │         │              │
└──────┬───────┘         └──────────────────────┘         └──────────────┘
       │
       │ (alerts)
       ▼
┌──────────────┐
│  stock_alert  │
└──────────────┘

┌──────────────────────┐         ┌──────────────────────┐
│ workflow_definition   │────────→│  workflow_instance    │
│                      │  1:N    │                      │
└──────────────────────┘         └──────┬───────────────┘
                                        │
                                 ┌──────┴───────────────┐
                                 │   workflow_task       │
                                 └──────────────────────┘
```

---

## Table Definitions

### 1. sys_user — System Users

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `username` | VARCHAR(64) | NO | — | Unique username |
| `password` | VARCHAR(255) | NO | — | BCrypt hashed password |
| `real_name` | VARCHAR(128) | YES | NULL | Real name |
| `email` | VARCHAR(128) | YES | NULL | Email address |
| `phone` | VARCHAR(32) | YES | NULL | Phone number |
| `avatar_url` | VARCHAR(512) | YES | NULL | Avatar URL |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=disabled, 1=normal, 2=locked |
| `failed_attempts` | INT UNSIGNED | NO | 0 | Failed login attempts (V4) |
| `locked_until` | DATETIME | YES | NULL | Lockout expiration (V4) |
| `password_changed_at` | DATETIME | YES | NULL | Last password change (V4) |
| `last_login_ip` | VARCHAR(64) | YES | NULL | Last login IP |
| `last_login_at` | DATETIME | YES | NULL | Last login timestamp |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by user ID |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete: 0=active, 1=deleted |

**Indexes:**
- `uk_username` UNIQUE on `username`
- `idx_status` on `status`
- `idx_create_time` on `create_time`

**Initial Data:**
- `admin` (id=1) — System administrator, BCrypt hashed password for `123456`
- `user` (id=2) — Test user, same password

---

### 2. sys_role — System Roles

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `name` | VARCHAR(64) | NO | — | Role display name |
| `code` | VARCHAR(64) | NO | — | Unique role code |
| `description` | VARCHAR(255) | YES | NULL | Role description |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=disabled, 1=normal |
| `sort_order` | INT UNSIGNED | NO | 0 | Display order |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete (V4) |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by user ID |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |

**Indexes:**
- `uk_code` UNIQUE on `code`
- `idx_status` on `status`

**Initial Data:**
- `SUPER_ADMIN` (id=1) — Full system permissions
- `USER` (id=2) — Normal user permissions

---

### 3. sys_user_role — User-Role Mapping

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `user_id` | BIGINT UNSIGNED | NO | — | User ID (FK → sys_user) |
| `role_id` | BIGINT UNSIGNED | NO | — | Role ID (FK → sys_role) |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |

**Indexes:**
- `uk_user_role` UNIQUE on `(user_id, role_id)`
- `idx_role_id` on `role_id`

**Initial Data:**
- user_id=1 → role_id=1 (admin is SUPER_ADMIN)
- user_id=2 → role_id=2 (user is USER)

---

### 4. sys_menu — Menu & Permission Resources

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `parent_id` | BIGINT UNSIGNED | NO | 0 | Parent menu ID (0=root) |
| `name` | VARCHAR(64) | NO | — | Menu name |
| `path` | VARCHAR(255) | YES | NULL | Route path |
| `component` | VARCHAR(255) | YES | NULL | Vue component path |
| `icon` | VARCHAR(64) | YES | NULL | Icon name |
| `sort_order` | INT UNSIGNED | NO | 0 | Display order |
| `visible` | TINYINT UNSIGNED | NO | 1 | 0=hidden, 1=visible |
| `is_external` | TINYINT UNSIGNED | NO | 0 | 0=internal, 1=external link |
| `permission` | VARCHAR(128) | YES | NULL | Permission identifier |
| `menu_type` | TINYINT UNSIGNED | NO | 1 | 1=directory, 2=menu, 3=button |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=disabled, 1=enabled (V4) |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |

**Indexes:**
- `idx_parent_id` on `parent_id`
- `idx_permission` on `permission`

**Initial Data (7 menus):**

| Name | Path | Type | Permission |
|------|------|------|-----------|
| 系统管理 | /system | Directory | — |
| 用户管理 | /system/user | Menu | `system:user:query` |
| 角色管理 | /system/role | Menu | `system:role:query` |
| 菜单管理 | /system/menu | Menu | `system:menu:query` |
| 操作日志 | /system/log | Menu | `system:log:query` |
| 数据大屏 | /dashboard | Menu | `dashboard:view` |
| 语音播报 | /voice | Menu | `voice:use` |
| 模板管理 | /template | Menu | `template:query` |

---

### 5. sys_role_menu — Role-Menu Mapping

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `role_id` | BIGINT UNSIGNED | NO | — | Role ID (FK → sys_role) |
| `menu_id` | BIGINT UNSIGNED | NO | — | Menu ID (FK → sys_menu) |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |

**Indexes:**
- `uk_role_menu` UNIQUE on `(role_id, menu_id)`
- `idx_menu_id` on `menu_id`

**Initial Data:**
- SUPER_ADMIN → all menus
- USER → dashboard menu only

---

### 6. sys_operation_log — Operation Audit Log

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `user_id` | BIGINT UNSIGNED | YES | NULL | Operator user ID |
| `username` | VARCHAR(64) | YES | NULL | Operator username (denormalized) |
| `module` | VARCHAR(128) | YES | NULL | Operation module |
| `operation` | VARCHAR(64) | YES | NULL | INSERT/DELETE/UPDATE/SELECT/LOGIN |
| `target_table` | VARCHAR(128) | YES | NULL | Target table name |
| `target_id` | VARCHAR(64) | YES | NULL | Target record ID |
| `method_name` | VARCHAR(256) | YES | NULL | Full Java method name |
| `request_params` | TEXT | YES | NULL | Request parameters (JSON) |
| `request_method` | VARCHAR(16) | YES | NULL | HTTP method |
| `request_url` | VARCHAR(512) | YES | NULL | Request URL |
| `ip_address` | VARCHAR(64) | YES | NULL | Client IP |
| `user_agent` | VARCHAR(512) | YES | NULL | User-Agent header |
| `operation_time` | DATETIME | NO | CURRENT_TIMESTAMP | Operation timestamp |
| `duration_ms` | BIGINT UNSIGNED | YES | NULL | Execution time (ms) |
| `result_status` | TINYINT UNSIGNED | YES | NULL | 0=failed, 1=success |
| `error_detail` | TEXT | YES | NULL | Error message on failure |

**Indexes:**
- `idx_user_id` on `user_id`
- `idx_module` on `module`
- `idx_operation_time` on `operation_time`
- `idx_operation` on `operation`
- `idx_target_table` on `target_table` (V4)

---

### 7. equipment — Equipment Assets

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `equipment_code` | VARCHAR(64) | NO | — | Unique equipment code |
| `name` | VARCHAR(128) | NO | — | Equipment name |
| `category` | VARCHAR(64) | YES | NULL | Category |
| `model` | VARCHAR(128) | YES | NULL | Model |
| `manufacturer` | VARCHAR(128) | YES | NULL | Manufacturer |
| `purchase_date` | DATE | YES | NULL | Purchase date |
| `warranty_expiry` | DATE | YES | NULL | Warranty expiry |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=maintenance, 1=normal, 2=scrapped |
| `location` | VARCHAR(255) | YES | NULL | Location description |
| `maintenance_cycle_days` | INT | YES | NULL | Maintenance cycle (days) |
| `next_maintenance_date` | DATE | YES | NULL | Next scheduled maintenance |
| `remarks` | TEXT | YES | NULL | Remarks |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by user ID |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `uk_equipment_code` UNIQUE on `equipment_code`
- `idx_category` on `category`
- `idx_status` on `status`
- `idx_create_time` on `create_time`

---

### 8. asset_location — Asset Locations

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `location_code` | VARCHAR(64) | NO | — | Unique location code |
| `location_name` | VARCHAR(128) | NO | — | Location name |
| `parent_id` | BIGINT UNSIGNED | NO | 0 | Parent location (0=root) |
| `address` | VARCHAR(512) | YES | NULL | Physical address |
| `description` | VARCHAR(512) | YES | NULL | Description |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=disabled, 1=active |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `uk_location_code` UNIQUE on `location_code`
- `idx_parent_id` on `parent_id`

---

### 9. maintenance_plan — Maintenance Plans

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `equipment_id` | BIGINT UNSIGNED | NO | — | Equipment ID (FK → equipment) |
| `plan_name` | VARCHAR(128) | NO | — | Plan name |
| `plan_type` | VARCHAR(32) | NO | 'PREVENTIVE' | PREVENTIVE/CORRECTIVE/EMERGENCY |
| `description` | TEXT | YES | NULL | Description |
| `scheduled_date` | DATE | YES | NULL | Scheduled date |
| `completed_date` | DATE | YES | NULL | Completed date |
| `assigned_to` | BIGINT UNSIGNED | YES | NULL | Assigned to user ID |
| `status` | TINYINT UNSIGNED | NO | 0 | 0=pending, 1=in_progress, 2=completed, 3=cancelled |
| `priority` | VARCHAR(16) | NO | 'MEDIUM' | LOW/MEDIUM/HIGH/CRITICAL |
| `cost` | DECIMAL(12,2) | YES | NULL | Estimated cost |
| `remarks` | TEXT | YES | NULL | Remarks |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `idx_equipment_id` on `equipment_id`
- `idx_status` on `status`
- `idx_scheduled_date` on `scheduled_date`

---

### 10. consumable — Consumable Inventory

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `product_code` | VARCHAR(64) | NO | — | Unique product code |
| `name` | VARCHAR(128) | NO | — | Product name |
| `category` | VARCHAR(64) | YES | NULL | Category |
| `unit` | VARCHAR(32) | NO | 'piece' | Unit of measure |
| `stock_quantity` | INT | NO | 0 | Current stock |
| `min_stock_level` | INT | YES | 0 | Minimum stock (alert threshold) |
| `max_stock_level` | INT | YES | NULL | Maximum stock |
| `unit_cost` | DECIMAL(12,2) | YES | NULL | Unit cost |
| `expiration_date` | DATE | YES | NULL | Expiration date |
| `supplier` | VARCHAR(128) | YES | NULL | Supplier name |
| `storage_location` | VARCHAR(255) | YES | NULL | Storage location |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=unavailable, 1=available |
| `reorder_point` | INT | YES | NULL | Reorder point |
| `last_check_date` | DATE | YES | NULL | Last inventory check date |
| `remarks` | TEXT | YES | NULL | Remarks |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `uk_product_code` UNIQUE on `product_code`
- `idx_category` on `category`
- `idx_status` on `status`
- `idx_stock_quantity` on `stock_quantity`

---

### 11. inventory_record — Equipment Maintenance/Inspection Records

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `equipment_id` | BIGINT UNSIGNED | NO | — | Equipment ID (FK → equipment) |
| `record_type` | VARCHAR(32) | NO | 'ROUTINE' | ROUTINE/MAINTENANCE/CALIBRATION/INSPECTION |
| `record_date` | DATE | NO | — | Record date |
| `inspector_id` | BIGINT UNSIGNED | YES | NULL | Inspector user ID |
| `inspector_name` | VARCHAR(64) | YES | NULL | Inspector name |
| `result` | VARCHAR(32) | YES | NULL | PASS/FAIL/CONDITIONAL |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=pending, 1=completed, 2=anomaly |
| `equipment_condition` | VARCHAR(32) | YES | NULL | GOOD/FAIR/POOR |
| `description` | TEXT | YES | NULL | Description |
| `materials_used` | TEXT | YES | NULL | Materials used (JSON) |
| `man_hours` | DECIMAL(6,1) | YES | NULL | Man hours spent |
| `next_date` | DATE | YES | NULL | Next inspection date |
| `signature` | VARCHAR(512) | YES | NULL | Digital signature URL |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `idx_equipment_id` on `equipment_id`
- `idx_record_type` on `record_type`
- `idx_record_date` on `record_date`

---

### 12. inventory_transaction — Inventory Transaction Log

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `consumable_id` | BIGINT UNSIGNED | NO | — | Consumable ID (FK → consumable) |
| `transaction_type` | VARCHAR(32) | NO | — | INBOUND/OUTBOUND/ADJUSTMENT |
| `quantity` | INT | NO | — | Quantity change (+/-) |
| `balance_after` | INT | NO | — | Balance after transaction |
| `reference_no` | VARCHAR(64) | YES | NULL | Reference number (PO, ticket) |
| `operator_id` | BIGINT UNSIGNED | YES | NULL | Operator user ID |
| `remarks` | VARCHAR(512) | YES | NULL | Remarks |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |

**Indexes:**
- `idx_consumable_id` on `consumable_id`
- `idx_transaction_type` on `transaction_type`
- `idx_create_time` on `create_time`

---

### 13. supplier — Suppliers

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `supplier_code` | VARCHAR(64) | NO | — | Unique supplier code |
| `supplier_name` | VARCHAR(128) | NO | — | Supplier name |
| `contact_person` | VARCHAR(64) | YES | NULL | Contact person |
| `contact_phone` | VARCHAR(32) | YES | NULL | Contact phone |
| `email` | VARCHAR(128) | YES | NULL | Email |
| `address` | VARCHAR(512) | YES | NULL | Address |
| `description` | TEXT | YES | NULL | Description |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=inactive, 1=active |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `uk_supplier_code` UNIQUE on `supplier_code`
- `idx_status` on `status`

---

### 14. stock_alert — Stock Alerts

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `consumable_id` | BIGINT UNSIGNED | NO | — | Consumable ID (FK → consumable) |
| `alert_type` | VARCHAR(32) | NO | — | LOW_STOCK/EXPIRING/OVERSTOCK |
| `message` | VARCHAR(512) | NO | — | Alert message |
| `threshold` | INT | YES | NULL | Threshold value |
| `current_value` | INT | YES | NULL | Current value |
| `acknowledged` | TINYINT UNSIGNED | NO | 0 | 0=no, 1=yes |
| `acknowledged_by` | BIGINT UNSIGNED | YES | NULL | Acknowledged by user ID |
| `acknowledged_at` | DATETIME | YES | NULL | Acknowledged timestamp |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `idx_consumable_id` on `consumable_id`
- `idx_alert_type` on `alert_type`
- `idx_acknowledged` on `acknowledged`

---

### 15. workflow_definition — Workflow Definitions

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `definition_name` | VARCHAR(128) | NO | — | Definition name |
| `definition_code` | VARCHAR(64) | NO | — | Unique definition code |
| `description` | TEXT | YES | NULL | Description |
| `workflow_type` | VARCHAR(64) | NO | — | Workflow type |
| `steps` | TEXT | YES | NULL | JSON array of approval steps |
| `status` | TINYINT UNSIGNED | NO | 1 | Status |
| `version` | INT | NO | 1 | Version number |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `uk_definition_code` UNIQUE on `definition_code`

---

### 16. workflow_instance — Workflow Instances

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `definition_id` | BIGINT UNSIGNED | NO | — | Definition ID (FK → workflow_definition) |
| `business_type` | VARCHAR(64) | NO | — | Business entity type |
| `business_id` | BIGINT UNSIGNED | NO | — | Business entity ID |
| `title` | VARCHAR(255) | NO | — | Instance title |
| `initiator_id` | BIGINT UNSIGNED | NO | — | Initiator user ID |
| `current_step` | INT | NO | 0 | Current step index |
| `total_steps` | INT | NO | 1 | Total step count |
| `status` | VARCHAR(32) | NO | 'PENDING' | PENDING/APPROVED/REJECTED/CANCELLED |
| `create_user` | BIGINT UNSIGNED | YES | NULL | Created by |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `idx_definition_id` on `definition_id`
- `idx_initiator_id` on `initiator_id`
- `idx_status` on `status`

---

### 17. workflow_task — Workflow Tasks

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `instance_id` | BIGINT UNSIGNED | NO | — | Instance ID (FK → workflow_instance) |
| `step_order` | INT | NO | — | Step order |
| `assignee_id` | BIGINT UNSIGNED | NO | — | Assignee user ID |
| `action` | VARCHAR(32) | NO | 'NONE' | NONE/APPROVE/REJECT |
| `comment` | TEXT | YES | NULL | Comment |
| `status` | VARCHAR(32) | NO | 'PENDING' | PENDING/COMPLETED |
| `action_time` | DATETIME | YES | NULL | Action timestamp |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |

**Indexes:**
- `idx_instance_id` on `instance_id`
- `idx_assignee_id` on `assignee_id`
- `idx_status` on `status`

---

### 18. template_entity — Generic Template Entity

| Column | Type | Nullable | Default | Comment |
|--------|------|----------|---------|---------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | Primary key |
| `name` | VARCHAR(128) | NO | — | Template name |
| `code` | VARCHAR(64) | NO | — | Unique code (per tenant) |
| `category` | VARCHAR(64) | YES | NULL | Category |
| `description` | VARCHAR(512) | YES | NULL | Description |
| `schema_json` | JSON | YES | NULL | Data structure schema |
| `ui_config_json` | JSON | YES | NULL | UI configuration |
| `preview_image` | VARCHAR(512) | YES | NULL | Preview image URL |
| `version` | VARCHAR(32) | NO | '1.0.0' | Version |
| `status` | TINYINT UNSIGNED | NO | 1 | 0=disabled, 1=normal, 2=draft |
| `is_public` | TINYINT UNSIGNED | NO | 0 | Public visibility |
| `tags` | VARCHAR(512) | YES | NULL | Tags |
| `create_user_id` | BIGINT UNSIGNED | YES | NULL | Created by user ID |
| `create_user_name` | VARCHAR(64) | YES | NULL | Created by user name |
| `create_time` | DATETIME | NO | CURRENT_TIMESTAMP | Created at |
| `update_time` | DATETIME | NO | CURRENT_TIMESTAMP ON UPDATE | Updated at |
| `tenant_id` | BIGINT UNSIGNED | NO | 1 | Tenant ID (multi-tenant ready) |
| `is_deleted` | TINYINT UNSIGNED | NO | 0 | Soft delete |

**Indexes:**
- `uk_code_tenant` UNIQUE on `(code, tenant_id)`
- `idx_category` on `category`
- `idx_status` on `status`
- `idx_tenant_id` on `tenant_id`

---

## Migration History

| Version | File | Description |
|---------|------|-------------|
| V1 | `sql/v1.0__init.sql` | Core RBAC tables (sys_user, sys_role, sys_user_role, sys_menu, sys_role_menu, sys_operation_log, template_entity) + seed data |
| V2 | `V2__equipment_and_asset_tables.sql` | Equipment, asset_location, maintenance_plan |
| V3 | `V3__inventory_and_consumable_tables.sql` | Consumable, inventory_record, inventory_transaction, supplier, stock_alert |
| V4 | `V4__add_missing_columns_and_indexes.sql` | Add failed_attempts/locked_until to sys_user, soft delete to sys_role, status to sys_menu, target_table index |
| V5 | `V5__workflow_tables.sql` | workflow_definition, workflow_instance, workflow_task |
