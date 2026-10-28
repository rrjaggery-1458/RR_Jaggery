#!/usr/bin/env bash
# ==============================================================================
# RR JAGGERY TRADERS — PostgreSQL & Uploads Restore Script
# Sprint 10 — Production Operations & Disaster Recovery
# ==============================================================================
# Usage:
#   bash restore.sh /path/to/rr_jaggery_db_YYYYMMDD_HHMMSS.sql.gz [/path/to/rr_jaggery_uploads_YYYYMMDD_HHMMSS.tar.gz]
#
# Environment Variable Overrides (for testing or non-default targets):
#   TARGET_CONTAINER       (default: rr-postgres)
#   TARGET_DB_USER         (default: postgres)
#   TARGET_DB_NAME         (default: rr_jaggery_db)
#   COMPOSE_FILE           (default: /home/ubuntu/RR_Jaggery/compose.production.yaml)
#   SKIP_SAFETY_SNAPSHOT   (default: false)
#   RESTORE_FORCE          (default: false)
#   BACKUP_DIR             (default: /var/backups/rr-jaggery)
# ==============================================================================

set -euo pipefail

BACKUP_FILE="${1:-}"
UPLOADS_FILE="${2:-}"

TARGET_CONTAINER="${TARGET_CONTAINER:-rr-postgres}"
TARGET_DB_USER="${TARGET_DB_USER:-postgres}"
TARGET_DB_NAME="${TARGET_DB_NAME:-rr_jaggery_db}"
COMPOSE_FILE="${COMPOSE_FILE:-/home/ubuntu/RR_Jaggery/compose.production.yaml}"
SKIP_SAFETY_SNAPSHOT="${SKIP_SAFETY_SNAPSHOT:-false}"
RESTORE_FORCE="${RESTORE_FORCE:-false}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/rr-jaggery}"
LOG_FILE="${LOG_FILE:-/var/log/rr-jaggery/restore.log}"

# Ensure log directory exists
mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null || true

log() {
    local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
    echo "$msg"
    if [ -w "$(dirname "$LOG_FILE")" ]; then
        echo "$msg" >> "$LOG_FILE" 2>/dev/null || true
    fi
}

# -----------------------------------------------------------------------------
# 1. Pre-Flight Validations
# -----------------------------------------------------------------------------
if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 /path/to/database_backup.sql.gz [/path/to/uploads_backup.tar.gz]" >&2
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    log "ERROR: Backup file not found: $BACKUP_FILE" >&2
    exit 1
fi

if [ ! -s "$BACKUP_FILE" ]; then
    log "ERROR: Backup file is empty (0 bytes): $BACKUP_FILE" >&2
    exit 1
fi

log "Verifying backup archive integrity..."
if ! gzip -t "$BACKUP_FILE" 2>/dev/null; then
    log "ERROR: Backup file failed gzip integrity check: $BACKUP_FILE" >&2
    exit 1
fi

# Verify SHA256 checksum if .sha256 sidecar file exists
if [ -f "${BACKUP_FILE}.sha256" ]; then
    log "Verifying SHA256 checksum against ${BACKUP_FILE}.sha256..."
    if ! (cd "$(dirname "$BACKUP_FILE")" && sha256sum -c "$(basename "$BACKUP_FILE").sha256" >/dev/null 2>&1); then
        log "ERROR: Checksum mismatch for $BACKUP_FILE! Aborting restore." >&2
        exit 1
    fi
    log "Checksum verified: OK."
fi

# Check uploads archive integrity if provided
if [ -n "$UPLOADS_FILE" ]; then
    if [ ! -f "$UPLOADS_FILE" ] || [ ! -s "$UPLOADS_FILE" ]; then
        log "ERROR: Uploads backup file not found or empty: $UPLOADS_FILE" >&2
        exit 1
    fi
    if ! tar -tzf "$UPLOADS_FILE" >/dev/null 2>&1; then
        log "ERROR: Uploads backup archive failed tar integrity check: $UPLOADS_FILE" >&2
        exit 1
    fi
