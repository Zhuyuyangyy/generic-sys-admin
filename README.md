# Generic Sys Admin — ERMS

> Enterprise resource management with a **guarded natural-language execution
> path**: intent parsing → command whitelist → permission check → typed
> executor → audit. Spring Boot 3.4 + Vue 3.

---

## Overview

A full-stack enterprise resource management system covering equipment
lifecycle, consumable inventory, inspection records, and RBAC access control.

The natural-language interface is deliberately **not** a free-form "AI agent":
user text is parsed into an intent, resolved against a fixed command table,
checked against the caller's granted authorities, and only then executed.
Everything that path can do is enumerated in one place
(`NLExecutor.ALLOWED`), and every write operation is recorded with the
authenticated operator — not a placeholder.

### What this project is (and is not)

| | |
|---|---|
| **Is** | A resource-management backend with a guarded NL orchestration layer |
| **Is** | A reference for "NL in → whitelisted, permission-checked command out" |
| **Is not** | An autonomous agent — it cannot issue commands outside the whitelist |
| **Is not** | A multi-tenant SaaS platform (no `tenant_id` isolation) |
| **Is not** | A forecasting engine (no predictive restock module) |

---

## Architecture

```
Vue 3 + TypeScript (Element Plus, Pinia, Axios)
        │  REST (JSON) + WebSocket
        ▼
Spring Boot 3.4  ·  Spring Security 6  ·  MyBatis-Plus 3.5
        │
        ├── JwtAuthFilter ──► SecurityContext (LoginUser + roles + permissions)
        │        │
        │        └── SecurityUtils.currentUserId()   ← every operator id
        │
        ├── URL rules  (WebSecurityConfig)   ← method-scoped public endpoints
        ├── Method rules (@PreAuthorize/@ss) ← per-endpoint authorities
        │
        ├── NL path
        │      HybridNLParser → NLRuleEngine → NLExecutor (whitelist)
        │                                       → Equipment/Consumable/Inventory service
        │
        ├── CausalDAGService → impact propagation for DELETE/UPDATE
        ├── OperationLogAspect → full-chain audit (user, params, IP, duration)
        └── MinioUtil → MinIO, falling back to local filesystem
                     ▼
              MySQL 8 · Redis · MinIO
```

---

## Tech Stack

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.4.0, Spring Security 6.4 |
| Persistence | MyBatis-Plus 3.5.5, MySQL 8, HikariCP |
| Auth | JJWT 0.12.3 (access + refresh), BCrypt |
| Cache / session | Redis (Lettuce) |
| Docs | springdoc-openapi + Knife4j |
| Frontend | Vue 3, TypeScript, Vite, Element Plus, Pinia, ECharts |
| Build / CI | Maven, GitHub Actions (`ci.yml`) |

---

## Quick Start

### Prerequisites

- JDK 17 (see "Build note" below — Lombok requires ≥ 1.18.42 on JDK 21+)
- Maven 3.9+, Node 20+, MySQL 8, Redis

### 1. Configure

```bash
cp .env.example .env
# set DB_PASSWORD, JWT_SECRET (>= 32 chars), REDIS_PASSWORD
```

### 2. Database

The application runs **Flyway** on startup. Create an empty database and let
Flyway apply the chain from `classpath:db/migration`:

```bash
mysql -u root -p -e "CREATE DATABASE generic_sys_admin \
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

cd backend && mvn spring-boot:run        # Flyway migrates automatically
```

To apply the migrations by hand instead, read the same files Flyway uses:

```bash
mysql -u root -p generic_sys_admin < backend/src/main/resources/db/migration/V1.0__init.sql
mysql -u root -p generic_sys_admin < backend/src/main/resources/db/migration/V1.1__operation_log.sql
mysql -u root -p generic_sys_admin < backend/src/main/resources/db/migration/V1.2__rbac_schema_completion.sql
```

The migration scripts deliberately contain **no** `CREATE DATABASE` / `USE` —
those redirect every table into a hardcoded database instead of the one you
pass. Creating the database is the caller's job.

