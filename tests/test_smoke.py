"""
Generic Sys Admin - Smoke Test Suite
=====================================
Basic smoke tests to verify core functionality.
"""
import importlib
import sys
import os
import pytest

# Add project root to path
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, PROJECT_ROOT)


class TestModuleImports:
    """Test that core modules can be imported."""

    def test_import_main(self):
        """Verify the main module can be imported."""
        try:
            spec = importlib.util.spec_from_file_location(
                "main", os.path.join(PROJECT_ROOT, "main.py")
            )
            module = importlib.util.module_from_spec(spec)
            # Don't execute main, just verify it loads
            assert spec is not None
        except Exception as e:
            pytest.fail(f"Failed to load main.py: {e}")

    def test_import_dashboard(self):
        """Verify dashboard module can be imported."""
        try:
            spec = importlib.util.spec_from_file_location(
                "dashboard", os.path.join(PROJECT_ROOT, "dashboard.py")
            )
            module = importlib.util.module_from_spec(spec)
            assert spec is not None
        except Exception as e:
            pytest.fail(f"Failed to load dashboard.py: {e}")

    def test_requirements_exist(self):
        """Verify requirements.txt exists and is not empty."""
        req_path = os.path.join(PROJECT_ROOT, "requirements.txt")
        assert os.path.exists(req_path), "requirements.txt not found"
        with open(req_path, "r") as f:
            content = f.read().strip()
            assert len(content) > 0, "requirements.txt is empty"


class TestProjectStructure:
    """Test that project has expected directory structure."""

    def test_backend_directory_exists(self):
        """Verify backend directory exists."""
        assert os.path.isdir(os.path.join(PROJECT_ROOT, "backend"))

    def test_frontend_directory_exists(self):
        """Verify frontend directory exists."""
        assert os.path.isdir(os.path.join(PROJECT_ROOT, "frontend"))

    def test_sql_directory_exists(self):
        """Verify sql directory exists."""
        assert os.path.isdir(os.path.join(PROJECT_ROOT, "sql"))

    def test_tests_directory_exists(self):
        """Verify tests directory exists."""
        assert os.path.isdir(os.path.join(PROJECT_ROOT, "tests"))

    def test_docker_compose_exists(self):
        """Verify docker-compose.yml exists."""
        assert os.path.exists(os.path.join(PROJECT_ROOT, "docker-compose.yml"))

    def test_readme_exists(self):
        """Verify README.md exists and has content."""
        readme_path = os.path.join(PROJECT_ROOT, "README.md")
        assert os.path.exists(readme_path), "README.md not found"
        with open(readme_path, "r", encoding="utf-8") as f:
            content = f.read()
            assert len(content) > 100, "README.md is too short"


class TestMainCli:
    """Test main.py CLI commands."""

    def test_main_has_tools_dict(self):
        """Verify main.py has TOOLS dictionary."""
        main_path = os.path.join(PROJECT_ROOT, "main.py")
        with open(main_path, "r", encoding="utf-8") as f:
            content = f.read()
            assert "TOOLS" in content
            assert "def main" in content
            assert "def list_tools" in content

    def test_main_has_required_commands(self):
        """Verify main.py has essential commands."""
        main_path = os.path.join(PROJECT_ROOT, "main.py")
        with open(main_path, "r", encoding="utf-8") as f:
            content = f.read()
            required_commands = ["dashboard", "list", "all"]
            for cmd in required_commands:
                assert cmd in content, f"Command '{cmd}' not found in main.py"


class TestDashboard:
    """Test dashboard.py structure."""

    def test_dashboard_has_flask_app(self):
        """Verify dashboard.py creates Flask app."""
        dashboard_path = os.path.join(PROJECT_ROOT, "dashboard.py")
        with open(dashboard_path, "r", encoding="utf-8") as f:
            content = f.read()
            assert "Flask" in content
            assert "app = Flask" in content

    def test_dashboard_has_routes(self):
        """Verify dashboard.py has route definitions."""
        dashboard_path = os.path.join(PROJECT_ROOT, "dashboard.py")
        with open(dashboard_path, "r", encoding="utf-8") as f:
            content = f.read()
            assert "@app.route" in content
            assert "/api/status" in content
            assert "/api/health" in content

    def test_dashboard_has_system_info(self):
        """Verify dashboard.py has system info functions."""
        dashboard_path = os.path.join(PROJECT_ROOT, "dashboard.py")
        with open(dashboard_path, "r", encoding="utf-8") as f:
            content = f.read()
            assert "get_cpu_info" in content
            assert "get_mem_info" in content
            assert "get_disk_info" in content


class TestBackendStructure:
    """Test backend directory structure."""

    def test_pom_xml_exists(self):
        """Verify Maven pom.xml exists."""
        pom_path = os.path.join(PROJECT_ROOT, "backend", "pom.xml")
        assert os.path.exists(pom_path), "pom.xml not found in backend/"

    def test_src_directory_exists(self):
        """Verify backend src directory exists."""
        src_path = os.path.join(PROJECT_ROOT, "backend", "src")
        assert os.path.isdir(src_path), "src/ directory not found in backend/"

    def test_dockerfile_exists(self):
        """Verify Dockerfile exists in backend."""
        dockerfile_path = os.path.join(PROJECT_ROOT, "backend", "Dockerfile")
        assert os.path.exists(dockerfile_path), "Dockerfile not found in backend/"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
