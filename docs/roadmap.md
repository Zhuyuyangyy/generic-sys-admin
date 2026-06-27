# ERMS Product Roadmap

## v0.1 — Clean Baseline (Current)

**Status:** Released
**Focus:** Repository cleanup, security hardening, one-click deployment

### Deliverables

- [x] Repository cleanup and product baseline
  - Remove non-product code from main source tree
  - Archive experimental and academic materials to `archive/`
  - Establish clean project structure
- [x] Security configuration
  - All secrets externalized to environment variables (no hardcoded values)
  - JWT authentication with configurable expiration
  - BCrypt password hashing
  - `.env.example` template for deployment
- [x] Docker Compose one-click deployment
  - MySQL 8.0 + Redis 7 + Spring Boot + Vue 3 + Nginx
  - Health checks for all services
  - Optional MinIO storage profile
  - Named volumes for data persistence
- [x] Product-level README and documentation
  - Tech stack overview
  - Quick start guide
  - Default accounts with security warnings
  - Environment variables reference

### Acceptance Criteria

- `docker compose up -d --build` starts all services within 3 minutes
- `curl http://localhost:8080/actuator/health` returns healthy
- Frontend accessible at `http://localhost`
- No hardcoded secrets in source code (verified by grep)
- Login with admin/123456 succeeds and returns JWT token

---

## v0.2 — Enterprise Backend

**Status:** Planned
**Focus:** Domain-driven architecture, RBAC completion, audit query API

### Deliverables

- [ ] Domain-driven module refactoring
  - Complete migration from flat package structure to domain modules
  - Each module (iam, asset, inventory, audit, nl, workflow) fully self-contained
  - Remove duplicate code between legacy and new packages
  - Ensure all controllers, services, mappers, and models live in domain packages
- [ ] RBAC completion
  - Full Role CRUD with permission assignment UI
  - Menu CRUD (create, update, delete, tree management)
  - `@PreAuthorize` annotations on all protected endpoints
  - Custom SecurityChecker (`@ss`) bean for permission evaluation
  - Button-level permission granularity on frontend
- [ ] Audit log query API
  - Paginated log query with multi-condition filtering (module, operation, operator, date range, status)
  - Log detail view
  - Log export to Excel
  - Audit log retention policy configuration
- [ ] Flyway database migrations
  - All schema changes managed through Flyway versioned migrations
  - V1–V5 already in place; ensure all future changes use migration scripts
  - Migration rollback strategy

### Acceptance Criteria

- All legacy flat packages migrated or removed
- Every protected endpoint has `@PreAuthorize` annotation
- Audit log query returns paginated results with filters
- `mvn flyway:info` shows all migrations applied
- Zero compilation warnings related to package moves

---

## v0.3 — Business Closed Loop

**Status:** Planned
**Focus:** Complete business workflows for equipment and consumable lifecycles

### Deliverables

- [ ] Equipment lifecycle
  - Equipment CRUD with status state machine (normal → maintenance → normal/scrapped)
  - Location management (hierarchical tree: building → floor → room)
  - Maintenance plan scheduling and tracking
  - Maintenance reminder notifications
  - Equipment health overview and scoring dashboard
- [ ] Consumable inventory closed loop
  - Full inbound/outbound workflow with approval
  - Supplier management CRUD
  - Batch tracking and expiration date management
  - Low-stock automatic alert generation
  - Stock alert acknowledgment workflow
  - Inventory check (stocktaking) functionality
- [ ] Lightweight approval workflow
  - Workflow definition CRUD (configurable steps in JSON)
  - Workflow instance creation and tracking
  - Task assignment and approval/rejection actions
  - Status tracking: PENDING → IN_PROGRESS → COMPLETED/CANCELLED
  - Integration with equipment scrapping and consumable procurement

### Acceptance Criteria

- Equipment can be registered, moved to maintenance, and scrapped with full audit trail
- Consumable inbound/outbound creates transaction records and updates stock
- Low-stock alert triggers when stock drops below `min_stock_level`
- Workflow instance can be created, approved through steps, and completed
- All business operations logged in audit trail

---

## v0.4 — NL Copilot

**Status:** Planned
**Focus:** Natural language command execution with safety guardrails

### Deliverables

- [ ] NL Dry Run with risk assessment
  - Parse NL input → assess risk level (LOW/MEDIUM/HIGH/CRITICAL)
  - Return preview of affected entities, tables, and expected changes
  - Generate confirmationId for single-use execution authorization
  - Bulk operation detection and risk elevation
- [ ] Confirmation execution protocol
  - Validate confirmationId before execution
  - Verify intent consistency between dry-run and execution
  - Single-use confirmation IDs (consumed on execution)
  - Confirmation expiration after timeout
- [ ] NL command test suite
  - Standard test command set (10+ commands covering all intents and entity types)
  - Automated accuracy testing framework
  - Intent detection accuracy metrics
  - Entity extraction accuracy metrics
- [ ] Causal impact analysis enhancement
  - Causal DAG with configurable node relationships
  - Topological propagation with decay factor
  - High-impact flagging for risky operations
  - Impact visualization on frontend

### Acceptance Criteria

- `POST /api/nl/dry-run` returns risk assessment without side effects
- `POST /api/nl/execute-confirmed` only executes with valid confirmationId
- Invalid or expired confirmationId returns error
- Intent mismatch between dry-run and execute returns error
- All 10 test commands parse correctly
- DELETE/UPDATE intents trigger causal check; QUERY intents do not

