# ERMS - Enterprise Resource Management System

Enterprise resource operations platform for asset lifecycle management, consumable inventory, RBAC access control, audit compliance, and natural language operations.

## Core Capabilities

| Module | Description |
|--------|-------------|
| Asset Management | Equipment registration, status tracking, maintenance scheduling |
| Consumable Inventory | Stock tracking, inbound/outbound records, low-stock alerts |
| RBAC Access Control | Role-based permissions, menu-level and button-level granularity, JWT authentication |
| Audit Trail | AOP-based operation logging, full traceability for all write operations |
| Natural Language Operations | NL-driven business commands with intent parsing and causal impact analysis |
| Real-time Dashboard | WebSocket push notifications, asset health metrics, inventory risk indicators |
| File Management | Local and MinIO object storage with upload/download |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, MyBatis-Plus, Spring Security, JWT, Redis, AOP |
| Frontend | Vue 3, TypeScript, Element Plus, Pinia, ECharts |
| Database | MySQL 8.0, Redis 7 |
| Storage | MinIO (optional), Local filesystem |
| Deployment | Docker Compose, Nginx |
| API Docs | Knife4j / OpenAPI 3.0 |

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Git

### One-Click Launch

```bash
git clone https://github.com/Zhuyuyangyy/generic-sys-admin.git
cd generic-sys-admin
cp .env.example .env
docker compose up -d --build
```

Wait for all services to be healthy (~2 minutes on first build):

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Access frontend
open http://localhost
```

### With MinIO Object Storage

```bash
docker compose --profile storage up -d --build
```

## Default Accounts

| Role | Username | Password |
|------|----------|----------|
| Administrator | admin | 123456 |

> **IMPORTANT**: Change default passwords before production deployment. Set `JWT_SECRET` to a strong random value.

## API Documentation

Once the backend is running, access Swagger/Knife4j docs at:

```
http://localhost:8080/doc.html
```

## Project Structure

```
generic-sys-admin/
├── backend/                    # Spring Boot backend
│   └── src/main/java/com/zyy/
│       ├── common/             # Unified response, exception handling
│       ├── config/             # Application configuration
│       ├── controller/         # REST API controllers
│       ├── security/           # JWT authentication filter
│       ├── aspect/             # AOP operation logging
│       ├── rbac/               # RBAC models
│       ├── nl/                 # Natural language service
│       ├── voice/              # TTS service (optional)
│       ├── service/            # Business logic
│       ├── mapper/             # Data access
│       └── model/              # DTOs, entities, VOs
├── frontend/                   # Vue 3 frontend
│   └── src/
│       ├── api/                # API client modules
│       ├── views/              # Page views
│       ├── components/         # UI components
│       ├── layout/             # App layout
│       ├── router/             # Vue Router
│       ├── store/              # Pinia store
│       └── utils/              # Utilities
├── sql/                        # Database init scripts
├── archive/                    # Archived non-product materials
├── docs/                       # Documentation
├── docker-compose.yml          # Service orchestration
├── .env.example                # Environment template
└── README.md
```

## Database

The initial schema is applied automatically via Docker entrypoint:

```
sql/v1.0__init.sql
```

Core tables:

| Table | Description |
|-------|-------------|
| `sys_user` | User accounts |
| `sys_role` | Roles |
| `sys_user_role` | User-role mapping |
| `sys_menu` | Menu and permission resources |
| `sys_role_menu` | Role-menu mapping |
| `sys_operation_log` | Operation audit log |
| `equipment` | Equipment assets |
| `consumable` | Consumable items |
| `inventory_record` | Stock movement records |
| `inventory_transaction` | Inventory transactions |

## Local Development

### Backend

```bash
cd backend
# Ensure MySQL and Redis are running locally
# Configure application.yml or set environment variables
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Dev server runs at http://localhost:5173
# API requests are proxied to http://localhost:8080
```

### Test

```bash
# Backend unit tests
cd backend && mvn test

# Frontend build check
cd frontend && npm run build
```

## Production Deployment

1. Generate a secure JWT secret:
   ```bash
   openssl rand -base64 48
   ```

2. Update `.env` with production values:
   - Set strong `DB_PASSWORD` and `DB_ROOT_PASSWORD`
   - Set the generated `JWT_SECRET`
   - Set `SPRING_PROFILES=prod`
   - Configure `MINIMAX_*` keys if using AI features

3. Deploy:
   ```bash
   docker compose up -d --build
   ```

4. Verify:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Environment Variables

All sensitive configuration is managed through environment variables. See `.env.example` for the complete list.

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | MySQL host | `mysql` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database name | `generic_sys_admin` |
| `DB_USERNAME` | Database user | `admin` |
| `DB_PASSWORD` | Database password | (must set) |
| `REDIS_HOST` | Redis host | `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | JWT signing key | (must override) |
| `JWT_EXPIRATION` | Token expiry (seconds) | `7200` |
| `MINIMAX_API_KEY` | Minimax AI API key | (empty) |

## Roadmap

### v0.1 - Clean Baseline (current)
- [x] Repository cleanup and product baseline
- [x] Security configuration (env vars, no hardcoded secrets)
- [x] Docker Compose one-click deployment
- [x] Product-level README and documentation

### v0.2 - Enterprise Backend
- [ ] Domain-driven module refactoring
- [ ] RBAC completion (role/menu CRUD, permission annotations)
- [ ] Audit log query API
- [ ] Flyway database migrations

### v0.3 - Business Closed Loop
- [ ] Equipment lifecycle (maintenance, inspection, location, health)
- [ ] Consumable inventory closed loop (suppliers, batches, stock alerts)
- [ ] Lightweight approval workflow

### v0.4 - NL Copilot
- [ ] NL Dry Run with risk assessment
- [ ] Confirmation execution protocol
- [ ] NL command test suite

### v0.5 - Product Frontend
- [ ] Enterprise operations console
- [ ] Asset center, inventory center, workflow center
- [ ] Real-time dashboard with WebSocket

### v0.6 - Observability & Deployment
- [ ] Prometheus + Grafana monitoring
- [ ] Production deployment scripts
- [ ] Security hardening

### v0.7 - Intelligence
- [ ] Predictive restock
- [ ] Equipment health scoring
- [ ] Audit anomaly detection

### v0.8+ - Multi-tenant
- [ ] Tenant isolation
- [ ] Data scope permissions (ABAC)
- [ ] SaaS management console

## Documentation

Full documentation is available in the `docs/` directory:

- [Architecture](docs/architecture.md)
- [Product Specification](docs/product-spec.md)
- [API Contract](docs/api-contract.md)
- [Database Schema](docs/database.md)
- [Deployment Guide](docs/deployment.md)
- [Security Policy](docs/security.md)
- [Roadmap](docs/roadmap.md)

## Limitations

- Default passwords must be changed before production use
- WebSocket event center is in-memory (no persistence across restarts)
- NL operations require confirmation for write commands
- AI features (TTS, image generation) are optional and not part of the core platform

## License

This project is licensed under the MIT License.
