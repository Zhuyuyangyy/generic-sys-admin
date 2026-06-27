#!/bin/bash
# =============================================
# ERMS - Production Deployment Script
# =============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE=".env"
COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v docker &>/dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! docker compose version &>/dev/null; then
        log_error "Docker Compose V2 is not available. Please install it."
        exit 1
    fi
}

# Check .env file
check_env() {
    if [ ! -f "$ENV_FILE" ]; then
        log_warn ".env file not found. Creating from .env.example..."
        if [ -f ".env.example" ]; then
            cp .env.example .env
            log_warn "Please review and update .env before deploying."
            log_warn "Key variables to set: JWT_SECRET, DB_PASSWORD, DB_ROOT_PASSWORD"
            exit 1
        else
            log_error "No .env or .env.example found. Please create .env with required variables."
            exit 1
        fi
    fi
    log_info ".env file found."
}

# Validate configuration
validate_config() {
    log_info "Validating configuration..."

    # Source env file for validation
    set -a
    source "$ENV_FILE"
    set +a

    local errors=0

    if [ -z "${JWT_SECRET:-}" ] || [ "$JWT_SECRET" = "please-change-this-secret-in-production-at-least-32-chars" ]; then
        log_error "JWT_SECRET is not set or uses default value. Please change it in .env"
        errors=$((errors + 1))
    fi

    if [ -z "${DB_PASSWORD:-}" ] || [ "$DB_PASSWORD" = "admin123" ]; then
        log_warn "DB_PASSWORD uses default value. Consider changing it for production."
    fi

    if [ -z "${CORS_ALLOWED_ORIGINS:-}" ]; then
        log_warn "CORS_ALLOWED_ORIGINS is not set. Default CORS policy will be used."
    fi

    if [ "$errors" -gt 0 ]; then
        log_error "Configuration validation failed with $errors error(s). Fix .env and retry."
        exit 1
    fi

    log_info "Configuration validation passed."
}

# Create production directories
create_dirs() {
    log_info "Creating production directories..."
    mkdir -p /opt/erms/mysql/data
    mkdir -p /opt/erms/redis/data
    mkdir -p /opt/erms/uploads
    mkdir -p /opt/erms/prometheus/data
    mkdir -p /opt/erms/grafana/data
    mkdir -p /opt/erms/backups
    log_info "Production directories created."
}

# Deploy
deploy() {
    log_info "Building and starting services..."
    docker compose $COMPOSE_FILES build --no-cache backend
    docker compose $COMPOSE_FILES up -d
    log_info "Waiting for services to become healthy..."
    sleep 10
    docker compose $COMPOSE_FILES ps
    log_info "Deployment complete!"
    log_info "  Backend:  http://localhost:${SERVER_PORT:-8080}"
    log_info "  Frontend: http://localhost:${FRONTEND_PORT:-80}"
    log_info "  Grafana:  http://localhost:${GRAFANA_PORT:-3001}"
}

# Main
main() {
    log_info "=== ERMS Production Deployment ==="
    check_prerequisites
    check_env
    validate_config
    create_dirs
    deploy
    log_info "=== Deployment finished successfully ==="
}

main "$@"
