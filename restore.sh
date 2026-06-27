#!/bin/bash
# =============================================
# ERMS - Restore Script
# =============================================
# Restores from a specified backup directory.
# Usage: ./restore.sh <backup_directory>
# =============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE=".env"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# Load environment
load_env() {
    if [ -f "$ENV_FILE" ]; then
        set -a
        source "$ENV_FILE"
        set +a
    else
        log_error ".env file not found at $ENV_FILE"
        exit 1
    fi
}

# Validate backup directory
validate_backup_dir() {
    if [ -z "${1:-}" ]; then
        log_error "Usage: $0 <backup_directory>"
        echo "Available backups:"
        ls -1d /opt/erms/backups/20* 2>/dev/null || echo "  No backups found"
        exit 1
    fi

    BACKUP_DIR="$1"
    if [ ! -d "$BACKUP_DIR" ]; then
        log_error "Backup directory not found: $BACKUP_DIR"
        exit 1
    fi
    log_info "Restoring from: $BACKUP_DIR"
}

# Stop services
stop_services() {
    log_info "Stopping backend service to prevent data conflicts..."
    docker compose -f docker-compose.yml -f docker-compose.prod.yml stop backend 2>/dev/null || true
}

# Restore MySQL
restore_mysql() {
    local db_name="${DB_NAME:-generic_sys_admin}"
    local sql_file="${BACKUP_DIR}/mysql_${db_name}.sql.gz"

    if [ ! -f "$sql_file" ]; then
        log_warn "MySQL backup not found at $sql_file, skipping."
        return
    fi

    log_info "Restoring MySQL database..."
    gunzip -k "$sql_file" 2>/dev/null || true
    local uncompressed="${sql_file%.gz}"

    if docker ps --format '{{.Names}}' | grep -q 'erms-mysql'; then
        docker exec -i erms-mysql mysql -u root -p"${DB_ROOT_PASSWORD:-root123456}" \
            "$db_name" < "$uncompressed" 2>/dev/null
    else
        mysql -h "${DB_HOST:-localhost}" -P "${DB_PORT:-3306}" \
            -u "${DB_USERNAME:-admin}" -p"${DB_PASSWORD:-admin123}" \
            "$db_name" < "$uncompressed" 2>/dev/null
    fi

    rm -f "$uncompressed"
    log_info "MySQL restore completed."
}

# Restore Redis
restore_redis() {
    local rdb_file="${BACKUP_DIR}/redis_dump.rdb.gz"

    if [ ! -f "$rdb_file" ]; then
        log_warn "Redis backup not found at $rdb_file, skipping."
        return
    fi

    log_info "Restoring Redis data..."

    # Stop Redis to replace data file
    docker compose -f docker-compose.yml -f docker-compose.prod.yml stop redis 2>/dev/null || true

    gunzip -k "$rdb_file" 2>/dev/null || true
    local uncompressed="${rdb_file%.gz}"

    if [ -d /opt/erms/redis/data ]; then
        cp "$uncompressed" /opt/erms/redis/data/dump.rdb
    fi

    rm -f "$uncompressed"
    docker compose -f docker-compose.yml -f docker-compose.prod.yml start redis 2>/dev/null || true
    log_info "Redis restore completed."
}

# Restore uploads
restore_uploads() {
    local tar_file="${BACKUP_DIR}/uploads.tar.gz"

    if [ ! -f "$tar_file" ]; then
        log_warn "Uploads backup not found at $tar_file, skipping."
        return
    fi

    log_info "Restoring uploaded files..."
    tar xzf "$tar_file" -C /opt/erms/
    log_info "Uploads restore completed."
}

# Start services
start_services() {
    log_info "Starting all services..."
    docker compose -f docker-compose.yml -f docker-compose.prod.yml start backend 2>/dev/null || true
    log_info "Services started."
}

# Main
main() {
    log_info "=== ERMS Restore Started ==="
    load_env
    validate_backup_dir "${1:-}"
    stop_services
    restore_mysql
    restore_redis
    restore_uploads
    start_services
    log_info "=== Restore Completed ==="
}

main "$@"
