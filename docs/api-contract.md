# ERMS API Contract

## Overview

All API endpoints are prefixed with `/api` and return a unified `Result<T>` response envelope:

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

Pagination uses `PageParam` query parameters: `pageNum` (default 1), `pageSize` (default 10).

Authentication: Bearer token in `Authorization` header for all endpoints except those marked as public.

---

## Authentication & User Management

### POST /api/users/login
Authenticate user and receive JWT tokens. **Public endpoint.**

**Request:**
```json
{
  "username": "admin",
  "password": "123456"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员"
  }
}
```

### POST /api/users
Register a new user account. **Public endpoint.**

**Request:**
```json
{
  "username": "newuser",
  "password": "password123",
  "realName": "New User",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

### GET /api/users
List users with pagination and filters. **Authenticated.**

**Query Parameters:**
- `pageNum` (default 1)
- `pageSize` (default 10)
- `username` (optional, partial match)
- `status` (optional: 0=disabled, 1=normal, 2=locked)

### GET /api/users/{id}
Get user by ID. **Authenticated.**

### PUT /api/users/{id}
Update user profile (admin operation). **Authenticated.**

**Request:** `SysUserUpdateDTO` body

### DELETE /api/users/{id}
Soft delete user. **Authenticated.**

### GET /api/users/me
Get current authenticated user's profile. **Authenticated.**

### PUT /api/users/me
Update current user's own profile. **Authenticated.**

### PUT /api/users/me/password
Change current user's password. **Authenticated.**

**Request:**
```json
{
  "oldPassword": "123456",
  "newPassword": "newPassword123"
}
```

### POST /api/users/refresh-token
Exchange refresh token for new access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```

### PATCH /api/users/{id}/status
Enable or disable user account.

**Query Parameters:** `enabled` (boolean)

### POST /api/users/{id}/lock
Lock user account.

### POST /api/users/{id}/unlock
Unlock user account and reset failed attempts.

---

## Role Management

### GET /api/roles
Paginated role list. **Requires:** `system:role:list`

**Query Parameters:**
- `pageNum`, `pageSize`
- `name` (optional, filter)
- `status` (optional)

### GET /api/roles/{id}
Get role by ID. **Requires:** `system:role:list`

### GET /api/roles/enabled
List all enabled roles (for dropdown selectors). **Authenticated.**

### POST /api/roles
Create role. **Requires:** `system:role:add`

**Request:** `SysRoleEntity` body (name, code, description, sortOrder, status)

### PUT /api/roles/{id}
Update role. **Requires:** `system:role:edit`

### DELETE /api/roles/{id}
Delete role. **Requires:** `system:role:del`

### PUT /api/roles/{id}/menus
Assign menus to role. **Requires:** `system:role:grant`

**Request:** JSON array of menu IDs: `[1, 2, 3, 5]`

---

## Menu Management

### GET /api/menus/current
Get dynamic menu tree for the current user. **Authenticated.**

### GET /api/menus/tree
Get full menu tree (for role management). **Requires:** `system:menu:list`

---

## Equipment Management

### GET /api/equipment
Paginated equipment list with filters. **Authenticated.**

**Query Parameters:**
- `pageNum`, `pageSize`
- `name` (optional, partial match)
- `category` (optional, exact match)
- `status` (optional: 0=maintenance, 1=normal, 2=scrapped)

### POST /api/equipment
Register new equipment. **Authenticated.**

**Request:**
```json
{
  "equipmentCode": "EQ-2024-001",
  "name": "CNC Machine A1",
  "category": "Manufacturing",
  "model": "CNC-500",
  "manufacturer": "Siemens",
  "purchaseDate": "2024-01-15",
  "warrantyExpiry": "2027-01-15",
  "status": 1,
  "location": "Workshop B-2",
  "maintenanceCycleDays": 90,
  "remarks": "Primary production unit"
}
```

### GET /api/equipment/{id}
Get equipment by ID. **Authenticated.**

### PUT /api/equipment/{id}
Update equipment details. **Authenticated.**