---

## v0.5 — Product Frontend

**Status:** Planned
**Focus:** Enterprise-grade operations console with real-time capabilities

### Deliverables

- [ ] Enterprise operations console
  - Consistent design system using Element Plus + custom gradient components
  - Responsive layout with sidebar navigation
  - Breadcrumb navigation and tab-based page management
  - Loading states and error handling
- [ ] Asset center
  - Equipment list with status badges and quick actions
  - Equipment detail page with maintenance history timeline
  - Location tree browser
  - Maintenance plan calendar view
  - Asset health dashboard with ECharts visualizations
- [ ] Inventory center
  - Consumable list with stock level indicators
  - Inbound/outbound operation forms with validation
  - Stock alert list with acknowledgment actions
  - Supplier directory
  - Inventory summary charts
- [ ] Workflow center
  - Workflow definition management
  - My tasks list with approve/reject actions
  - Workflow instance timeline view
- [ ] Real-time dashboard with WebSocket
  - Equipment status distribution pie chart
  - Inventory risk indicators
  - Operation log activity feed
  - System health metrics

### Acceptance Criteria

- All CRUD operations have corresponding frontend pages
- Dashboard updates in real-time via WebSocket
- Form validation prevents invalid data submission
- Mobile-responsive layout works on 1024px+ screens
- Page load time < 3 seconds on first visit

---

## v0.6 — Observability & Deployment

**Status:** Planned
**Focus:** Production monitoring, deployment automation, security hardening

### Deliverables

- [ ] Prometheus + Grafana monitoring
  - Spring Boot Actuator metrics exposure
  - Custom business metrics (equipment count, stock levels, operation rate)
  - Grafana dashboards for system and business metrics
  - Alert rules for critical conditions (high error rate, low stock, disk usage)
- [ ] Production deployment scripts
  - Automated deployment pipeline (CI/CD)
  - Blue-green deployment strategy
  - Database migration automation
  - Configuration management across environments
- [ ] Security hardening
  - CORS origin restriction (remove wildcard)
  - Rate limiting on login and registration endpoints
  - WebSocket authentication
  - AI endpoint authentication requirement
  - Security headers (CSP, X-Frame-Options, etc.)
  - TLS/HTTPS configuration

### Acceptance Criteria

- Grafana dashboards display real-time metrics
- Alerts trigger when stock drops below threshold
- Deployment can be completed with a single CI/CD pipeline
- CORS only allows configured origins
- Login rate limiting prevents brute-force attacks
- All endpoints (except login/register) require authentication

---

## v0.7 — Intelligence

**Status:** Planned
**Focus:** AI-driven predictive capabilities and anomaly detection

### Deliverables

- [ ] Predictive restock
  - Consumption pattern analysis from inventory_transaction history
  - Automatic reorder point adjustment based on usage trends
  - Restock recommendation dashboard
- [ ] Equipment health scoring
  - Health score calculation based on maintenance history, age, and usage
  - Predictive maintenance scheduling based on health trends
  - Health degradation alerts
- [ ] Audit anomaly detection
  - Statistical analysis of operation patterns
  - Anomaly scoring for unusual operations (off-hours, bulk deletes, privilege escalation)
  - Anomaly event dashboard with drill-down
  - Notification on high-severity anomalies

### Acceptance Criteria

- Restock recommendations generated weekly based on consumption data
- Equipment health scores update after each maintenance record
- Anomaly detection flags operations with >2σ deviation from baseline
- False positive rate < 10% for anomaly alerts
- All predictions logged with confidence scores

---

## v0.8+ — Multi-Tenant

**Status:** Future
**Focus:** SaaS-ready multi-tenant isolation and ABAC

### Deliverables

- [ ] Tenant isolation
  - Schema-level or row-level tenant isolation
  - Tenant-aware data access layer (automatic tenant_id filtering)
  - Tenant provisioning and management API
  - Per-tenant configuration (features, limits, branding)
- [ ] Data scope permissions (ABAC)
  - Attribute-based access control extending RBAC
  - Data scope rules: department, region, project-level visibility
  - Configurable data scope per role
  - Dynamic query filtering based on data scope
- [ ] SaaS management console
  - Tenant onboarding and lifecycle management
  - Usage metrics and billing integration
  - Feature flag management per tenant
  - Tenant admin self-service portal

### Acceptance Criteria

- Tenants cannot access each other's data
- Tenant provisioning completes in < 5 minutes
- Data scope rules apply transparently to all queries
- Tenant admin can manage own users, roles, and menus
- Cross-tenant data leakage test passes 100%

---

## Version Summary

| Version | Name | Status | Key Theme |
|---------|------|--------|-----------|
| v0.1 | Clean Baseline | **Current** | Security, Docker, Documentation |
| v0.2 | Enterprise Backend | Planned | DDD, RBAC, Audit API |
| v0.3 | Business Closed Loop | Planned | Equipment lifecycle, Inventory, Workflow |
| v0.4 | NL Copilot | Planned | NL safety, Dry Run, Causal analysis |
| v0.5 | Product Frontend | Planned | Operations console, Dashboard, WebSocket |
| v0.6 | Observability | Planned | Monitoring, CI/CD, Security hardening |
| v0.7 | Intelligence | Planned | Predictive restock, Health scoring, Anomaly |
| v0.8+ | Multi-Tenant | Future | Tenant isolation, ABAC, SaaS |
