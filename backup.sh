#!/bin/bash
# =============================================
# ERMS - Backup Script
# =============================================
# Backs up MySQL, Redis, and uploaded files.
# Rotates backups keeping the last 7.
# =============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE=".env"
BACKUP_ROOT="/opt/erms/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_ROOT}/${TIMESTAMP}"
KEEP_COUNT=7

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

# Backup MySQL
backup_mysql() {
    log_info "Backing up MySQL database..."
    mkdir -p "$BACKUP_DIR"

    local db_host="${DB_HOST:-localhost}"
    local db_port="${DB_PORT:-3306}"
    local db_name="${DB_NAME:-generic_sys_admin}"
    local db_user="${DB_USERNAME:-admin}"
    local db_pass="${DB_PASSWORD:-admin123}"

    # If running inside docker, use the container directly
    if docker ps --format '{{.Names}}' | grep -q 'erms-mysql'; then
        docker exec erms-mysql mysqldump \
            -u root -p"${DB_ROOT_PASSWORD:-root123456}" \
            --single-transaction --routines --triggers \
            "$db_name" > "${BACKUP_DIR}/mysql_${db_name}.sql" 2>/dev/null
    else
        mysqldump -h "$db_host" -P "$db_port" -u "$db_user" -p"$db_pass" \
            --single-transaction --routines --triggers \
            "$db_name" > "${BACKUP_DIR}/mysql_${db_name}.sql" 2>/dev/null
    fi

    gzip "${BACKUP_DIR}/mysql_${db_name}.sql"
    log_info "MySQL backup completed: mysql_${db_name}.sql.gz"
}

# Backup Redis
backup_redis() {
    log_info "Backing up Redis data..."
    mkdir -p "$BACKUP_DIR"

    if docker ps --format '{{.Names}}' | grep -q 'erms-redis'; then
        docker exec erms-redis redis-cli -a "${REDIS_PASSWORD:-}" BGSAVE 2>/dev/null || true
        sleep 2
        docker cp erms-redis:/data/dump.rdb "${BACKUP_DIR}/redis_dump.rdb" 2>/dev/null || true
    else
        if [ -f /opt/erms/redis/data/dump.rdb ]; then
            cp /opt/erms/redis/data/dump.rdb "${BACKUP_DIR}/redis_dump.rdb"
        fi
    fi

    if [ -f "${BACKUP_DIR}/redis_dump.rdb" ]; then
        gzip "${BACKUP_DIR}/redis_dump.rdb"
        log_info "Redis backup completed: redis_dump.rdb.gz"
    else
        log_warn "Redis dump file not found, skipping Redis backup."
    fi
}

# Backup uploaded files
backup_uploads() {
    log_info "Backing up uploaded files..."
    mkdir -p "$BACKUP_DIR"

    if [ -d /opt/erms/uploads ] && [ "$(ls -A /opt/erms/uploads 2>/dev/null)" ]; then
        tar czf "${BACKUP_DIR}/uploads.tar.gz" -C /opt/erms uploads/
        log_info "Uploads backup completed: uploads.tar.gz"
    else
        log_warn "No uploads directory or it is empty, skipping."
    fi
}

# Rotate old backups
rotate_backups() {
    log_info "Rotating backups (keeping last ${KEEP_COUNT})..."
    local count
    count=$(ls -1d "${BACKUP_ROOT}"/20* 2>/dev/null | wc -l)

    if [ "$count" -gt "$KEEP_COUNT" ]; then
        local to_remove
        to_remove=$((count - KEEP_COUNT))
        ls -1d "${BACKUP_ROOT}"/20* | head -n "$to_remove" | while read -r dir; do
            log_info "Removing old backup: $dir"
            rm -rf "$dir"
        done
    fi

    log_info "Backup rotation completed."
}

# Main
main() {
    log_info "=== ERMS Backup Started ==="
    load_env
    backup_mysql
    backup_redis
    backup_uploads
    rotate_backups
    log_info "=== Backup Completed: ${BACKUP_DIR} ==="
}

main "$@"