**Request:** `EquipmentUpdateDTO` body

### PATCH /api/equipment/{id}/status
Transition equipment status. **Authenticated.**

**Query Parameters:** `status` (0=maintenance, 1=normal, 2=scrapped)

### DELETE /api/equipment/{id}
Soft delete equipment. **Authenticated.**

---

## Consumable Management

### GET /api/consumables
Paginated consumable list with filters. **Authenticated.**

**Query Parameters:**
- `pageNum`, `pageSize`
- `name` (optional)
- `category` (optional)
- `status` (optional: 0=unavailable, 1=available)

### POST /api/consumables
Register new consumable. **Authenticated.**

**Request:**
```json
{
  "productCode": "CS-2024-008",
  "name": "Lubricant Oil 5W-30",
  "category": "Lubricants",
  "unit": "liter",
  "stockQuantity": 200,
  "minStockLevel": 50,
  "unitCost": 35.00,
  "supplier": "ChemCorp",
  "storageLocation": "Warehouse A-3"
}
```

### GET /api/consumables/{id}
Get consumable by ID. **Authenticated.**

### PUT /api/consumables/{id}
Update consumable details. **Authenticated.**

**Request:** `ConsumableUpdateDTO` body

### POST /api/consumables/{id}/inbound
Record incoming stock. **Authenticated.**

**Query Parameters:**
- `quantity` (required) — amount to add
- `referenceNo` (optional) — PO or reference number
- `remarks` (optional)

### POST /api/consumables/{id}/outbound
Record outgoing stock. **Authenticated.**

**Query Parameters:**
- `quantity` (required) — amount to deduct
- `referenceNo` (optional)
- `remarks` (optional)

### PATCH /api/consumables/{id}/stock
Adjust stock (positive or negative delta). **Authenticated.**

**Query Parameters:**
- `delta` (required) — quantity change (+/-)
- `referenceNo` (optional)
- `remarks` (optional)

### DELETE /api/consumables/{id}
Soft delete consumable. **Authenticated.**

---

## Inventory Records (Maintenance/Inspection)

### GET /api/inventory-records
Paginated inventory record list. **Authenticated.**

**Query Parameters:**
- `pageNum`, `pageSize`
- `equipmentId` (optional)
- `recordType` (optional: ROUTINE / MAINTENANCE / CALIBRATION / INSPECTION)
- `startDate` (optional: yyyy-MM-dd)
- `endDate` (optional: yyyy-MM-dd)

### POST /api/inventory-records
Create maintenance/inspection record. **Authenticated.**

**Request:**
```json
{
  "equipmentId": 1,
  "recordType": "MAINTENANCE",
  "recordDate": "2024-06-15",
  "inspectorId": 1,
  "inspectorName": "张三",
  "result": "PASS",
  "equipmentCondition": "GOOD",
  "description": "Quarterly preventive maintenance",
  "manHours": 4.5
}
```

### PUT /api/inventory-records/{id}
Update record. **Authenticated.**

### GET /api/inventory-records/{id}
Get record by ID. **Authenticated.**

### GET /api/inventory-records/equipment/{equipmentId}
Get maintenance history for specific equipment. **Authenticated.**

### DELETE /api/inventory-records/{id}
Delete record. **Authenticated.**

---

## Natural Language Operations

### POST /api/nl/execute-with-causal-check
Execute NL command with causal impact analysis. **Authenticated.**

**Request:**
```json
{
  "input": "删除设备EQ-2024-001"
}
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "intent": "DELETE",
    "entityId": "EQ-2024-001",
    "causalCheckPerformed": true,
    "impactedNodesCount": 3,
    "hasHighImpact": false,
    "latencyMs": 45
  }
}
```

### POST /api/nl/parse
Parse NL input without executing. **Authenticated.**

**Request:**
```json
{
  "input": "查询设备EQ-2024-001的状态"
}
```

**Response:** `NLParseResult` with intent, entityId, entityType, causalGraph (if applicable)

