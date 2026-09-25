# Migration Policy

The project runs **Flyway** (`spring.flyway.locations: classpath:db/migration`).

Migrations live at the Flyway standard location,
`backend/src/main/resources/db/migration/`, and that is the only tracked copy.
There is no build-time copy step, so nothing can drift.

| Order | File | Notes |
|---|---|---|
| 1.0 | `.../db/migration/v1.0__init.sql` | schema + seed, **no** `CREATE DATABASE`/`USE` |
| 1.1 | `.../db/migration/v1.1__operation_log.sql` | `sys_operation_log` |
| 1.2 | `.../db/migration/v1.2__rbac_schema_completion.sql` | `sys_menu` RBAC columns + permission seed |

Three tests guard this arrangement:
`test_migrations_live_at_flyway_standard_location` (they must be where Flyway
reads them, not in a repo-root `sql/`),
`test_no_unversioned_sql_in_flyway_dir`, and
`test_migrations_do_not_switch_database`.

## Rules

### 1. Never modify a published migration

A migration is "published" once it is committed, pushed, or has been run against
any shared database. After that, **only add a new version** — editing it in
place causes a checksum mismatch on every other machine.

```
published V1.2  →  fix goes in V1.3, never in a rewritten V1.2
```

As of this writing **V1.2 has never been committed or pushed** (`git ls-files`
does not track it), so it may still be corrected in place. This exception
expires the moment it is committed.

### 2. Migrations must not switch databases

`CREATE DATABASE` / `USE` redirect every table into a hardcoded database instead
of the one passed on the command line. V1.0 originally did this and it silently
broke V1.2 with `Table '...sys_menu' doesn't exist`.

Creating the database is the caller's job:

```bash
mysql -u root -p -e "CREATE DATABASE erms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p erms < backend/src/main/resources/db/migration/v1.0__init.sql
```

`tests/test_smoke.py::test_migrations_do_not_switch_database` guards this.

### 3. Idempotency is a safety net, not a licence

Flyway runs a versioned migration exactly once and records its checksum in
`flyway_schema_history`; `migrate()` a second time applies nothing, and
`validate()` fails if a file changed in place. That is the real guarantee.

V1.2 additionally guards its `ALTER`s with `information_schema` checks and its
`INSERT`s with `NOT EXISTS`, so re-running the file by hand is harmless. That matters only because the hand-run path bypasses
Flyway — it is **not** permission to mutate a later V1.3 in place.

### 4. Every permission in code must be seeded

`AuthoritySeedConsistencyTest` fails the build when a `@ss.hasAuthority('…')`
value is missing from the V1.2 seed or from `PermConst`. Both directions matter:
an unseeded authority means 403 for every admin; a seeded-but-unreferenced
authority is dead weight.

### 5. Ghost permissions get deleted, not kept for appearances

`system:menu:add` / `edit` / `del` and `system:user:export` were removed because
nothing referenced them — no Controller endpoint, no Service use case, no
frontend call. Verify all three before deleting a permission, and record the
evidence in the commit.

## Verification status

```
RUNTIME_STATUS: VERIFIED   (MySQL 8.0.44, scratch database)
```

Full commands, recorded output, and the `CREATE DATABASE`/`USE` defect are in
`migration-audit.md`. Re-run after any change:

```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS erms_migrate_test;
  CREATE DATABASE erms_migrate_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
for f in backend/src/main/resources/db/migration/v1.0__init.sql backend/src/main/resources/db/migration/v1.1__operation_log.sql backend/src/main/resources/db/migration/v1.2__rbac_schema_completion.sql; do
  mysql -u root -p erms_migrate_test < "$f" || echo "FAILED: $f"
done
mysql -u root -p erms_migrate_test -e "
  SELECT COUNT(*) FROM sys_menu WHERE permission IS NOT NULL;
  SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1;"
```