`v1.2` adds the `permission` / `menu_type` / `is_external` columns to `sys_menu`
and seeds the button-level permissions that `@PreAuthorize` checks. **Skip it
and every admin request returns 403** — `SysMenuEntity` maps those columns and
`SysMenuMapper` uses `SELECT m.*`.

Migrations live at the Flyway standard location,
`backend/src/main/resources/db/migration/`, and are the only tracked copy —
there is no build-time copy step to drift.

The chain is verified automatically by `MigrationIT` against a real MySQL 8.0.44
container; see `docs/database/migration-audit.md`.

The chain has been executed end-to-end against MySQL 8.0.44; see
`docs/database/migration-audit.md` for the recorded output.

### 3. Run

```bash
# backend
cd backend && mvn spring-boot:run

# frontend
cd frontend && npm install && npm run dev
```

Swagger UI: <http://localhost:8081/swagger-ui.html>

### Build note (JDK 21+)

Lombok's annotation processor is silently skipped on JDK 21+, which makes every
`@Data` / `@Slf4j` / `@Builder` accessor vanish and the build fail with
"cannot find symbol". The fix is already in `backend/pom.xml`: a Lombok version
of ≥ 1.18.42 **and** an explicit `annotationProcessorPaths` entry. If you bump
`lombok.version`, keep the dependency referencing `${lombok.version}` — a
hardcoded version silently overrides the property.

---

## Natural Language Execution Path

```
"删除设备 EQ-2024-0001"
        │
        ▼  HybridNLParser (rule engine, LLM fallback when a key is set)
   intent = DELETE, entityType = EQUIPMENT
        │
        ▼  NLRuleEngine.resolveService()
   "deleteEquipment"
        │
        ▼  NLExecutor.describe() ─── not in whitelist? ──► refuse, nothing runs
        │                         permission = equipment:del
        ▼
   EquipmentService.delete(id, operatorId)   ← operatorId from SecurityUtils
        │
        ▼  OperationLogAspect
   audit record (who, what, params, IP, duration)
```

`NLExecutor` holds the single authoritative table. It resolves commands by
**exact name** against that table (no `startsWith` matching, no reflection on
caller-supplied names), and parses entity ids as strictly
`<PREFIX>-<digits>` — a malformed id is rejected rather than forwarded.

For `DELETE` / `UPDATE`, `CausalDAGService` first propagates impact through the
dependency DAG so the caller sees the blast radius before the write lands.

---

## RBAC Model

`user → user_role → role → role_menu → menu(permission)`.

Authorization is enforced in two layers:

1. **URL layer** (`WebSecurityConfig`) — only `POST /api/users/login`,
   `POST /api/users/refresh-token`, the Swagger paths, and
   `/actuator/health|info` are public. Everything else requires authentication.
2. **Method layer** (`@PreAuthorize("@ss.hasAuthority('...'))`) — per-endpoint
   authorities, e.g. `system:user:del` for user deletion.

`@PreAuthorize` is deliberately spelled `@ss.hasAuthority(...)` rather than
`hasAuthority(...)`: `SecurityChecker` reads directly from the
`SecurityContext`, which sidesteps the SpEL principal-field restrictions in
Spring Security 6.

Note on `application.yml`: `management.endpoints.web.exposure.include` is
`health,info`. Other actuator endpoints are not exposed at all.

---

## Functional Modules

| Module | Path | Highlights |
|---|---|---|
| Equipment | `/api/equipment` | Lifecycle, status state machine, maintenance scheduling |
| Consumables | `/api/consumables` | Inbound / outbound / stock adjustment, transactions |
| Inspection records | `/api/inventory-records` | Maintenance history per equipment |
| Users | `/api/users` | Login, refresh, profile, lock/unlock, soft delete |
| Roles / menus | `/api/roles`, `/api/menus` | Role-menu grant tree |
| NL orchestration | `/api/nl` | `execute`, `execute-with-causal-check`, `causal/dag` |
| Operation logs | `/api/logs` | AOP-driven audit trail |
| Dashboard / AI / voice | `/api/dashboard`, `/api/ai`, `/api/tts` | Stats, MiniMax multimodal, TTS |

