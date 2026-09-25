# Migration Audit

Status of every migration. Static checks were done against source; runtime
verification is now automated against a real MySQL container (see
`MigrationIT`).

## Chain

Migrations live at the Flyway standard location,
`backend/src/main/resources/db/migration/` (i.e. `classpath:db/migration`), and
that is the only copy. There is no build-time copy step, so nothing can drift.

| Order | File | Notes |
|---|---|---|
| 1.0 | `.../db/migration/v1.0__init.sql` | schema + seed, **no** `CREATE DATABASE`/`USE` |
| 1.1 | `.../db/migration/v1.1__operation_log.sql` | `sys_operation_log` |
| 1.2 | `.../db/migration/v1.2__rbac_schema_completion.sql` | `sys_menu` RBAC columns + permission seed |

### Why they moved here

They used to sit in a repo-root `sql/` and were copied into the module by the
pom. Three variants were measured, and **all three copied zero files on this
platform**:

| Attempt | Result |
|---|---|
| `<resource><directory>../../sql</directory><targetPath>db/migration` | `Skipping non-existing directory ...\target\classes` — the whole resource group skipped |
| `copy-resources` execution, `outputDirectory` under `target/` | ran, copied nothing; `directory` resolved to `...\backend/../../sql` (mixed separators + unnormalised `..`), which `java.io.File` does not resolve on Windows |
| `maven-antrun-plugin` `copy`/`fileset` | `D:\ZYY Project\sql does not exist` — `${project.basedir}` is not visible inside the antrun Ant context |

The consequence was silent and severe: `mvn clean package` produced a jar with
**no migrations at all**, so a fresh deployment would start with an empty schema
and Flyway would report nothing to do.
Before this arrangement there were two hand-synced copies, and the one the tests
read was not the one the manual instructions used — the drift was invisible.

## Static findings

### 1. `sys_menu` was missing three mapped columns — FIXED in V1.2

`SysMenuEntity` declares `permission`, `menu_type`, `is_external`, and
`SysMenuMapper` uses `SELECT m.*`. V1.0's `sys_menu` had neither
`permission` nor `menu_type`/`is_external` (`init.sql` had `cacheable` only).
Any query would have failed with `Unknown column 'permission'`.
V1.2 adds all three and V1.0/`init.sql` were updated so fresh databases get
them directly.

### 2. Entity ↔ schema consistency — verified for all tables

Checked every entity's camelCase fields against its table's snake_case
columns, including inherited `BaseEntity` fields:

| Entity | Table | Result |
|---|---|---|
| `SysUserEntity` | `sys_user` | consistent |
| `SysRoleEntity` | `sys_role` | consistent |
| `SysMenuEntity` | `sys_menu` | consistent **after V1.2** |
| `SysRoleMenuEntity` | `sys_role_menu` | consistent |
| `SysUserRoleEntity` | `sys_user_role` | consistent |
| `EquipmentEntity` | `sys_equipment` | consistent |
| `ConsumableEntity` | `sys_consumable` | consistent |
| `InventoryRecordEntity` | `sys_inventory_record` | consistent |
| `InventoryTransactionEntity` | `sys_inventory_transaction` | consistent |
| `SysOperationLogEntity` | `sys_operation_log` | consistent |

### 3. Seed alignment with `@PreAuthorize` — FIXED in V1.2

`RbacServiceImpl.getPermissionsByUserId()` reads `menu.getPermission()`.
V1.0 seeded 8 top-level menus with **no** `permission` value, so the fallback
`pathToPermission()` produced names like `system:user:index` that no
`@PreAuthorize` in the codebase matches. V1.2 sets the real `PermConst` values
on the top-level menus and seeds button-level permissions.

### 4. Idempotency

| Script | Idempotent? | Notes |
|---|---|---|
| `init.sql` | no | `DROP TABLE IF EXISTS` then recreate — destructive by design, intended for empty DB only |
| `v1.0__init.sql` | no | same |
| `v1.1__operation_log.sql` | no | `DROP TABLE IF EXISTS` then recreate |
| `v1.2__rbac_schema_completion.sql` | **yes** | `ALTER`s guarded by `information_schema` checks; all `INSERT`s guarded by `NOT EXISTS` |

V1.2 uses `SET @var := ...` + `PREPARE`/`EXECUTE`/`DEALLOCATE` because MySQL
DDL cannot be parameterised or run inside an `IF`. Re-running it is a no-op,
which matters here precisely because there is no migration table.

### 5. Ordering / dependencies

`ALTER` statements only touch `sys_menu`, which V1.0 creates. No `ALTER`
targets a table created later. V1.2's seed `JOIN`s on `sys_menu.path`, which
V1.0 populates.

