# Current Baseline

Measured on the working tree, not from history or prior reports.
All numbers below were produced by the commands listed; nothing is estimated.

## 1. Git

```
branch:            main (ahead of origin/main by 4 commits, not pushed)
uncommitted:       19 modified files, 98 staged deletions, 4 untracked paths
```

| Metric | Value |
|---|---|
| tracked files (HEAD) | 229 |
| tracked files (after staged deletions) | 131 |

## 2. File counts

| Kind | Count | Where |
|---|---|---|
| Java (main) | 128 | `backend/src/main/java` |
| Java (test) | 16 | `backend/src/test/java` |
| Vue components/views | 18 | `frontend/src` |
| Python | 2 | `tests/test_smoke.py`, `tests/__init__.py` |
| SQL migrations | 3 | `backend/src/main/resources/db/migration/` (see §6) |

Module packages under `com.zyy`:

```
aspect  causal  client  common  config  controller  enums  exception
mapper  model  nl  rbac  security  service  util  voice  websocket
```

Controllers (11): `AIController`, `ConsumableController`, `DashboardController`,
`EquipmentController`, `FileController`, `InventoryRecordController`,
`MenuController`, `NLController`, `OperationLogController`, `RoleController`,
`SysUserController`.

Services (9 + impls): Consumable, Dashboard, Equipment, InventoryRecord, Rbac,
SysMenu, SysOperationLog, SysRole, SysUser.

NL package: `CausalDAGService`, `HybridNLParser`, `LLMNLParser`, `NLExecutor`,
`NLIntent`, `NLParser`, `NLRuleEngine`, `NLService`.

## 3. Test baseline

| Command | Result |
|---|---|
| `mvn verify -B` (backend) | **PASS** — 95 unit tests, 0 failures, 0 errors, 1 skipped; failsafe phase adds `MigrationIT` (skipped without Docker) |
| `pytest tests/ -q` | **PASS** — 18 passed |
| `vue-tsc --noEmit` | **PASS** (exit 0, no output) |
| `npm run build` | **PASS** |

Historical note: before this round, `mvn clean compile` did not even build —
Lombok's annotation processor never ran on JDK 24, so every `@Data` / `@Slf4j`
/ `@Builder` accessor was missing. That is fixed and is the reason the suite is
green now.

## 4. Static greps (audit inputs)

| Pattern | Result |
|---|---|
| `TODO` / `FIXME` | none |
| `return 1L` | none (only string literals inside regression tests) |
| `permitAll` (main) | 4 occurrences, all in `WebSecurityConfig` |
| `@PreAuthorize` | 43 occurrences across 9 controllers |
| `DB_PASSWORD` | in `docker-compose.yml`, `.env.example`, `application*.yml` (placeholders only) |

## 5. CI (`.github/workflows/ci.yml`)

| Job | Steps |
|---|---|
| `python-lint` | `ruff check .` |
| `python-test` | `pytest tests/ -x -v` |
| `backend-build` | `mvn clean compile` → `mvn test` → `mvn package -DskipTests` |
| `frontend-build` | `npm ci` → `npx vue-tsc --noEmit` → `npm run build` |
| `docker-build` | builds both images after backend+frontend |

Already fixed this round: the backend step no longer pins
`-Dtest=SmokeTest,JwtUtilTest`, the unused `mysql` service container is gone,
and `vue-tsc` no longer ends in `\|\| echo` (which always exited 0).

## 6. Database migration chain (Flyway)

Migrations live at the Flyway standard location,
`backend/src/main/resources/db/migration/`, and are the only tracked copy. No
build-time copy step exists, so nothing can drift.

| Version | File | Purpose |
|---|---|---|
| 1.0 | `.../db/migration/v1.0__init.sql` | schema + seed |
| 1.1 | `.../db/migration/v1.1__operation_log.sql` | `sys_operation_log` |
| 1.2 | `.../db/migration/v1.2__rbac_schema_completion.sql` | `sys_menu` RBAC columns + permission seed |

`spring.flyway.locations: classpath:db/migration` in `application.yml`. Disabled
for H2 unit tests; `MigrationIT` exercises the real chain against Testcontainers.

Guards: `test_migrations_live_at_flyway_standard_location` (they must be where
Flyway reads them, not in a repo-root `sql/`), `test_no_unversioned_sql_in_flyway_dir`,
and `test_migrations_do_not_switch_database`.
## 7. Known gaps

1. `RoleController` and `MenuController` had their own `getCurrentUserId()`
   implementations, duplicated from `SecurityUtils`. **Fixed** — all controllers
   now go through the single entry point.
2. `AIController`, `FileController`, `NLController` had zero `@PreAuthorize`.
   **Fixed** this round.
3. `AuthorizationDeniedException` fell through to the generic handler and
   returned **500** instead of 403. **Fixed** with a dedicated handler; the
   anonymous case returns 401 via a `RestAuthenticationEntryPoint`.
4. `NLRuleEngine.SERVICE_MAP` mapped intents onto method names that do not exist
   on any service (`getEquipmentById`, `deleteEquipment`, …), so every NL
   dispatch would have thrown `NoSuchMethodException` and been swallowed as
   "执行失败". **Fixed** — the engine now resolves to real
   `(service, method)` pairs.
5. `sys_menu` write endpoints do not exist, so `system:menu:add/edit/del` were
   seed-only ghosts with no consumer. **Removed** rather than left as clutter or
   back-filled with unused CRUD endpoints.
6. `Parameter validation runs before @PreAuthorize` (argument binding precedes
   method security), so an unauthenticated caller can receive a 400 naming the
   fields before the 403.
7. `Experiment3EndToEndTest` / `Experiment5CaseStudyTest` / `Experiment6AblationTest`
   build their own security config with `anyRequest().permitAll()`. Intentional
   for isolated experiments, but they prove nothing about the real filter chain.
