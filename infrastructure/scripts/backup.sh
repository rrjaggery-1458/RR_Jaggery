#!/usr/bin/env bash
# =============================================================================
# RR JAGGERY TRADERS — AUTOMATED DATABASE BACKUP SCRIPT
# Runs via cron on host VPS, compresses PostgreSQL cluster dump, and manages retention
# =============================================================================

set -eo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/rr-jaggery}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILENAME="rr_jaggery_db_${TIMESTAMP}.sql.gz"
TARGET_FILE="${BACKUP_DIR}/${BACKUP_FILENAME}"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Starting RR Jaggery PostgreSQL backup..."

# Execute dump using docker exec on postgres container
docker exec -t rr-postgres pg_dumpall -U postgres | gzip > "${TARGET_FILE}"

echo "[$(date)] Backup completed: ${TARGET_FILE} ($(du -sh "${TARGET_FILE}" | cut -f1))"

# Prune archives older than RETENTION_DAYS
find "${BACKUP_DIR}" -type f -name "rr_jaggery_db_*.sql.gz" -mtime +"${RETENTION_DAYS}" -exec rm -f {} \;
echo "[$(date)] Cleaned up backups older than ${RETENTION_DAYS} days."