### POST /api/nl/dry-run
Preview NL command execution without side effects. **Authenticated.**

**Request:**
```json
{
  "input": "删除设备EQ-2024-001"
}
```

**Response:** `DryRunResult` with confirmationId, intent, entities, affectedTables, expectedChanges, riskLevel, requiresApproval, confirmRequired

### POST /api/nl/execute-confirmed
Execute NL command after dry-run confirmation. **Authenticated.**

**Request:**
```json
{
  "input": "删除设备EQ-2024-001",
  "confirmationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### GET /api/nl/test-commands
Get standard test command set. **Authenticated.**

### GET /api/nl/causal/dag
Get causal DAG topology information (diagnostic). **Authenticated.**

---

## Operation Logs (Audit)

### GET /api/logs
Paginated operation log list. **Requires:** `sys:log:list`

**Query Parameters:**
- `pageNum`, `pageSize`
- `module` (optional)
- `operation` (optional: INSERT/DELETE/UPDATE/SELECT/LOGIN)
- `operator` (optional)
- `status` (optional: 0=failed, 1=success)
- `startTime` (optional: yyyy-MM-dd)
- `endTime` (optional: yyyy-MM-dd)

### GET /api/logs/{id}
Get log detail by ID. **Requires:** `sys:log:query`

---

## Dashboard

### GET /api/dashboard/stats
Aggregated dashboard statistics. **Requires:** `dashboard:view`

**Response:** `DashboardStatsVO` with equipment/consumable/user stats and log trends

---

## File Management

### POST /api/files/upload
Upload a single file. **Authenticated. Multipart form-data.**

**Request:** `file` (MultipartFile)

**Constraints:**
- Max size: 10 MB
- Allowed types: jpg, jpeg, png, gif, bmp, webp, pdf, doc, docx, xls, xlsx, ppt, pptx, txt

**Response:**
```json
{
  "code": 200,
  "data": {
    "url": "http://localhost:9000/generic-sys-admin/f7c3a8b1.jpg",
    "filename": "f7c3a8b1.jpg",
    "originalName": "avatar.jpg",
    "size": 102400,
    "contentType": "image/jpeg"
  }
}
```

### POST /api/files/upload/batch
Batch upload files (max 20). **Authenticated. Multipart form-data.**

**Request:** `files` (MultipartFile[])

### DELETE /api/files
Delete a file by URL. **Authenticated.**

**Query Parameters:** `url` (full file URL)

### DELETE /api/files/batch
Batch delete files. **Authenticated.**

**Request:**
```json
{
  "urls": ["http://localhost:9000/bucket/file1.jpg", "http://localhost:9000/bucket/file2.pdf"]
}
```

---

## AI Services (Optional)

### POST /api/ai/tts
Text-to-speech synthesis. **Public endpoint.**

**Query Parameters:** `text` (required), `voice` (default: `male-qn-qingse`)

### POST /api/ai/image
AI image generation. **Public endpoint.**

**Query Parameters:** `prompt` (required), `aspectRatio` (default: `1:1`, options: `1:1`/`16:9`/`9:16`/`3:4`/`4:3`)

### POST /api/ai/video/generate
AI video generation (async). **Public endpoint.**

**Query Parameters:** `prompt` (required), `duration` (default: 5, options: 5/10)

### GET /api/ai/video/status/{jobId}
Query video generation status. **Public endpoint.**

### GET /api/ai/models
List available AI models. **Public endpoint.**

---

## TTS Voice (Optional)

### POST /voice/synthesize
Text-to-speech synthesis. **Authenticated.**

**Request:**
```json
{
  "text": "Hello world",
  "voiceId": "male-qn-qingse",
  "speed": 1.0,
  "volume": 50.0,
  "pitch": 1.0
}
```

---

## Error Response Format

All errors follow the unified `Result` envelope:

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": null
}
```

Common HTTP status codes used:

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad request / Validation error |
| 401 | Unauthorized (missing or invalid token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Resource not found |
| 500 | Internal server error |
