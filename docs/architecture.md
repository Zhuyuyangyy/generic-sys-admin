# ERMS System Architecture

## Overview

ERMS (Enterprise Resource Management System) is a full-stack enterprise resource operations platform for asset lifecycle management, consumable inventory, RBAC access control, audit compliance, and natural language operations.

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend Framework | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| Security | Spring Security + JWT | 6.x / jjwt 0.12.x |
| Cache | Redis | 7.x |
| Database | MySQL | 8.0 |
| Database Migration | Flyway | Built-in |
| Frontend Framework | Vue | 3.x |
| Frontend Language | TypeScript | 5.x |
| UI Library | Element Plus | Latest |
| State Management | Pinia | Latest |
| Charts | ECharts | 5.x |
| Object Storage | MinIO (optional) | Latest |
| API Documentation | Knife4j / OpenAPI 3.0 | 4.x |
| Build | Maven | 3.9.x |
| Deployment | Docker Compose + Nginx | Latest |

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Vue 3 SPA   │  │  ECharts     │  │  WebSocket Client│  │
│  │  + TypeScript│  │  Dashboard   │  │  Event Stream    │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼─────────────────┼───────────────────┼────────────┘
          │                 │                   │
          ▼                 ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                      Nginx Reverse Proxy                    │
│            (Static files + API proxy /ws proxy)             │
└──────────────────────┬──────────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                Security Layer                         │   │
│  │  JwtAuthFilter → SecurityContext → @PreAuthorize      │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │Controller│ │  AOP     │ │  NL      │ │  WebSocket   │   │
│  │  Layer   │ │  Audit   │ │  Engine  │ │  Event Push  │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘   │
│       │            │            │               │           │
│  ┌────▼────────────▼────────────▼───────────────▼───────┐   │
│  │                   Service Layer                       │   │
│  │  EquipmentService / ConsumableService / UserService   │   │
│  │  RbacService / OperationLogService / NLService        │   │
│  │  CausalDAGService / RiskAssessor / DashboardService   │   │
│  └─────────────────────┬────────────────────────────────┘   │
│                        │                                    │
│  ┌─────────────────────▼────────────────────────────────┐   │
│  │                   Data Access Layer                    │   │
│  │  MyBatis-Plus Mappers + XML Mapper Files              │   │
│  └─────────────────────┬────────────────────────────────┘   │
└────────────────────────┼────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  MySQL 8.0   │ │  Redis 7     │ │  MinIO       │
│  Primary DB  │ │  Cache/Auth  │ │  File Storage │
└──────────────┘ └──────────────┘ └──────────────┘
```

## Package Structure

### Current Structure (Flat / Feature-Sliced Hybrid)

The codebase is in transition from a flat package structure to a domain-driven design. Both coexist currently:

**Legacy flat structure** (`com.zyy`):
```
com.zyy/
├── controller/          # REST controllers (legacy, being migrated)
├── service/             # Business logic (legacy, being migrated)
├── mapper/              # Data access (legacy, being migrated)
├── model/               # DTOs, entities, VOs (legacy, being migrated)
├── config/              # Application configuration
├── security/            # JWT auth filter and utilities
├── aspect/              # AOP operation logging
├── enums/               # Business enums
├── exception/           # Custom exceptions
├── common/              # Shared Result, PageParam, etc.
├── util/                # Utility classes
├── rbac/                # RBAC models
├── nl/                  # Natural language engine
├── voice/               # TTS service
├── websocket/           # WebSocket event push
├── client/              # AI client
```

**New domain-driven structure** (partially migrated):
```
com.zyy/
├── iam/                 # Identity & Access Management
│   ├── controller/      # SysUserController, SysRoleController, SysMenuController
│   ├── service/         # SysUserService, SysRoleService, SysMenuService
│   ├── mapper/          # SysUserMapper, SysRoleMapper, SysMenuMapper
│   └── model/           # DTOs, entities, VOs for IAM
├── asset/               # Equipment & Asset Management
│   ├── controller/      # EquipmentController, LocationController, etc.
│   ├── service/         # EquipmentService, AssetHealthService, etc.
│   ├── mapper/
│   └── model/
├── inventory/           # Consumable & Inventory Management
│   ├── controller/      # ConsumableController, StockAlertController, etc.
│   ├── service/
│   ├── mapper/
│   └── model/
├── audit/               # Operation Logging & Anomaly Detection
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   └── model/
├── workflow/             # Approval Workflow
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   └── model/
├── report/              # Reporting
├── dashboard/           # Dashboard aggregation
├── file/                # File management
└── causal/              # Causal DAG engine
```

### Planned Refactoring (v0.2)

Full migration to domain-driven packages, removing the legacy flat structure. Each domain module will be self-contained with its own controller/service/mapper/model layers.

## Security Flow

```
Client Request
     │
     ▼
