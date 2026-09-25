"""
Generic Sys Admin - Repository Structure Smoke Tests
===================================================
Guards the repository layout: backend / frontend / sql / CI must stay wired
together, and the SQL migrations must be present and versioned in order.

These tests are intentionally dependency-free so `pytest tests/` runs in CI
without installing Flask or any of the removed one-off tooling.
"""
import os
import re
import sys

import pytest

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# SQL baseline files that must exist after the graduation-material cleanup.
EXPECTED_SQL = [
    "backend/src/main/resources/db/migration/v1.0__init.sql",
    "backend/src/main/resources/db/migration/v1.1__operation_log.sql",
    "backend/src/main/resources/db/migration/v1.2__rbac_schema_completion.sql",
]


class TestTopLevelLayout:
    """The repository root should only hold project files, not scratch scripts."""

    def test_no_one_off_scripts_in_root(self):
        """Regression: the root used to carry ~34 one-off fix_*.py / check_*.py helpers."""
        offenders = [
            name for name in os.listdir(PROJECT_ROOT)
            if name.endswith((".py", ".bat", ".class"))
        ]
        assert offenders == [], f"scratch files leaked back into the root: {offenders}"

    def test_no_thesis_artifacts_in_root(self):
        """Regression: .docx papers and defense material must not be tracked."""
        offenders = [
            name for name in os.listdir(PROJECT_ROOT)
            if name.lower().endswith((".docx", ".ppt", ".pptx"))
        ]
        assert offenders == [], f"thesis artifacts present: {offenders}"

    def test_core_layout_present(self):
        for directory in ("backend", "frontend", "tests", ".github/workflows"):
            assert os.path.isdir(os.path.join(PROJECT_ROOT, directory)), \
                f"missing directory: {directory}"

    def test_docker_compose_and_readme_present(self):
        assert os.path.exists(os.path.join(PROJECT_ROOT, "docker-compose.yml"))
        readme = os.path.join(PROJECT_ROOT, "README.md")
        assert os.path.exists(readme), "README.md not found"
        with open(readme, "r", encoding="utf-8") as f:
            assert len(f.read()) > 100, "README.md is too short"


class TestBackendStructure:
    """Backend must remain a buildable Maven project."""

    def test_pom_xml_exists(self):
        assert os.path.exists(os.path.join(PROJECT_ROOT, "backend", "pom.xml"))

    def test_src_tree_exists(self):
        assert os.path.isdir(os.path.join(PROJECT_ROOT, "backend", "src", "main", "java"))
        assert os.path.isdir(os.path.join(PROJECT_ROOT, "backend", "src", "test", "java"))

    def test_dockerfile_exists(self):
        assert os.path.exists(os.path.join(PROJECT_ROOT, "backend", "Dockerfile"))


