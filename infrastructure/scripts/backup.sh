#!/usr/bin/env bash
# =============================================================================
# RR JAGGERY TRADERS — AUTOMATED DATABASE & UPLOADS BACKUP SCRIPT
# Runs via cron or manually on host VPS.
# Performs pre-flight checks, streams uncorrupted compressed dumps, validates
# archive integrity and checksums, and applies safe retention pruning.
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# Configuration & Defaults
# -----------------------------------------------------------------------------
BACKUP_DIR="${BACKUP_DIR:-/var/backups/rr-jaggery}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
MIN_KEEP_BACKUPS="${MIN_KEEP_BACKUPS:-3}"
MIN_FREE_MB="${MIN_FREE_MB:-500}"
CONTAINER_NAME="${PG_CONTAINER:-rr-postgres}"
DB_USER="${PG_USER:-postgres}"
DB_NAME="${PG_DATABASE:-rr_jaggery_db}"
BACKUP_UPLOADS="${BACKUP_UPLOADS:-true}"
COMMERCE_CONTAINER="${COMMERCE_CONTAINER:-rr-commerce-service}"
UPLOADS_DIR="${UPLOADS_DIR:-/app/uploads}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
TARGET_DB_FILE="${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.sql.gz"
TMP_DB_FILE="${BACKUP_DIR}/.tmp_rr_jaggery_db_${TIMESTAMP}.sql.gz"
TARGET_UPLOADS_FILE="${BACKUP_DIR}/rr_jaggery_uploads_${TIMESTAMP}.tar.gz"
TMP_UPLOADS_FILE="${BACKUP_DIR}/.tmp_rr_jaggery_uploads_${TIMESTAMP}.tar.gz"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

# Clean up partial temporary files on failure or unexpected exit
cleanup_tmp() {
    rm -f "${BACKUP_DIR}/.tmp_"* 2>/dev/null || true
}
trap cleanup_tmp EXIT INT TERM

# -----------------------------------------------------------------------------
# 1. Pre-Flight Checks & Disk Space
# -----------------------------------------------------------------------------
mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}" 2>/dev/null || true

# Validate directory is safe and not root
if [ "${BACKUP_DIR}" = "/" ] || [ "${BACKUP_DIR}" = "/var" ] || [ "${BACKUP_DIR}" = "/home" ]; then
    log "ERROR: Dangerous BACKUP_DIR specified: ${BACKUP_DIR}. Aborting." >&2
    exit 1
fi

# Overwrite protection
if [ -e "${TARGET_DB_FILE}" ]; then
    log "ERROR: Target backup file already exists: ${TARGET_DB_FILE}. Aborting to avoid overwrite." >&2
    exit 1
fi

# Disk space check
if command -v df >/dev/null 2>&1; then
    FREE_MB=$(df -m "${BACKUP_DIR}" | awk 'NR==2 {print $4}')
    if [ -n "${FREE_MB}" ] && [ "${FREE_MB}" -lt "${MIN_FREE_MB}" ]; then
        log "ERROR: Insufficient disk space on ${BACKUP_DIR}: ${FREE_MB}MB free (minimum required: ${MIN_FREE_MB}MB)." >&2
        exit 1
    fi
fi

# Database container check
if ! docker ps --format '{{.Names}}' | grep -wq "${CONTAINER_NAME}"; then
    log "ERROR: PostgreSQL container '${CONTAINER_NAME}' is not running." >&2
    exit 1
fi

if ! docker exec "${CONTAINER_NAME}" pg_isready -U "${DB_USER}" -d "${DB_NAME}" >/dev/null 2>&1; then
    log "ERROR: PostgreSQL in '${CONTAINER_NAME}' is not accepting connections." >&2
    exit 1
fi

log "======================================================================"
log " RR Jaggery Traders — Backup Started"
log " Database: ${DB_NAME} (Container: ${CONTAINER_NAME})"
log " Target Dir: ${BACKUP_DIR}"
log "======================================================================"

# -----------------------------------------------------------------------------
# 2. Database Backup (PostgreSQL Cluster Dump)
# -----------------------------------------------------------------------------
log "[1/3] Generating PostgreSQL database dump..."

# CRITICAL: docker exec without -t (pseudo-TTY) to prevent binary stream corruption
docker exec -i "${CONTAINER_NAME}" pg_dumpall -U "${DB_USER}" | gzip -c > "${TMP_DB_FILE}"

# Verify temporary dump is non-empty
if [ ! -s "${TMP_DB_FILE}" ]; then
    log "ERROR: Database backup output is empty or failed to generate." >&2
    exit 1
