# ERMS Deployment Guide

## Docker Compose Deployment (Recommended)

### Prerequisites

- Docker Engine 20.10+
- Docker Compose V2+
- Minimum 4 GB RAM, 2 CPU cores
- 20 GB disk space

### Quick Start

```bash
git clone https://github.com/Zhuyuyangyy/generic-sys-admin.git
cd generic-sys-admin
cp .env.example .env
docker compose up -d --build
```

Wait for all services to report healthy (~2 minutes on first build):

```bash
# Verify backend health
curl http://localhost:8080/actuator/health

# Access frontend
open http://localhost
```

### With MinIO Object Storage

```bash
docker compose --profile storage up -d --build
```

This starts the optional MinIO container alongside the core services.

## Service Architecture

| Service | Container | Port | Dependencies |
|---------|-----------|------|-------------|
| MySQL 8.0 | erms-mysql | 3306 | — |
| Redis 7 | erms-redis | 6379 | — |
| Spring Boot Backend | erms-backend | 8080 | mysql, redis |
| Vue 3 Frontend | erms-frontend | 80 | backend |
| MinIO (optional) | erms-minio | 9000, 9001 | — |

All services communicate over the `erms-network` bridge network.

## Environment Variables

All sensitive configuration is managed through environment variables. Copy `.env.example` to `.env` and customize.

### Database

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | MySQL host | `mysql` (container) / `localhost` (local) |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database name | `generic_sys_admin` |
| `DB_USERNAME` | Application DB user | `admin` |
| `DB_PASSWORD` | Application DB password | (must set) |
| `DB_ROOT_PASSWORD` | MySQL root password | (must set) |

### Redis

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_HOST` | Redis host | `redis` (container) / `localhost` (local) |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | (empty = no auth) |

### JWT Authentication

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | JWT signing key | **MUST override in production** |
| `JWT_EXPIRATION` | Access token validity (seconds) | `7200` (2 hours) |
| `JWT_REFRESH_EXPIRATION` | Refresh token validity (seconds) | `604800` (7 days) |

### MinIO (Optional)

| Variable | Description | Default |
|----------|-------------|---------|
| `MINIO_ROOT_USER` | MinIO admin user | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | MinIO admin password | `minioadmin` |
| `MINIO_PORT` | MinIO API port | `9000` |
| `MINIO_CONSOLE_PORT` | MinIO Console port | `9001` |
| `MINIO_ENDPOINT` | MinIO endpoint URL | `http://minio:9000` |

### Minimax AI (Optional)

| Variable | Description | Default |
|----------|-------------|---------|
| `MINIMAX_APP_ID` | MiniMax application ID | (empty) |
| `MINIMAX_API_KEY` | MiniMax API key | (empty) |
| `MINIMAX_API_URL` | MiniMax API base URL | `https://api.minimax.chat` |
| `MINIMAX_GROUP_ID` | MiniMax group ID | (empty) |

### Server

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Backend HTTP port | `8080` |
| `SPRING_PROFILES` | Spring profile | `prod` |
| `DEMO_MODE` | Demo mode toggle | `false` |
| `FRONTEND_PORT` | Frontend exposed port | `80` |

### Storage & TTS

| Variable | Description | Default |
|----------|-------------|---------|
| `STORAGE_PROVIDER` | Storage strategy | `auto` (MinIO if available, else local) |
| `TTS_PROVIDER` | TTS provider | (empty = mock) |

## Local Development

### Backend

```bash
cd backend
# Ensure MySQL 8.0 and Redis 7 are running locally
mvn spring-boot:run
# or with custom profile:
# mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend starts at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
# Dev server at http://localhost:5173
# API requests proxied to http://localhost:8080
```

### Using Docker for Infrastructure Only

```bash
# Start only MySQL and Redis
docker compose up -d mysql redis

# Run backend locally
cd backend && mvn spring-boot:run

# Run frontend locally
cd frontend && npm run dev
```

## Production Checklist

### Security

- [ ] Change all default passwords (`DB_PASSWORD`, `DB_ROOT_PASSWORD`, `REDIS_PASSWORD`)
- [ ] Generate a strong `JWT_SECRET`: `openssl rand -base64 48`
- [ ] Set `SPRING_PROFILES=prod`
- [ ] Disable demo mode: `DEMO_MODE=false`
- [ ] Restrict MySQL and Redis port exposure (remove port mappings in compose for internal-only)
- [ ] Configure firewall rules (only expose ports 80/443)
- [ ] Set MinIO credentials to strong values if using storage profile
- [ ] Review CORS configuration in `WebSecurityConfig` (restrict `allowedOriginPatterns`)

### Reliability

- [ ] Configure MySQL `innodb_buffer_pool_size` appropriately (default 256M in compose)
- [ ] Set Redis `maxmemory` policy if needed
- [ ] Verify health checks are passing for all services
- [ ] Set up log rotation for Docker containers
- [ ] Configure appropriate `max_connections` for MySQL (default 500 in compose)

### Monitoring

- [ ] Verify `/actuator/health` endpoint is accessible
- [ ] Set up container restart policies (`unless-stopped` is default)
- [ ] Plan for Prometheus + Grafana integration (v0.6)

## Backup and Recovery

### MySQL Backup

```bash
# Create a full backup
docker exec erms-mysql mysqldump -u root -p${DB_ROOT_PASSWORD} \
  --single-transaction --routines --triggers \
  generic_sys_admin > backup_$(date +%Y%m%d_%H%M%S).sql

# Or using docker compose
docker compose exec mysql mysqldump -u root -p${DB_ROOT_PASSWORD} \
  generic_sys_admin > backup.sql
```

### MySQL Recovery

```bash
# Restore from backup
docker exec -i erms-mysql mysql -u root -p${DB_ROOT_PASSWORD} \
  generic_sys_admin < backup_20240615_120000.sql
```

### Redis Backup

Redis uses AOF persistence (`appendonly yes`) configured in the compose file. Data is stored in the `redis_data` volume.

```bash
# Trigger a manual BGSAVE
docker exec erms-redis redis-cli -a ${REDIS_PASSWORD} BGSAVE

# Copy the dump file
docker cp erms-redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

### Volume Backup

All persistent data is stored in Docker named volumes:

| Volume | Service | Content |
|--------|---------|---------|
| `mysql_data` | MySQL | Database files |
| `redis_data` | Redis | AOF/RDB persistence |
| `minio_data` | MinIO | Uploaded files |

```bash
# List volumes
docker volume ls | grep erms

# Inspect volume location
docker volume inspect erms_mysql_data

# Backup a volume
docker run --rm -v erms_mysql_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/mysql_data_backup.tar.gz -C /data .
```

### Full System Recovery

```bash
# 1. Stop all services
docker compose down

# 2. Restore volumes from backup
docker run --rm -v erms_mysql_data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/mysql_data_backup.tar.gz -C /data

# 3. Restart services
docker compose up -d

# 4. Verify health
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### Backend fails to start

```bash
# Check backend logs
docker compose logs backend

# Common issues:
# - MySQL not ready: wait for healthcheck (30s start_period)
# - Redis not reachable: verify REDIS_HOST and REDIS_PASSWORD
# - JWT_SECRET too short: ensure at least 32 characters
```

### MySQL connection refused

```bash
# Check MySQL health
docker compose exec mysql mysqladmin ping -h localhost -u root -p

# Verify database exists
docker compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

### Frontend shows blank page

```bash
# Check Nginx logs
docker compose logs frontend

# Verify backend is reachable from frontend container
docker compose exec frontend curl -s http://backend:8080/actuator/health
```