---

## Security Notes

Implemented and regression-tested:

- **Operator identity** always comes from `SecurityUtils.currentUserId()`,
  which reads `SecurityContext` and throws when there is no authenticated
  principal. No endpoint falls back to a hardcoded user id.
- **Method-scoped public rules** — the `/api/users` resource as a whole is not
  anonymous; only the two auth endpoints are.
- **Per-endpoint authorities** on every write endpoint across 11 controllers;
  `AuthoritySeedConsistencyTest` fails the build if any of them is missing from
  the SQL seed or `PermConst`.
- **Per-command authorities inside the NL path** — `nl:execute` alone does not
  let a caller delete equipment; each of the 16 registered commands enforces the
  permission it declares.
- **Command whitelist** for the NL path, plus strict entity-id parsing.
- **Fail-fast secrets** — `JWT_SECRET` has no default outside the dev profile,
  and known placeholder values are rejected at startup.
- **Actuator** limited to `health,info`.
- **Gitleaks** in CI with full-history checkout.

Not implemented (stated plainly):

- **Multi-tenancy** — no `tenant_id` column, no tenant isolation.
- **ABAC / data-scope filtering** — authorization is purely RBAC.
- **Idempotency keys** on write endpoints — the stock operations are protected
  (`Idempotency-Key` header on inbound / outbound / adjust), but no other
  endpoint accepts one. See `docs/audit/write-side-effects.md` for why the rest
  do not need it.
- **NL-path audit detail** — `POST /api/nl/execute` is logged, but the parsed
  intent, resolved command, and target ID are not captured.
- **Rate limiting / brute-force lockout** beyond the login attempt counter.
- **Parameter validation runs before authorization.** `@PreAuthorize` is method
  security, so it executes *after* argument binding — an unauthenticated caller
  gets a 400 with field names before the 403. Not a data leak, but it means the
  authorization gate is not the first thing a request meets.

## Audit documents

| Document | Contents |
|---|---|
| `docs/audit/current-baseline.md` | Measured file counts, test baseline, module structure, CI, migration chain |
| `docs/audit/write-side-effects.md` | Every state-changing operation and whether a duplicate is dangerous |
| `docs/security/permission-matrix.md` | Every endpoint × HTTP method × required authority |
| `docs/security/nl-command-surface.md` | The commands the NL path can trigger, and what it cannot reach |
| `docs/database/migration-audit.md` | Migration chain, static schema checks, runtime status |
| `docs/database/migration-policy.md` | How to add a migration; why V1.2 must not be edited in place |

---

## Testing

```bash
cd backend && mvn verify        # 109 unit tests + failsafe IT (Testcontainers MySQL)
pytest tests/ -q                # repo layout + security regressions
```

`mvn test` runs the fast unit suite only (H2, no Docker). `mvn verify` adds the
integration phase, including `MigrationIT` against a real MySQL container —
it skips itself when no Docker daemon is reachable.

Backend highlights (`backend/src/test/java`):

| Class | Covers |
|---|---|
| `SecurityRuntimeAuthorizationTest` | MockMvc → filter chain → method security: anonymous 401 / wrong authority 403 / correct 200, across user, role, inventory and NL endpoints |
| `SecurityUtilsTest` | Current-user resolution; no fallback id; rejects non-`LoginUser` principals |
| `AuthoritySeedConsistencyTest` | Every `@ss.hasAuthority('…')` exists in the SQL seed and in `PermConst`; blocks a whole class of 403 bugs |
| `JwtUtilTest` | Token signing / parsing / validation; fail-fast on missing or placeholder secrets |
| `NLExecutorTest` | Whitelist enforcement, malformed entity ids, per-command authorization, and **proof that a refused command never touches a service** |
| `ConsumableStockInvariantTest` | Stock ≥ 0, positive quantities, and no lost update under concurrent inbounds |
| `ConsumableIdempotencyTest` | Exactly-once for stock writes: same key/same payload once, same key/different payload 409, 2 and 8 concurrent identical requests yield one mutation, failed key retryable |
| `MigrationIT` | Flyway discovers/migrates/validates V1.0–V1.3 on real MySQL; schema + permission seed asserted |
| `SmokeTest` | Application context + core bean wiring |
| `service.*` | Consumable and user service behaviour |
| `experiment.*` | NL accuracy, causal propagation, end-to-end, ablation |