fi

# Verify gzip archive integrity
if ! gzip -t "${TMP_DB_FILE}" 2>/dev/null; then
    log "ERROR: Database backup archive failed gzip integrity test." >&2
    exit 1
fi

# Atomic finalization
mv "${TMP_DB_FILE}" "${TARGET_DB_FILE}"
sha256sum "${TARGET_DB_FILE}" > "${TARGET_DB_FILE}.sha256"

DB_SIZE=$(du -sh "${TARGET_DB_FILE}" | cut -f1)
log "Database backup verified and completed: ${TARGET_DB_FILE} (${DB_SIZE})"

# -----------------------------------------------------------------------------
# 3. Uploaded Files Backup (Persistent Uploads Volume)
# -----------------------------------------------------------------------------
if [ "${BACKUP_UPLOADS}" = "true" ]; then
    log "[2/3] Generating application uploads backup..."
    if docker ps --format '{{.Names}}' | grep -wq "${COMMERCE_CONTAINER}"; then
        docker exec -i "${COMMERCE_CONTAINER}" tar -czf - -C "${UPLOADS_DIR}" . > "${TMP_UPLOADS_FILE}" 2>/dev/null || true
    fi

    # Fallback to direct volume inspection if container stream was empty
    if [ ! -s "${TMP_UPLOADS_FILE}" ]; then
        UPLOADS_VOL=$(docker volume ls -q --filter "name=uploads_data" | head -n 1 || true)
        if [ -n "${UPLOADS_VOL}" ]; then
            docker run --rm -v "${UPLOADS_VOL}:/data:ro" alpine tar -czf - -C /data . > "${TMP_UPLOADS_FILE}" 2>/dev/null || true
        fi
    fi

    if [ -s "${TMP_UPLOADS_FILE}" ] && gzip -t "${TMP_UPLOADS_FILE}" 2>/dev/null; then
        mv "${TMP_UPLOADS_FILE}" "${TARGET_UPLOADS_FILE}"
        sha256sum "${TARGET_UPLOADS_FILE}" > "${TARGET_UPLOADS_FILE}.sha256"
        UPLOADS_SIZE=$(du -sh "${TARGET_UPLOADS_FILE}" | cut -f1)
        log "Uploads backup completed: ${TARGET_UPLOADS_FILE} (${UPLOADS_SIZE})"
    else
        log "NOTICE: Uploads data was empty or not found; skipping uploads archive."
        rm -f "${TMP_UPLOADS_FILE}" 2>/dev/null || true
    fi
else
    log "[2/3] Uploads backup skipped (BACKUP_UPLOADS=false)."
fi

# -----------------------------------------------------------------------------
# 4. Safe Retention Pruning
# -----------------------------------------------------------------------------
log "[3/3] Evaluating backup retention..."

# Ensure we never prune if total backups <= MIN_KEEP_BACKUPS
TOTAL_BACKUPS=$(find "${BACKUP_DIR}" -maxdepth 1 -type f -name "rr_jaggery_db_*.sql.gz" | wc -l)

if [ "${TOTAL_BACKUPS}" -le "${MIN_KEEP_BACKUPS}" ]; then
    log "Retention pruning skipped: only ${TOTAL_BACKUPS} backup(s) exist (minimum preserved: ${MIN_KEEP_BACKUPS})."
else
    # Prune only matching archives older than RETENTION_DAYS
    PRUNED_COUNT=0
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            rm -f "$file" "${file}.sha256" 2>/dev/null || true
            PRUNED_COUNT=$((PRUNED_COUNT + 1))
        fi
    done < <(find "${BACKUP_DIR}" -maxdepth 1 -type f -name "rr_jaggery_db_*.sql.gz" -mtime +"${RETENTION_DAYS}")

    # Also prune matching uploads archives
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            rm -f "$file" "${file}.sha256" 2>/dev/null || true
        fi
    done < <(find "${BACKUP_DIR}" -maxdepth 1 -type f -name "rr_jaggery_uploads_*.tar.gz" -mtime +"${RETENTION_DAYS}")

    log "Retention pruning complete: ${PRUNED_COUNT} old archive(s) removed (retention threshold: ${RETENTION_DAYS} days)."
fi

log "======================================================================"
log " RR Jaggery Traders — Backup Finished Successfully"
log " NOTE: Local backups remain on the VPS host. For disaster recovery,"
log " sync '${BACKUP_DIR}' to an off-host vault (e.g., S3, Cloudflare R2, or remote SSH)."
log "======================================================================"
