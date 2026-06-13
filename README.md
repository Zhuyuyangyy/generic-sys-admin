# Generic Sys Admin -- Enterprise Resource Management System (ERMS)

> An enterprise-grade platform for equipment lifecycle management, consumable inventory control, RBAC-based access control, natural-language business flow orchestration, and full-chain audit logging. Built with Spring Boot 3.4 + Vue 3.

---

## Overview

Generic Sys Admin is a full-stack enterprise resource management system designed to digitize physical asset management for organizations. It provides unified equipment ledger management, consumable inventory workflows, role-based access control, real-time status monitoring via WebSocket, and a natural-language interface that allows non-technical users to operate the system through plain-language commands.

The system follows the NIST RBAC standard, implements AOP-based operation logging for complete audit trails, and integrates optional AI services (TTS, image generation, video generation) via the MiniMax API.

---

## Key Features

| Feature | Description |
|---------|-------------|
| **RBAC Permission Model** | NIST-standard Role-Based Access Control with user-role-menu three-layer association |
| **NL Business Flow Engine** | Natural language command parsing with intent recognition, entity extraction, and rule-based execution |
| **Real-Time Monitoring** | WebSocket push for equipment status changes, consumable stock alerts, and operation logs |
| **Full-Chain Audit** | AOP aspect automatically records all critical operations with user, module, parameters, IP, and duration |
| **Equipment Lifecycle** | Complete tracking from procurement through maintenance to disposal |
| **Consumable Inventory** | Inbound, outbound, adjustment, and return workflows with min/max stock thresholds and batch management |
| **AI Service Integration** | Built-in MiniMax multi-modal AI: TTS, image generation, and video generation |
| **Spring Boot 3.4** | Latest Spring Boot 3.4 + Spring Security 6.x + MyBatis-Plus 3.5 |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Client Layer                                  │
│   Vue 3 + TypeScript + Element Plus + Axios + Pinia + WebSocket     │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ HTTP/REST (JSON) + WebSocket
┌────────────────────────────────▼────────────────────────────────────┐
│                     API Layer (Spring Boot 3.4)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │Equipment │ │Consumable│ │   User   │ │   Role   │ │    NL    │  │
│  │Controller│ │Controller│ │Controller│ │Controller│ │ Service  │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │
│       └─────────────┴────────────┴────────────┴────────────┘        │
│                              │                                      │
│  ┌───────────────────────────▼────────────────────────────────────┐ │
│  │                    Service Layer                                │ │
│  │  EquipmentService  ConsumableService  NLService  AIService     │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
│                              │                                      │
│  ┌───────────────────────────▼────────────────────────────────────┐ │
│  │  Security (JWT + Spring Security)  │  AOP (Operation Logging)  │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
│                              │                                      │
│  ┌───────────────────────────▼────────────────────────────────────┐ │
│  │                 MyBatis-Plus ORM                                │ │
│  └───────────────────────────┬────────────────────────────────────┘ │
└──────────────────────────────┼──────────────────────────────────────┘
                               │ JDBC
┌──────────────────────────────▼──────────────────────────────────────┐
│                          Data Layer                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────────┐ │
│  │   MySQL 8.0     │  │  Redis (opt.)   │  │    WebSocket         │ │
│  │   Primary Store │  │  Cache/Session  │  │    Real-time Push    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

### Backend

| Category | Technology | Version |
|----------|-----------|---------|
| Runtime | Java | 17+ (LTS) |
| Core Framework | Spring Boot | 3.4.x |
| Security | Spring Security | 6.x |
| Authentication | JWT (jjwt) | 0.12.x |
| ORM | MyBatis-Plus | 3.5.x |
| Database | MySQL | 8.0+ |
| Cache | Redis | 7.x (optional, graceful degradation) |
| WebSocket | Spring WebSocket | 6.x |
| NL Engine | Custom rule-based parser | 1.0 |
| API Docs | Knife4j / Swagger | 4.x |
| Utilities | Hutool, Lombok | 5.x, 1.18.x |
| Build | Maven | 3.8+ |

### Frontend

| Category | Technology | Version |
|----------|-----------|---------|
| Framework | Vue | 3.x |
| Type System | TypeScript | 5.x |
| UI Library | Element Plus | 2.x |
| Build Tool | Vite | 5.x |
| State Management | Pinia | 2.x |
| HTTP Client | Axios | 1.x |
| Router | Vue Router | 4.x |

