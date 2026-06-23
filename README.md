# ERMS - Enterprise Resource Management System

AI-native enterprise resource operations platform for asset lifecycle management, consumable inventory, RBAC access control, audit compliance, and natural language operations.

## Features

- **Asset Lifecycle Management** - Equipment registration, assignment, maintenance planning, inspection, repair, and retirement
- **Consumable Inventory** - Stock tracking, inbound/outbound records, low-stock alerts, batch and expiry management
- **RBAC Access Control** - Role-based permissions with menu-level and button-level granularity, JWT stateless authentication
- **Audit Trail** - AOP-based operation logging, full traceability for all write operations
- **Natural Language Operations** - NL-driven business commands with intent parsing, risk assessment, and human confirmation
- **Real-time Dashboard** - WebSocket push notifications, asset health metrics, inventory risk indicators
- **File Management** - Local and MinIO object storage with upload/download

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4, MyBatis-Plus, Spring Security, JWT, Redis |
| Frontend | Vue 3, TypeScript, Element Plus, Pinia, ECharts |
| Database | MySQL 8.0, Redis 7 |
| Storage | MinIO (optional), Local filesystem |
| Deployment | Docker Compose, Nginx |

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
| User | user | 123456 |

> **IMPORTANT**: Change default passwords before production deployment.

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
│       ├── security/           # JWT authentication
│       ├── aspect/             # AOP operation log
│       ├── rbac/               # RBAC models
│       ├── nl/                 # Natural language service
│       ├── voice/              # TTS service (optional)
│       ├── service/            # Business logic
│       ├── mapper/             # Data access
│       └── model/              # DTOs, entities, VOs
├── frontend/                   # Vue 3 frontend
│   └── src/
│       ├── api/                # API client
│       ├── views/              # Pages
│       ├── components/         # UI components
│       ├── layout/             # App layout
│       ├── router/             # Vue Router
│       ├── store/              # Pinia store
│       └── utils/              # Utilities
├── sql/                        # Database migration scripts
├── archive/                    # Archived non-product materials
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
| `DB_PASSWORD` | Database password | `admin123` |
| `REDIS_HOST` | Redis host | `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | JWT signing key | (must override) |
| `JWT_EXPIRATION` | Token expiry (seconds) | `7200` |
| `MINIMAX_API_KEY` | Minimax AI API key | (empty) |

## Roadmap

- [x] Core CRUD for equipment and consumables
- [x] JWT + RBAC authentication and authorization
- [x] AOP operation audit logging
- [x] Docker Compose deployment
- [x] Natural language business flow (NL-1)
- [x] Domain-driven module architecture (iam, asset, inventory, audit, nl, ai, file, workflow, report, dashboard, websocket)
- [x] Equipment lifecycle management (maintenance plans, locations, health scoring)
- [x] Consumable inventory closed loop (suppliers, stock alerts, inbound/outbound)
- [x] Lightweight approval workflow
- [x] NL Dry Run and risk guard (NL-3/4)
- [x] Real-time WebSocket event center
- [x] Audit anomaly detection
- [x] Report generation center
- [x] Database migration with Flyway
- [x] CI/CD pipeline
- [x] Enterprise frontend console (21 views)
- [ ] Multi-tenant support
- [ ] SSO / LDAP / OAuth2
- [ ] Prometheus + Grafana observability
- [ ] Backup and recovery
- [ ] Mobile-responsive optimization

## License

This project is licensed under the MIT License.
