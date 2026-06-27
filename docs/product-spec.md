# ERMS Product Specification

## Product Overview

**ERMS** (Enterprise Resource Management System) is an enterprise resource operations platform that provides asset lifecycle management, consumable inventory tracking, role-based access control, audit compliance, and natural language command execution with causal impact analysis.

### Target Users

- Enterprise IT and operations administrators
- Asset management teams
- Warehouse and inventory personnel
- Compliance and audit officers

### Value Proposition

- Unified platform for equipment and consumable lifecycle management
- Automated audit trail for all write operations
- Natural language interface for business commands with safety guardrails
- Fine-grained RBAC with menu and button-level permission control

## Core Modules

### 1. Equipment Management

Equipment asset lifecycle tracking from registration through maintenance to disposal.

**Features:**
- Equipment registration with code, name, category, model, manufacturer
- Status management with state machine validation:
  - `0` = Maintenance (under repair)
  - `1` = Normal (operational)
  - `2` = Scrapped (retired)
- Location tracking (asset_location with hierarchical parent-child structure)
- Maintenance plan scheduling (PREVENTIVE / CORRECTIVE / EMERGENCY)
- Maintenance history via inventory_record (ROUTINE / MAINTENANCE / CALIBRATION / INSPECTION)
- Asset health scoring and overview dashboard
- Soft delete with audit trail

**Business Rules:**
- Equipment code must be unique (`uk_equipment_code`)
- Status transitions are validated by the service layer
- All write operations are automatically logged by AOP audit

### 2. Consumable & Inventory Management

Consumable product tracking with stock control and transaction logging.

**Features:**
- Consumable registration with product code, name, category, unit, unit cost
- Stock management:
  - Inbound (procurement, returns) — `POST /api/consumables/{id}/inbound`
  - Outbound (usage, issuance) — `POST /api/consumables/{id}/outbound`
  - Manual adjustment — `PATCH /api/consumables/{id}/stock`
- Minimum/maximum stock level configuration
- Low-stock alert thresholds with `reorder_point` and `min_stock_level`
- Inventory transaction log with balance-after tracking
- Supplier management (contact info, status)
- Stock alerts (LOW_STOCK / EXPIRING / OVERSTOCK) with acknowledgment workflow
- Expiration date tracking

**Business Rules:**
- Product code must be unique (`uk_product_code`)
- Outbound operations must not exceed available stock
- All stock changes create an inventory_transaction record
- Stock alerts trigger when `stock_quantity` drops below `min_stock_level`

### 3. User / Role / Menu Management (RBAC)

Hierarchical role-based access control with JWT stateless authentication.

**Features:**
- User management:
  - Registration, profile update, password change
  - Account status control (enabled / disabled / locked)
  - Failed login tracking with automatic lockout
  - Last login IP and timestamp recording
- Role management:
  - Role CRUD with unique code
  - Menu/permission assignment to roles
  - Sort ordering for UI display
- Menu management:
  - Hierarchical tree structure (parent_id)
  - Three menu types: Directory (1), Menu page (2), Button permission (3)
  - Permission identifiers (e.g., `system:user:query`, `system:role:add`)
  - Dynamic menu generation based on user's roles
- Authentication:
  - JWT access token (default 2 hours) + refresh token (default 7 days)
  - BCrypt password hashing
  - Stateless session management (no server-side sessions)

**RBAC Hierarchy:**
```
User → sys_user_role → Role → sys_role_menu → Menu/Permission
```

**Pre-seeded Data:**
- Super Admin role (`SUPER_ADMIN`) — all permissions
- Normal User role (`USER`) — limited permissions (dashboard only)
- Admin user (`admin` / `123456`) — bound to SUPER_ADMIN
- Test user (`user` / `123456`) — bound to USER

### 4. Audit Trail

AOP-based automatic operation logging for full traceability.

**Features:**
- Automatic logging of all controller method invocations
- Configurable via `@Log` annotation (module, operation type, description)
- Fallback inference when `@Log` is absent (module from class name, operation from method prefix)
- Captured data per log entry:
  - Operator: userId, username (from SecurityContext)
  - Request: method, URL, IP address, User-Agent, parameters (JSON)
  - Execution: duration (ms), result status (success/failure)
  - Error: exception detail (truncated to 500 chars) on failure