fi

# Verify target container is running
if ! docker ps --format '{{.Names}}' | grep -wq "${TARGET_CONTAINER}"; then
    log "ERROR: Target PostgreSQL container '${TARGET_CONTAINER}' is not running." >&2
    exit 1
fi

if ! docker exec "${TARGET_CONTAINER}" pg_isready -U "${TARGET_DB_USER}" -d "${TARGET_DB_NAME}" >/dev/null 2>&1; then
    log "ERROR: Target database '${TARGET_DB_NAME}' in container '${TARGET_CONTAINER}' is not accepting connections." >&2
    exit 1
fi

log "======================================================================"
log " RR Jaggery Traders — Database Restore"
log " Source File:      $BACKUP_FILE"
if [ -n "$UPLOADS_FILE" ]; then
log " Uploads File:     $UPLOADS_FILE"
fi
log " Target Container: $TARGET_CONTAINER"
log " Target Database:  $TARGET_DB_NAME"
log "======================================================================"

if [ "$RESTORE_FORCE" != "true" ]; then
    log "WARNING: This will overwrite data in container '${TARGET_CONTAINER}'."
    log "Press Ctrl+C within 10 seconds to abort..."
    sleep 10
fi

# -----------------------------------------------------------------------------
# 2. Safety Pre-Restore Snapshot
# -----------------------------------------------------------------------------
if [ "$SKIP_SAFETY_SNAPSHOT" != "true" ]; then
    mkdir -p "${BACKUP_DIR}"
    SNAPSHOT_FILE="${BACKUP_DIR}/pre_restore_safety_snapshot_$(date +%Y%m%d_%H%M%S).sql.gz"
    log "[Safety] Creating pre-restore snapshot of current database state..."
    if docker exec -i "${TARGET_CONTAINER}" pg_dumpall -U "${TARGET_DB_USER}" | gzip -c > "${SNAPSHOT_FILE}"; then
        if [ -s "${SNAPSHOT_FILE}" ] && gzip -t "${SNAPSHOT_FILE}" 2>/dev/null; then
            log "[Safety] Pre-restore snapshot successfully saved to: ${SNAPSHOT_FILE}"
        else
            log "WARNING: Pre-restore snapshot was empty. Proceeding cautiously."
        fi
    else
        log "WARNING: Pre-restore snapshot failed. If this is a blank target, continue."
    fi
else
    log "[Safety] Pre-restore snapshot skipped (SKIP_SAFETY_SNAPSHOT=true)."
fi

# -----------------------------------------------------------------------------
# 3. Stop Microservices (if compose file is present)
# -----------------------------------------------------------------------------
if [ -f "$COMPOSE_FILE" ]; then
    log "[1/5] Stopping application microservices to prevent concurrent writes..."
    docker compose -f "$COMPOSE_FILE" stop \
        auth-service \
        commerce-service \
        customer-finance-service \
        inventory-procurement-service \
        production-service || true
    log "Application microservices stopped."
else
    log "[1/5] Compose file not present ($COMPOSE_FILE); skipping service shutdown."
fi

# -----------------------------------------------------------------------------
# 4. Decompress & Restore PostgreSQL Dump
# -----------------------------------------------------------------------------
log "[2/5] Restoring database from archive..."
# CRITICAL: Stream via docker exec -i without pseudo-TTY (-t)
gunzip -c "$BACKUP_FILE" | docker exec -i "${TARGET_CONTAINER}" psql -U "${TARGET_DB_USER}"

log "Database restoration script completed."

# -----------------------------------------------------------------------------
# 5. Post-Restore Schema & Integrity Verification
# -----------------------------------------------------------------------------
log "[3/5] Verifying restored schema objects..."