### 6. Reserved words and types

No table or column collides with a MySQL reserved word that requires quoting
beyond the usual backticks. Types used are portable within MySQL 5.7/8.0
(`BIGINT UNSIGNED`, `VARCHAR`, `TINYINT`, `DATETIME`). Note
`BIGINT UNSIGNED` is MySQL-specific; it is not portable to PostgreSQL.

### 7. Foreign keys

None are declared. Referential integrity between `sys_role_menu`,
`sys_user_role`, `sys_menu` and `sys_role` is by convention only, matching the
MyBatis-Plus `BaseMapper` usage. Not a defect, but worth knowing: a deleted
role leaves orphan `sys_role_menu` rows.

## Runtime verification

```
RUNTIME_STATUS: VERIFIED
MySQL version:  8.0.44
Verified by:     backend/src/test/java/com/zyy/database/MigrationIT
Driver:          Flyway (classpath:db/migration) + Testcontainers mysql:8.0.44
Also verified:   手工 mysql CLI 执行同一批 migration 文件
```

### What the automated test proves

`mvn verify` runs `MigrationIT`, which:

1. starts a `mysql:8.0.44` container;
2. asserts `flyway.info().all()` discovers exactly `1.0`, `1.1`, `1.2` in order;
3. `migrate()` on an empty DB applies exactly 3 migrations;
4. asserts the 10 tables and the three `sys_menu` columns exist;
5. extracts every `@ss.hasAuthority('…')` from
   `backend/src/main/java/com/zyy/controller/` and asserts each one is present
   in `sys_menu.permission` **and** granted to `role_id = 1`;
6. runs the `sys_menu` ⋈ `sys_role_menu` join that `SysMenuMapper` uses;
7. re-runs `migrate()` and asserts 0 applied;
8. `flyway.validate()`.

If no Docker daemon is reachable the class aborts with a skip, so a local
`mvn verify` stays green while CI actually exercises MySQL.

### What was executed manually first

```
1. DROP/CREATE erms_migrate_test (utf8mb4 / utf8mb4_unicode_ci)
2. backend/src/main/resources/db/migration/v1.0__init.sql                          → OK (10 tables + seed)
3. backend/src/main/resources/db/migration/v1.1__operation_log.sql                 → OK
4. backend/src/main/resources/db/migration/v1.2__rbac_schema_completion.sql        → OK
5. backend/src/main/resources/db/migration/v1.2__rbac_schema_completion.sql (re-run) → OK
```

### What was actually executed

```
1. DROP/CREATE erms_migrate_test (utf8mb4 / utf8mb4_unicode_ci)
2. backend/src/main/resources/db/migration/v1.0__init.sql            → OK   (10 tables + seed)
3. backend/src/main/resources/db/migration/v1.1__operation_log.sql   → OK
4. backend/src/main/resources/db/migration/v1.2__rbac_schema_completion.sql → OK
5. backend/src/main/resources/db/migration/v1.2__rbac_schema_completion.sql (re-run, idempotency) → OK, no error
```

### Results

| Check | Result |
|---|---|
| Tables created | 10 — `sys_user`, `sys_role`, `sys_user_role`, `sys_role_menu`, `sys_menu`, `sys_operation_log`, `sys_equipment`, `sys_consumable`, `sys_inventory_record`, `sys_inventory_transaction` |
| Menus with a permission | 41 |
| Menus granted to Administrator (`role_id = 1`) | 41 (all match) |
| Button-level permissions (`menu_type = 3`) | 33 |
| Roles / users seeded | 3 / 3 |
| V1.2 re-run | no error — the `information_schema` guards and `NOT EXISTS` inserts work |

### Authority coverage against source

Every `@ss.hasAuthority('…')` in `backend/src/main/java/com/zyy/controller/`
(31 distinct values) exists in `sys_menu.permission` and is granted to the
Administrator role. Seeded-but-unused values: `system:menu:add`,
`system:menu:edit`, `system:menu:del`, `system:user:export` — expected, because
`MenuController` has no write endpoints (see `../security/permission-matrix.md`).

### Defect found and fixed by this run

`backend/src/main/resources/db/migration/v1.0__init.sql` and  began with
`CREATE DATABASE IF NOT EXISTS generic_sys_admin …; USE generic_sys_admin;`.
Importing into any other database silently created all tables inside
`generic_sys_admin`, so `v1.2` then failed with
`Table '…sys_menu' doesn't exist`. Both files no longer contain
`CREATE DATABASE` / `USE`; creating the database is now the caller's
responsibility. This is exactly the class of breakage that only a real run
surfaces — the static review had passed both files.
