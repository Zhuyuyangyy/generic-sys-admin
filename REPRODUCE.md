# Reproduce

How to rebuild and re-run everything in this repository from a clean checkout.

## 1. Backend

```bash
cd backend
mvn clean test          # full suite: unit, security, NL executor, experiments
mvn spring-boot:run
```

The test suite uses an in-memory H2 database (`MODE=MySQL`), so no external
MySQL, Redis, or MinIO is required to run `mvn test`. Experiment 4 (LLM
baseline comparison) is skipped unless `DEEPSEEK_API_KEY` is set.

## 2. Database

Create the database first, then apply the migrations in order:

```bash
mysql -u root -p -e "CREATE DATABASE generic_sys_admin \
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

mysql -u root -p generic_sys_admin < backend/src/main/resources/db/migration/V1.0__init.sql
mysql -u root -p generic_sys_admin < backend/src/main/resources/db/migration/V1.1__operation_log.sql
mysql -u root -p generic_sys_admin < backend/src/main/resources/db/migration/V1.2__rbac_schema_completion.sql
```

The migration scripts contain no `CREATE DATABASE` / `USE`, so the tables land
in whichever database you pass — create it yourself as above. `v1.2` is
idempotent and can be re-run.

Alternatively let the application do it: the same files sit at
`backend/src/main/resources/db/migration/`, which is where Flyway reads them on
startup (`classpath:db/migration`).

This chain was executed end-to-end against MySQL 8.0.44 in a scratch database;
`MigrationIT` now runs it automatically against a Testcontainers MySQL. See
`docs/database/migration-audit.md` for the recorded output.

## 3. Frontend

```bash
cd frontend
npm ci
npm run dev              # dev server
npm run build            # production bundle into frontend/dist
```

## 4. Docker

```bash
docker compose up --build
```

Builds the backend and frontend images defined in `backend/Dockerfile` and
`frontend/Dockerfile`.

## 5. Repository guards

```bash
pytest tests/ -q         # repo layout + security regression checks
```

These assert that one-off scripts and thesis artifacts stay out of the tree and
that the security fixes (no hardcoded operator id, method-scoped public
endpoints, actuator limited to `health,info`) are not reverted.

## Configuration

Copy `.env.example` to `.env` and set at minimum:

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | ≥ 32 characters; the app refuses the dev default in production |
| `REDIS_PASSWORD` | Redis password |

See `backend/src/main/resources/application.yml` for the full list.