EXPECTED_SCHEMAS="auth_schema commerce_schema customer_schema finance_schema inventory_schema notification_schema procurement_schema production_schema"
MISSING_SCHEMAS=""
for s in $EXPECTED_SCHEMAS; do
    if ! docker exec -i "${TARGET_CONTAINER}" psql -U "${TARGET_DB_USER}" -d "${TARGET_DB_NAME}" -tAc \
        "SELECT 1 FROM information_schema.schemata WHERE schema_name='$s';" | grep -q "1"; then
        MISSING_SCHEMAS="$MISSING_SCHEMAS $s"
    fi
done

if [ -n "$MISSING_SCHEMAS" ]; then
    log "ERROR: Missing application schemas after restore: $MISSING_SCHEMAS" >&2
    exit 1
fi
log "All 8 application schemas verified present."

# Check table count
TABLE_COUNT=$(docker exec -i "${TARGET_CONTAINER}" psql -U "${TARGET_DB_USER}" -d "${TARGET_DB_NAME}" -tAc "
    SELECT count(*) FROM information_schema.tables
    WHERE table_schema IN ('auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
                           'inventory_schema', 'notification_schema', 'procurement_schema', 'production_schema')
       OR (table_schema = 'public' AND table_name LIKE 'form_%');
")
log "Total restored application tables: ${TABLE_COUNT} (expected: 58)"

if [ "${TABLE_COUNT}" -lt 58 ]; then
    log "WARNING: Restored table count (${TABLE_COUNT}) is below expected 58. Schema parity check recommended."
fi

# Check email_verified column parity
EMAIL_VERIFIED=$(docker exec -i "${TARGET_CONTAINER}" psql -U "${TARGET_DB_USER}" -d "${TARGET_DB_NAME}" -tAc "
    SELECT count(*) FROM information_schema.columns
    WHERE table_schema = 'auth_schema' AND table_name = 'users' AND column_name = 'email_verified';
")
if [ "${EMAIL_VERIFIED}" -eq 1 ]; then
    log "Verified: 'auth_schema.users.email_verified' column is present."
else
    log "WARNING: 'auth_schema.users.email_verified' missing. Apply V1 migration script."
fi

# -----------------------------------------------------------------------------
# 6. Restore Uploaded Files (if provided)
# -----------------------------------------------------------------------------
if [ -n "$UPLOADS_FILE" ]; then
    log "[4/5] Restoring customer uploads from $UPLOADS_FILE..."
    UPLOADS_VOL=$(docker volume ls -q --filter "name=uploads_data" | head -n 1 || true)
    if [ -n "${UPLOADS_VOL}" ]; then
        docker run --rm -v "${UPLOADS_VOL}:/data" -v "$(dirname "$UPLOADS_FILE")":/backup alpine \
            sh -c "rm -rf /data/* && tar -xzf /backup/$(basename "$UPLOADS_FILE") -C /data"
        log "Uploads volume restored into: ${UPLOADS_VOL}"
    else
        log "NOTICE: 'uploads_data' volume not found; skipping volume file extraction."
    fi
else
    log "[4/5] No uploads archive specified; skipping uploads restoration."
fi

# -----------------------------------------------------------------------------
# 7. Restart Microservices & Health Check
# -----------------------------------------------------------------------------
if [ -f "$COMPOSE_FILE" ]; then
    log "[5/5] Restarting application microservices..."
    docker compose -f "$COMPOSE_FILE" start \
        auth-service \
        commerce-service \
        customer-finance-service \
        inventory-procurement-service \
        production-service || true
    log "Waiting 15s for services to stabilize..."
    sleep 15
    docker compose -f "$COMPOSE_FILE" ps
else
    log "[5/5] No compose file; skipping microservice start."
fi

log "======================================================================"
log " RR Jaggery Traders — Restore Complete"
log " IMPORTANT POST-RESTORE ACTIONS:"
log " 1. If restoring onto a new host, re-run 'provision_runtime_roles.sh'"
log "    to configure runtime role passwords."
log " 2. Run 'verify_schema_parity.sql' to ensure table ownership and grants"
log "    are intact."
log "======================================================================"
