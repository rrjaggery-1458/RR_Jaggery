#!/usr/bin/env bash
# ==============================================================================
# RR JAGGERY TRADERS — PostgreSQL Restore Script
# Sprint 10 — Production Operations
# ==============================================================================
# Usage: bash restore.sh /var/backups/rr-jaggery/rr_jaggery_db_YYYYMMDD_HHMMSS.sql.gz
# ==============================================================================
set -euo pipefail

BACKUP_FILE="${1:-}"
CONTAINER_NAME="rr-postgres"
COMPOSE_FILE="/opt/rr-jaggery/compose.production.yaml"
DB_USER="postgres"
LOG_FILE="/var/log/rr-jaggery/restore.log"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

if [ -z "$BACKUP_FILE" ]; then
  echo "Usage: $0 /path/to/backup.sql.gz"
  exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
  log "ERROR: Backup file not found: $BACKUP_FILE"
  exit 1
fi

log "======================================================================"
log " RR Jaggery Traders — PostgreSQL RESTORE"
log " File: $BACKUP_FILE"
log "======================================================================"
log "WARNING: This will overwrite the current database contents."
log "Press Ctrl+C within 10 seconds to abort..."
sleep 10

# ------------------------------------------------------------------
# 1. Stop JVM services to prevent concurrent writes during restore
# ------------------------------------------------------------------
log "[1/5] Stopping JVM services..."
docker compose -f "$COMPOSE_FILE" stop \
  auth-service \
  commerce-service \
  customer-finance-service \
  inventory-procurement-service \
  production-service
log "JVM services stopped."

# ------------------------------------------------------------------
# 2. Decompress and restore
# ------------------------------------------------------------------
log "[2/5] Restoring database from $BACKUP_FILE..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER"
log "Restore SQL complete."

# ------------------------------------------------------------------
# 3. Verify schemas
# ------------------------------------------------------------------
log "[3/5] Verifying schemas after restore..."
docker exec -t "$CONTAINER_NAME" psql -U "$DB_USER" -d rr_jaggery_db -c "\dn" | tee -a "$LOG_FILE"

EXPECTED_SCHEMAS="auth_schema commerce_schema customer_schema finance_schema inventory_schema notification_schema procurement_schema production_schema"
MISSING=""
for SCHEMA in $EXPECTED_SCHEMAS; do
  if ! docker exec -t "$CONTAINER_NAME" psql -U "$DB_USER" -d rr_jaggery_db -tAc \
    "SELECT 1 FROM information_schema.schemata WHERE schema_name='$SCHEMA'" | grep -q "1"; then
    MISSING="$MISSING $SCHEMA"
  fi
done
if [ -n "$MISSING" ]; then
  log "WARNING: Missing schemas after restore:$MISSING"
else
  log "All 8 expected schemas present."
fi

# ------------------------------------------------------------------
# 4. Restart JVM services
# ------------------------------------------------------------------
log "[4/5] Restarting JVM services..."
docker compose -f "$COMPOSE_FILE" start \
  auth-service \
  commerce-service \
  customer-finance-service \
  inventory-procurement-service \
  production-service
log "JVM services restarted."

# ------------------------------------------------------------------
# 5. Post-restore health check
# ------------------------------------------------------------------
log "[5/5] Waiting 30s for services to stabilize..."
sleep 30
log "Container status:"
docker compose -f "$COMPOSE_FILE" ps | tee -a "$LOG_FILE"
log "Restore complete. Verify application behavior manually."