`Experiment4LLMBaselineTest` requires `DEEPSEEK_API_KEY` and is skipped when it
is unset.

## Inventory invariants

`ConsumableMapper.adjustStockAtomic()` performs
`UPDATE sys_consumable SET stock_quantity = stock_quantity + #{delta}
WHERE … AND stock_quantity + #{delta} >= 0`, so:

- stock can never go negative, **even under concurrency**;
- two concurrent inbounds both land (no lost update — this used to fail and
  `ConsumableStockInvariantTest` now guards it);
- the stock update and the transaction ledger insert share one `@Transactional`.

## Project Structure

```
generic-sys-admin/
├── backend/
│   ├── Dockerfile
│   ├── src/main/java/com/zyy/
│   │   ├── config/        Security, MyBatis-Plus, MinIO, storage strategy
│   │   ├── controller/    REST endpoints (11 controllers)
│   │   ├── nl/            NLParser, HybridNLParser, LLMNLParser, NLRuleEngine,
│   │   │                  NLExecutor (whitelist), CausalDAGService, NLService
│   │   ├── security/      JwtAuthFilter, JwtUtil, LoginUser, SecurityUtils,
│   │   │                  SecurityErrorHandlers
│   │   ├── service/       + impl/
│   │   └── mapper/ model/ aspect/ exception/ voice/ websocket/
│   ├── src/main/resources/  application*.yml, logback
│   ├── src/main/resources/
│   │   ├── application*.yml, logback
│   │   └── db/migration/
│   │       ├── V1.0__init.sql            ← Flyway migrations (tracked, single copy)
│   │       ├── V1.1__operation_log.sql
│   │       └── V1.2__rbac_schema_completion.sql
│   └── src/test/java/com/zyy/
│       ├── database/MigrationIT.java            ← Testcontainers + Flyway
│       ├── nl/ security/ service/
│       └── SmokeTest, IntegrationTest, experiment/
├── frontend/
├── tests/test_smoke.py    repo layout + security regression guards
├── docs/
│   ├── audit/             current-baseline, write-side-effects
│   ├── database/          migration-audit, migration-policy
│   └── security/          permission-matrix, nl-command-surface
└── .github/workflows/ci.yml
```

## CI

`ci.yml` runs, on every push and pull request:

| Job | What it does |
|---|---|
| `secret-scan` | Gitleaks with `fetch-depth: 0` (full history) |
| `python-lint` | `ruff check .` |
| `python-test` | `pytest tests/ -x -v` |
| `backend-build` | `mvn clean compile` → `mvn verify` (unit + failsafe IT incl. Testcontainers MySQL) → `mvn package` |
| `frontend-build` | `npm ci`, `vue-tsc --noEmit`, `npm run build` |
| `docker-build` | builds both images after backend + frontend pass |

No job ends in `|| true` or `|| echo`, and no job selects a subset of tests.
CI has a Docker daemon, so `MigrationIT` actually runs there — the backend log
shows the MySQL container start, the migrations Flyway applied, and the
integration-test results. Locally without Docker it skips.

## Configuration and fail-fast

`application.yml` sets `sys.config.security.jwt-secret: ${JWT_SECRET:}` — **no
default**. Only `application-dev.yml` supplies a dev placeholder. Starting with
the `prod` profile without `JWT_SECRET` therefore fails at bean initialisation
rather than silently signing tokens with a known key
(`JwtUtil.afterPropertiesSet` also rejects a list of known placeholder values).

---

## License

No license file has been added to this repository yet. Until one is, all rights
are reserved by default — treat the code as read-only reference material.
