# Migration Audit — V1–V10

Status: **BLOCKED — V1–V9 cannot be installed on an empty MySQL database.**

This document records what is verified, what is proven broken, and why it was
left alone. It is the evidence trail for the red CI job, not a claim of success.

## Chain

| Version | File | Status on MySQL 8.0 |
|---|---|---|
| V1 | `V1__init_schema.sql` | applies |
| V2 | `V2__equipment_and_asset_tables.sql` | applies |
| V3 | `V3__inventory_and_consumable_tables.sql` | applies |
| V4 | `V4__add_missing_columns_and_indexes.sql` | **FAILS — syntax error** |
| V5 | `V5__workflow_tables.sql` | applies (never reached in practice) |
| V6 | `V6__seed_default_data.sql` | applies (never reached in practice) |
| V7 | `V7__business_closed_loop.sql` | **FAILS — `ADD INDEX IF NOT EXISTS`** |
| V8 | `V8__observability_and_security.sql` | **FAILS — syntax error** |
| V9 | `V9__multi_tenant.sql` | **FAILS — syntax error, wrong table name** |
| V10 | `V10__fix_mysql_if_not_exists_syntax.sql` | applies, idempotent, **but unreachable** |

V1–V9 are published in `origin/master` (11139eb). Under the immutability rule
that applies to them, V10 was added instead of editing them.

## The defect

`ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — 26 occurrences across V4, V8 and V9
— and `ADD INDEX IF NOT EXISTS` — 4 across V4 and V7.

**MySQL does not support either form.** They are MariaDB / PostgreSQL syntax.
On MySQL 8.0 each is a hard syntax error:

```
ERROR 1064 (42000): You have an error in your SQL syntax ... near
'IF NOT EXISTS `failed_attempts` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ...' at line 7
```

Remote CI, run 36214323191, capturing the real failure:

```
Caused by: org.flywaydb.core.internal.command.DbMigrate$FlywayMigrateException:
    Migration V4__add_missing_columns_and_indexes.sql failed
Caused by: java.sql.SQLSyntaxErrorException: ... near 'IF NOT EXISTS `failed_attempts` ...'
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException:
    Error creating bean with name 'equipmentMapper' ...
    Error creating bean with name 'flywayInitializer' ...
```

Because Flyway aborts at V4, V5 through V10 never run, `flyway_schema_history`
stops there, and `sqlSessionFactory` cannot be created — so the whole
application context fails to load. This is not a test-only problem: **a fresh
deployment against an empty database cannot start.**

Two further defects surfaced while verifying:

- V9 references `sys_supplier`, but V1 creates `supplier`. The ALTER would fail
  with "table doesn't exist" even after the syntax is fixed.
- Any V10 statement positioned `AFTER tenant_id` fails when that column is added
  in the same migration.

## Why V10 does not fix it

V10 was written to be immutable-rule-compliant, and it is idempotent: run by
hand against a database where V1, V2, V3, V5, V6 and V7-without-indexes have
already applied, it completes and a second run is a no-op.

It cannot help a real installation, though. Flyway executes migrations in
version order and stops at the first failure. V4 fails, so V10 is never reached.
A migration can only repair what runs after it.

## Runtime verification status

```
RUNTIME_STATUS: VERIFIED BROKEN
MySQL version:  8.0.44 (local), Testcontainers mysql:8.0 (CI)
Method:         Flyway driven by Spring Boot's own spring.flyway.* config,
                not a hand-ordered `mysql < V1.sql` sequence
```

`MigrationIT` (`backend/src/test/java/com/zyy/database/MigrationIT.java`) is the
thing that proved this. It starts a `mysql:8.0` container — the same image
`docker-compose.yml` declares — and lets Spring Boot's Flyway run. It asserts
V1–V10 discovery, zero pending, `validate`, a second migrate of zero, the core
tables, the V9 tenant columns, V6 seed data, and real queries through
`SysUserMapper` / `SysRoleMapper` / `SysMenuMapper` / `EquipmentMapper` /
`ConsumableMapper`.

## What was verified by hand on MySQL 8.0.44

V1, V2, V3, V5, V6 and V7-without-the-three-indexes all apply cleanly to an
empty database. V10 then applies and is idempotent. After that, every field
these entities map is queryable:

```
sys_user:       id, username, password, real_name, email, phone, avatar_url,
                status, failed_attempts, locked_until, password_changed_at,
                department_id, tenant_id
sys_role:       id, name, code, description, status, sort_order, tenant_id, is_deleted
sys_menu:       id, permission, menu_type, is_external, tenant_id, status
equipment:      id, tenant_id, department_id, next_maintenance_date
consumable:     id, tenant_id, expiration_date
supplier:       tenant_id
```

So the schema *content* is sound; only the SQL dialect of the ALTERs is not.

## Resolution options (not executed — owner decision)

### Option A — fix V4/V8/V9 in place (the 1.7 exception)

The exception applies only if V1–V9 never formed an installable release. They
did not. But they *are* published, and they may already have been applied by hand
to a real database — in which case `flyway_schema_history` has no rows at all,
because Flyway never completed, so no checksum would actually conflict.

Effect: unblocks `mvn clean verify`, and a fresh deployment finally starts.
Cost: rewrites published migrations; anyone who applied them manually had
partial schemas that need inspecting.

### Option B — keep immutability, accept the red CI

Leave the chain as it is, keep this document and V10 as evidence, and mark the
backend job as blocked. A fresh database still cannot be brought up, which is a
real operational risk that has to be handled out of band.

Chosen for this round: **B**, pending an owner decision on A.

## Recommended commands for Option A (not run)

```bash
# Replace every ADD COLUMN IF NOT EXISTS / ADD INDEX IF NOT EXISTS with the
# information_schema + PREPARE guard pattern used in V10, then delete V10.
```

Once A is approved and applied, V10 should be deleted rather than left as dead
weight, and `MigrationIT` will go green.