---

## Quick Start

### Option 1: Docker Deployment (Recommended)

```bash
git clone <repository-url>
cd generic-sys-admin

# Start all services (MySQL + Backend + Frontend)
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
```

Startup order: MySQL (5s) -> Backend (15s) -> Frontend. Full startup takes approximately 30 seconds.

### Option 2: Manual Deployment

#### Prerequisites

| Dependency | Version |
|-----------|---------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7.x (optional) |

#### Step 1: Initialize Database

```bash
mysql -u root -p < sql/v1.0__init.sql
mysql -u root -p < sql/v1.1__operation_log.sql
```

#### Step 2: Configure Backend

Edit `backend/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_password_here

jwt:
  secret: your-256-bit-secret-key-change-in-production
  expiration: 86400000
```

#### Step 3: Build and Run Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/generic-sys-admin-1.0.0.jar --spring.profiles.active=dev
```

#### Step 4: Build and Run Frontend

```bash
cd frontend
npm install
npm run dev
```

#### Default Accounts

| Username | Password | Role |
|----------|----------|------|
| `admin` | `123456` | System Administrator (full access) |
| `operator` | `123456` | Operator (equipment/consumable management) |
| `viewer` | `123456` | Viewer (read-only) |

> **Change default passwords immediately in production environments.**

---

## NL Natural Language Business Flow

The NL engine allows users to operate the system through natural language commands, eliminating the need to navigate menus.

### How It Works

```
User: "查询设备编号为 EQ-2024-001 的维护记录"
         │
         ▼
   NL Parser ── Tokenization, entity recognition, intent classification
         │
         ▼
   Rule Engine ── Match business rule templates, generate execution plan
         │
         ▼
   Executor ── Call corresponding Service method
         │
         ▼
   Return structured result to user