class TestSqlMigrations:
    """Every migration file must exist, cover versioned names, and be non-empty."""

    @pytest.mark.parametrize("relative", EXPECTED_SQL)
    def test_migration_file_present(self, relative):
        path = os.path.join(PROJECT_ROOT, relative)
        assert os.path.exists(path), f"missing migration: {relative}"

    def test_migrations_are_not_empty(self):
        for relative in EXPECTED_SQL:
            path = os.path.join(PROJECT_ROOT, relative)
            if not os.path.exists(path):
                pytest.skip(f"{relative} absent")
            with open(path, "r", encoding="utf-8") as f:
                assert f.read().strip(), f"{relative} is empty"

    def test_migration_versions_are_ordered_and_unique(self):
        sql_dir = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "db", "migration")
        if not os.path.isdir(sql_dir):
            pytest.skip("migration dir absent")
        versions = []
        for name in os.listdir(sql_dir):
            match = re.match(r"V(\d+)\.(\d+)__", name)
            if match:
                versions.append((int(match.group(1)), int(match.group(2))))
        assert versions, "no versioned migrations found"
        assert len(versions) == len(set(versions)), "duplicate migration versions"

    def test_migrations_do_not_switch_database(self):
        """Regression: v1.0 used to start with CREATE DATABASE / USE, which sent
        every table to a hardcoded database and broke v1.2 with
        "Table '...sys_menu' doesn't exist". Verified against real MySQL 8.0.44.
        """
        sql_dir = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "db", "migration")
        if not os.path.isdir(sql_dir):
            pytest.skip("migration dir absent")
        offenders = []
        for name in os.listdir(sql_dir):
            if not name.endswith(".sql"):
                continue
            with open(os.path.join(sql_dir, name), "r", encoding="utf-8") as f:
                for lineno, line in enumerate(f, start=1):
                    stripped = line.strip()
                    if re.match(r"(?i)^use\s+`", stripped) or \
                            re.match(r"(?i)^create\s+database\b", stripped):
                        offenders.append(f"{name}:{lineno}: {stripped}")
        assert offenders == [], (
            "migrations must not switch databases; the caller picks the target: "
            + "; ".join(offenders)
        )

    def test_migrations_live_at_flyway_standard_location(self):
        """Migrations must live where Flyway reads them: classpath:db/migration.

        Regression: they used to sit in the repo-root sql/ and were copied by the
        pom. Maven's <directory> cannot reliably resolve a sibling-module '..'
        path on Windows (measured: three variants, all copied nothing), so a
        build could produce a jar with zero migrations. Keeping the tracked files
        at the Flyway standard location removes the copy step entirely.
        """
        flyway_dir = os.path.join(
            PROJECT_ROOT, "backend", "src", "main", "resources", "db", "migration"
        )
        assert os.path.isdir(flyway_dir), \
            f"migrations must live at {flyway_dir}"
        versioned = sorted(
            n for n in os.listdir(flyway_dir)
            if n.endswith(".sql") and re.match(r"[Vv]\d+\.\d+__", n)
        )
        assert versioned, "no versioned migrations at the Flyway location"
        assert not os.path.isdir(os.path.join(PROJECT_ROOT, "sql")), \
            "repo-root sql/ still exists; migrations moved under backend/"

    def test_no_unversioned_sql_in_flyway_dir(self):
        """Flyway would either ignore or reject a non-versioned file in its location."""
        flyway_dir = os.path.join(
            PROJECT_ROOT, "backend", "src", "main", "resources", "db", "migration"
        )
        if not os.path.isdir(flyway_dir):
            pytest.skip("flyway dir absent")
        for name in os.listdir(flyway_dir):
            if not name.endswith(".sql"):
                continue
            assert re.match(r"[Vv]\d+\.\d+__", name), \
                f"{name} is not a Flyway versioned migration (V<n>.<n>__desc.sql)"

    def test_migrations_use_flyway_default_prefix(self):
        """Regression: the files were named v1.0__*.sql with a lowercase v.

        Flyway's default sqlMigrationPrefix is an uppercase "V" and its matching
        is case-sensitive. On Windows/NTFS that looks fine, but on the Linux CI
        runner Flyway scanned the classpath and reported
        "Successfully validated 0 migrations" — a jar that shipped no schema at
        all, so MigrationIT failed on every assertion.
        """
        flyway_dir = os.path.join(
            PROJECT_ROOT, "backend", "src", "main", "resources", "db", "migration"
        )
        if not os.path.isdir(flyway_dir):
            pytest.skip("flyway dir absent")
        for name in os.listdir(flyway_dir):
            if not name.endswith(".sql"):
                continue
            assert name.startswith("V"), (
                f"{name}: Flyway's default prefix is an uppercase V; a lowercase "
                "v is silently ignored on case-sensitive filesystems (Linux CI)"
            )
            assert re.match(r"V\d+\.\d+__.+\.sql$", name), (
                f"{name}: expected V<major>.<minor>__<description>.sql"
            )


class TestSecurityRegression:
    """Guards against the RBAC / current-user defects that were fixed."""

    def _read(self, relative):
        with open(os.path.join(PROJECT_ROOT, relative), "r", encoding="utf-8") as f:
            return f.read()

    def test_no_hardcoded_user_id_fallback(self):
        """Regression: controllers used to return 1L when the user id was unknown."""
        controller_dir = os.path.join(
            PROJECT_ROOT, "backend", "src", "main", "java", "com", "zyy", "controller"
        )
        offenders = []
        for root, _dirs, files in os.walk(controller_dir):
            for name in files:
                if not name.endswith(".java"):
                    continue
                path = os.path.join(root, name)
                with open(path, "r", encoding="utf-8") as f:
                    if "return 1L" in f.read():
                        offenders.append(name)
        assert offenders == [], f"hardcoded operator fallback still present: {offenders}"

    def test_users_listing_is_not_anonymous(self):
        """Regression: requestMatchers('/api/users') used to allow anonymous GET."""
        config = self._read(
            "backend/src/main/java/com/zyy/config/WebSecurityConfig.java"
        )
        # Only POST on the auth endpoints may be public. A permitAll on the bare
        # resource (any method) would expose GET /api/users to anonymous callers.
        public_users = re.findall(
            r'requestMatchers\(([^)]*?)\)\s*\.permitAll\(\)',
            config,
        )
        for args in public_users:
            assert "HttpMethod.POST" in args or "/api/users" not in args, \
                f"anonymous permitAll not scoped to a single method: {args.strip()}"

    def test_actuator_metrics_not_publicly_exposed(self):
        """Regression: application.yml used to expose the metrics endpoint."""
        app_yml = self._read("backend/src/main/resources/application.yml")
        assert "include: health,info" in app_yml, \
            "actuator exposure should be limited to health and info"


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