JwtAuthFilter (OncePerRequestFilter)
     │
     ├── Extract "Authorization: Bearer <token>" header
     ├── Validate token via JwtUtil.validate()
     │   ├── Parse claims (userId, username, permissions)
     │   └── Check expiration
     ├── Create UsernamePasswordAuthenticationToken
     │   ├── Principal: LoginUser(userId, username)
     │   └── Authorities: List<SimpleGrantedAuthority> from permissions claim
     └── Set SecurityContextHolder.context.authentication
               │
               ▼
     Spring Security Filter Chain
               │
               ├── PermitAll: /api/users/login, /api/users (POST), /api/ai/**, /swagger-ui/**
               └── Authenticated: all other /api/** endpoints
                        │
                        ▼
               @PreAuthorize checks (e.g., @ss.hasAuthority('system:role:list'))
                        │
                        ▼
               Controller Method
```

### JWT Token Lifecycle

1. **Login**: `POST /api/users/login` → BCrypt password verification → `JwtUtil.sign()` generates access token (2h) + refresh token (7d)
2. **Request**: Client sends `Authorization: Bearer <token>` → `JwtAuthFilter` validates and populates `SecurityContext`
3. **Refresh**: `POST /api/users/refresh-token` → Exchange refresh token for new access token
4. **Token Claims**: `{ sub: userId, username, userId, permissions: [...] }`

## Audit Flow

```
Controller Method Execution
     │
     ▼
OperationLogAspect (@Around advice)
     │
     ├── Pointcut: execution(* com.zyy..*Controller.*(..))
     │
     ├── Before execution:
     │   ├── Extract @Log annotation (module, operation, description)
     │   ├── If no @Log: infer module from class name, operation from method name
     │   ├── Build SysOperationLogEntity:
     │   │   ├── userId/username from SecurityContext (LoginUser)
     │   │   ├── module, operation, methodName
     │   │   ├── requestMethod, requestUrl, ipAddress, userAgent
     │   │   └── requestParams (serialized, excluding HttpServletRequest/Response/MultipartFile)
     │
     ├── Proceed with original method
     │
     └── After execution (finally block):
         ├── Record duration (ms)
         ├── Set resultStatus: 1=success, 0=failure
         ├── On exception: capture errorDetail (truncated to 500 chars)
         ├── Async persist: operationLogService.saveLog()
         └── WebSocket push: webSocketService.pushOperationLog()
```

### Business Type Inference

When `@Log` annotation is absent, operation type is inferred from method name prefix:

| Method Prefix | BusinessType |
|---------------|-------------|
| `add*`, `create*`, `save*` | INSERT |
| `update*`, `modify*` | UPDATE |
| `delete*`, `remove*` | DELETE |
| `login*` | LOGIN |
| `export*` | EXPORT |
| `import*` | IMPORT |
| other | OTHER |

## Natural Language (NL) Flow

```
User Input: "删除设备EQ-2024-001"
     │
     ▼
NLService.parse(input)
     │
     ├── Intent Detection (keyword matching):
     │   "删除" → DELETE, "修改" → UPDATE, "添加" → CREATE, "查询" → QUERY
     │
     ├── Entity Extraction:
     │   ├── Entity ID: regex (EQ|CS|USER|INV)-\d{4}-\d{3} or fallback defaults
     │   └── Entity Type: "设备" → EQUIPMENT, "耗材" → CONSUMABLE, "用户" → USER
     │
     └── Causal Impact Check (for non-QUERY intents):
         └── CausalDAGService.predictImpact(entityId, entityType)
              ├── Returns CausalGraph with impacted nodes
              └── Flags high-impact operations

NLService.executeWithCausalCheck(input)
     │
     ├── Calls parse() → gets NLParseResult
     ├── Returns ExecuteResult with:
     │   ├── intent, entityId, entityType
     │   ├── causalCheckPerformed (true for write operations)
     │   ├── impactedNodesCount, hasHighImpact
     │   └── latencyMs

NLService.dryRun(input)  → Dry Run Protocol
     │
     ├── Parses input → assesses risk via RiskAssessor
     ├── Generates confirmationId → stores in ConfirmationStore
     └── Returns DryRunResult:
         ├── confirmationId, intent, entities
         ├── affectedTables, expectedChanges
         └── riskLevel, requiresApproval, confirmRequired

NLService.executeConfirmed(input, confirmationId)
     │
     ├── Validates confirmationId from ConfirmationStore
     ├── Verifies intent hasn't changed since dry-run
     └── Executes with causal check
```

### NL Parser Architecture

```
NLParser (interface)
     │
     ├── NLRuleEngine     — Rule-based keyword matching (fast, deterministic)
     ├── LLMNLParser      — LLM-powered parsing (flexible, requires API key)
     └── HybridNLParser   — Combines rule + LLM with fallback chain
```

### Causal DAG Engine

```
CausalDAG → CausalNode[] + CausalEdge[]
     │
     ├── Nodes: Equipment, Consumable, Inventory, User, etc.
     ├── Edges: Directed causal relationships with weights
     └── CausalPropagationEngine:
         ├── Topological ordering for propagation
         └── Decay factor (0.8) for downstream impact attenuation
```

## Data Flow

```
Vue 3 Frontend
     │  HTTP/REST
     ▼
Nginx (port 80/443)
     │  proxy_pass /api → backend:8080
     │  proxy_pass /ws  → backend:8080/ws
     ▼
Spring Boot (port 8080)
     │
     ├── Read Path: Controller → Service → Mapper → MySQL
     │                                └→ Redis Cache
     │
     ├── Write Path: Controller → Service → Mapper → MySQL
     │                   └→ AOP: OperationLogAspect → sys_operation_log
     │                   └→ WebSocket: push notification
     │
     └── NL Path: NLController → NLService → Intent Detection
                    └→ CausalDAGService → RiskAssessor → NLExecutor
```

## External Service Integration

| Service | Purpose | Configuration |
|---------|---------|--------------|
| MiniMax API | TTS, Image/Video Generation | `minimax.api-key`, `minimax.api-url` |
| MinIO | Object file storage (optional) | `minio.endpoint`, `minio.access-key` |

Both are optional. When MinIO is unavailable, the system falls back to local filesystem storage (`LocalFileStorageStrategy`). When MiniMax API key is not configured, AI/TTS features return graceful errors.