```

### Supported Commands

| Command Example | Intent | Parameters |
|----------------|--------|------------|
| "查询设备 EQ-2024-001" | Query device | `deviceCode` |
| "列出所有维护中的设备" | List devices | `status=0` |
| "给设备 EQ-2024-001 做巡检" | Create inspection | `deviceCode`, `inspectionType` |
| "入库耗材键盘 50个" | Consumable inbound | `productName`, `quantity` |
| "查询我的操作日志" | Query logs | `currentUser` |
| "给用户张三分配管理员角色" | Assign role | `username`, `roleName` |

---

## RBAC Permission Model

Four-layer permission model aligned with NIST RBAC:

```
Permission (Menu + CRUD)  ◄──N:M──►  Role  ◄──N:M──►  User
```

| Role Code | Role Name | Access Level |
|-----------|-----------|-------------|
| `ROLE_ADMIN` | System Administrator | Full system access |
| `ROLE_EQUIPMENT_ADMIN` | Equipment Manager | Equipment lifecycle management |
| `ROLE_CONSUMABLE_ADMIN` | Consumable Manager | Inventory management |
| `ROLE_OPERATOR` | Operator | Inspection and requisition |
| `ROLE_VIEWER` | Viewer | Read-only access |

Permission enforcement:

- **Backend**: `@PreAuthorize` annotations with Spring Security method-level security
- **Frontend**: Route guards with dynamic menu rendering based on user roles

---

## Functional Modules

| Module | Key Capabilities |
|--------|-----------------|
| **User Management** | CRUD, password reset, account locking (5 failed attempts), role assignment |
| **Role Management** | Role CRUD, menu permission assignment, multi-role per user |
| **Equipment Management** | Equipment ledger, status lifecycle (Normal/Maintenance/Scrapped), maintenance scheduling, inspection records |
| **Consumable Management** | Inventory ledger, inbound/outbound/adjustment/return transactions, min/max stock alerts, batch and expiration management |
| **Operation Log** | AOP-based automatic logging of all critical operations with full request/response details |
| **NL Interface** | Natural language command parsing and execution |
| **AI Services** | MiniMax TTS, image generation, video generation (optional) |
| **Dashboard** | Real-time WebSocket monitoring of equipment status and consumable alerts |

---

## Project Structure

```
generic-sys-admin/
├── backend/
│   ├── src/main/java/com/zyy/
│   │   ├── GenericSysAdminApplication.java    # Application entry
│   │   ├── controller/                        # REST controllers
│   │   │   ├── SysUserController.java
│   │   │   ├── RoleController.java
│   │   │   ├── MenuController.java
│   │   │   ├── EquipmentController.java
│   │   │   ├── ConsumableController.java
│   │   │   ├── InventoryRecordController.java
│   │   │   ├── OperationLogController.java
│   │   │   ├── AIController.java
│   │   │   ├── DashboardController.java
│   │   │   └── FileController.java
│   │   ├── service/                           # Business logic
│   │   │   └── impl/
│   │   ├── mapper/                            # MyBatis-Plus mappers
│   │   ├── model/
│   │   │   ├── entity/                        # Database entities
│   │   │   ├── dto/                           # Request DTOs
│   │   │   └── vo/                            # Response VOs
│   │   ├── rbac/                              # RBAC core module
│   │   ├── security/                          # JWT + Spring Security
│   │   ├── nl/                                # NL parsing engine
│   │   │   ├── parser/
│   │   │   ├── rules/
│   │   │   └── executor/
│   │   ├── aspect/                            # AOP logging
│   │   ├── websocket/                         # WebSocket config
│   │   ├── config/                            # Application config
│   │   ├── exception/                         # Global exception handling
│   │   ├── enums/
│   │   └── util/
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── nl-rules/                          # NL rule definitions
├── frontend/
│   └── src/
│       ├── api/                               # API client
│       ├── views/
│       │   ├── dashboard/
│       │   ├── system/                        # User/Role/Menu/Log
│       │   ├── equipment/
│       │   ├── consumable/
│       │   ├── inventory/
│       │   └── ai-studio/                     # AI services UI
│       ├── router/
│       ├── stores/
│       ├── utils/
│       └── websocket/
├── sql/
│   ├── v1.0__init.sql                         # Full schema + seed data
│   └── v1.1__operation_log.sql                # Operation log table
├── docker-compose.yml
├── start.sh
├── docs/
│   ├── 技术交底书.md
│   ├── SCI_Paper_Skeleton.md
│   └── Claim_Evidence_Table.md
├── tests/
│   └── test_smoke.py
└── REPRODUCE.md
```

---

## Security

| Mechanism | Implementation |
|-----------|---------------|
| Password Storage | BCrypt (cost factor = 10), never stored in plaintext |
| Authentication | JWT tokens with 24-hour expiry |
| Account Protection | Auto-lock after 5 failed login attempts (30-minute cooldown) |
| SQL Injection | MyBatis-Plus parameterized queries |
| XSS Prevention | Input filtering + Element Plus built-in XSS protection |
| Interface Security | `@PreAuthorize` annotation-based access control |
| Data Masking | Sensitive fields (passwords) cleared before API response |
| CORS | Configurable allowed origins (restrict in production) |

---

## API Documentation

After starting the backend, access the interactive API documentation at:

```
http://localhost:8080/doc.html    (Knife4j UI)
```

### Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/login` | POST | User authentication, returns JWT |
| `/api/users` | GET/POST/PUT/DELETE | User management |
| `/api/roles` | GET/POST/PUT/DELETE | Role management |
| `/api/equipment` | GET/POST/PUT/DELETE | Equipment management |
| `/api/consumables` | GET/POST/PUT/DELETE | Consumable management |
| `/api/consumables/{id}/inbound` | POST | Consumable inbound |
| `/api/consumables/{id}/outbound` | POST | Consumable outbound |
| `/api/nl/execute` | POST | Execute NL command |
| `/api/logs` | GET | Operation log query |
| `/api/ai/tts` | POST | Text-to-speech |
| `/api/ai/image` | POST | Image generation |
| `/api/ai/video/generate` | POST | Video generation |
| `/ws/monitor` | WebSocket | Real-time status monitoring |

---

## Database Design

The system uses 9 core tables:

| Table | Description |
|-------|-------------|
| `sys_user` | User accounts with BCrypt passwords and lockout tracking |
| `sys_role` | Role definitions (Admin, Equipment Admin, Operator, Viewer) |
| `sys_menu` | Menu tree with permission identifiers |
| `sys_user_role` | User-role many-to-many association |
| `sys_role_menu` | Role-menu many-to-many association |
| `sys_equipment` | Equipment ledger with lifecycle status |
| `sys_consumable` | Consumable inventory with stock thresholds |
| `sys_inventory_transaction` | Consumable inbound/outbound/adjustment records |
| `sys_operation_log` | Full audit trail for all operations |
| `sys_nl_command_log` | NL command execution history |