- Anomaly detection service for identifying suspicious operation patterns
- Operation log query API with multi-condition filtering
- WebSocket real-time push of operation events

**Operation Types:** INSERT, DELETE, UPDATE, SELECT, LOGIN, EXPORT, IMPORT, OTHER

### 5. Natural Language Operations

Intent-based NL command execution with causal impact analysis and dry-run safety protocol.

**Features:**
- Intent detection via keyword matching (rule engine):
  - "删除" → DELETE, "修改/更新" → UPDATE, "增加/添加" → CREATE, "查询/查看" → QUERY
- Entity extraction:
  - Entity ID via regex pattern `(EQ|CS|USER|INV)-\d{4}-\d{3}`
  - Entity type from keywords: "设备" → EQUIPMENT, "耗材" → CONSUMABLE, "用户" → USER, "库存" → INVENTORY
- Causal impact analysis:
  - CausalDAG models relationships between business entities
  - CausalPropagationEngine computes downstream impact
  - Decay factor (0.8) attenuates impact along propagation chains
  - High-impact operations are flagged
- Dry Run protocol:
  - Parse input and assess risk without executing
  - Return confirmationId, affected tables, expected changes, risk level
  - Risk levels: LOW / MEDIUM / HIGH / CRITICAL
  - Requires approval for HIGH/CRITICAL risk
  - Confirmation required for MEDIUM and above
- Confirmation execution:
  - Validate confirmationId (single-use, expires)
  - Verify intent consistency between dry-run and execution
  - Execute with causal check
- Hybrid parser architecture:
  - NLRuleEngine for fast deterministic matching
  - LLMNLParser for flexible natural language understanding
  - HybridNLParser combines both with fallback chain

**NL Safety Guardrails:**
- Query intents bypass causal check (read-only)
- Write operations always trigger causal impact prediction
- Bulk operations ("批量", "所有", "全部") elevate risk level
- Confirmation IDs are single-use and expire after consumption

### 6. Dashboard

Aggregated statistics for system overview.

**Features:**
- Equipment count by status (normal / maintenance / scrapped)
- Consumable inventory summary
- User statistics
- Operation log trend data
- Recent activity feed

### 7. Workflow (In Progress)

Lightweight approval workflow engine.

**Features:**
- Workflow definition with configurable steps (JSON)
- Workflow instance lifecycle management
- Task assignment and action tracking (APPROVE / REJECT)
- Status tracking: PENDING → IN_PROGRESS → COMPLETED / CANCELLED

### 8. File Management

File upload/download with dual storage strategy.

**Features:**
- MinIO object storage (primary, when available)
- Local filesystem fallback (LocalFileStorageStrategy)
- Allowed file types:
  - Images: jpg, jpeg, png, gif, bmp, webp
  - Documents: pdf, doc, docx, xls, xlsx, ppt, pptx, txt
- Maximum file size: 10 MB
- Batch upload (max 20 files) and batch delete
- Auto-detection of MinIO availability

## Future Modules (Planned)

| Module | Version | Description |
|--------|---------|-------------|
| Workflow Engine | v0.3 | Lightweight approval with configurable steps |
| Maintenance Plans | v0.3 | Scheduled maintenance with reminders |
| Stock Alerts | v0.3 | Automatic low-stock and expiration alerts |
| Equipment Health Scoring | v0.7 | Predictive health assessment |
| Predictive Restock | v0.7 | AI-driven reorder recommendations |
| Audit Anomaly Detection | v0.7 | ML-based anomaly detection on audit logs |
| Multi-tenant Isolation | v0.8+ | Tenant-level data isolation and ABAC |

## Product Constraints

- Default passwords (`admin`/`123456`) must be changed before production
- WebSocket events are in-memory only (lost on restart)
- NL operations require confirmation for write commands
- AI features (TTS, image generation) are optional and not core platform capabilities
- File uploads limited to 10 MB per file
- JWT tokens have a maximum validity of 7 days (refresh token)