---

## Limitations and Honest Disclaimers

This project is a **research prototype / course graduation project**, not a production system. The following disclaimers clarify known limitations that readers should be aware of:

### Self-Review Score

> An earlier self-evaluation assigned a score of **7.50/10**. An independent re-evaluation found the actual score to be approximately **5.79/10**. The discrepancy arises from over-claiming novelty on features that are standard implementations (RBAC, AOP logging, WebSocket) and from conflating "code exists" with "code is rigorously validated." We acknowledge this gap and do not stand behind the 7.50 figure.

### NL Engine: Rule-Based Keyword Matching, Not a True NLU System

The natural language interface uses a **rule-based keyword parser** (regex + template matching), not a trained NLU/NLP model. It supports a fixed set of command templates and will fail silently or produce incorrect results on out-of-vocabulary inputs. The "NL" label is aspirational; the implementation is closer to a structured command parser with Chinese keyword aliases.

### Causal Graph: Hardcoded 7-Edge DAG, Not Dynamically Learned

The causal impact system uses a **statically defined 7-edge DAG** over 4 system module nodes (`EQUIPMENT_MGMT`, `CONSUMABLE_MGMT`, `OPERATION_LOG`, `CONSUMABLE_ALERT`). The graph is hand-coded in `CausalDAGService.buildSystemDAG()` and does not learn from data. The claim of "dynamic causal graph construction" in patent or paper documents should be understood as "the code structure supports dynamic edges," not that the system actually infers causal relationships from observed data. BFS traversal over a 4-node graph is trivial and does not constitute meaningful causal inference.

### Test Suite: ~15 Smoke Tests, Not 500

The `tests/test_smoke.py` file contains approximately **15 smoke tests** that verify directory structure and file existence (e.g., "does `backend/` exist?", "does `README.md` have content?"). These are tautological structural checks, not functional tests of business logic. Claims of "500+ test cases" refer to the theoretical parameterized coverage space of the NL keyword parser, not to distinct test methods that actually exercise behavior.

### Frontend NL Page

A frontend NL input page has been added at `/nl` route. It includes a visible disclaimer banner and demonstrates the NL command execution and causal check APIs. It is a UI prototype for concept demonstration only.

---

## References

### Causal DAGs and Causal Inference

1. Pearl, J. (2009). *Causality: Models, Reasoning, and Inference* (2nd ed.). Cambridge University Press. -- The foundational text on causal directed acyclic graphs (DAGs) and do-calculus.
2. Spirtes, P., Glymour, C., & Scheines, R. (2000). *Causation, Prediction, and Search* (2nd ed.). MIT Press. -- Algorithms for causal structure learning from observational data.
3. Peters, J., Janzing, D., & Scholkopf, B. (2017). *Elements of Causal Inference: Foundations and Learning Algorithms*. MIT Press. -- Modern treatment of causal inference with machine learning.

### Natural Language Interfaces to Databases

4. Li, F., & Jagadish, H. V. (2014). Constructing an Interactive Natural Language Interface for Relational Databases. *Proceedings of the VLDB Endowment*, 8(1), 73-84. -- NLIDB system design using semantic parsing.
5. Yaghmazadeh, N., Wang, Y., Dillig, I., & Dillig, T. (2017). SQLizer: Query Synthesis from Natural Language. *Proceedings of OOPSLA*, Article 63. -- Automatic SQL generation from natural language.
6. Kamath, A., & Das, R. (2019). A Survey on Semantic Parsing. *arXiv:1812.00978*. -- Comprehensive survey of semantic parsing approaches for NL-to-formal-language translation.

### RBAC and Access Control

7. Ferraiolo, D. F., Sandhu, R., Gavrila, S., Kuhn, D. R., & Chandramouli, R. (2001). Proposed NIST Standard for Role-Based Access Control. *ACM Transactions on Information and System Security*, 4(3), 224-274. -- The NIST RBAC standard that this system implements.

---

## License

MIT -- free to use, modify, and distribute with attribution.

---

## Contact

For questions or issues, please open an issue on the repository.
