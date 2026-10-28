#!/usr/bin/env bash
# =============================================================================
# RR JAGGERY TRADERS — RUNTIME ROLE PASSWORD PROVISIONING SCRIPT
# Script: provision_runtime_roles.sh
# Target: PostgreSQL 16 Runtime Service Roles (Linux / Docker VPS)
# =============================================================================

set -euo pipefail

CONTAINER_NAME="${1:-rr-postgres}"
DB_NAME="${2:-rr_jaggery_db}"
ADMIN_USER="${3:-postgres}"
ADMIN_PASSWORD="${DB_PASSWORD:-${POSTGRES_PASSWORD:-}}"

# 1. Validate all required passwords before attempting connection
for var in RR_AUTH_DB_PASSWORD RR_COMMERCE_DB_PASSWORD RR_CUSTFIN_DB_PASSWORD RR_INVPROC_DB_PASSWORD RR_PRODUCTION_DB_PASSWORD; do
    if [ -z "${!var:-}" ]; then
        echo "[ERROR] Required environment variable '$var' is missing or empty." >&2
        exit 1
    fi
    if [[ "${!var}" =~ [$'\r\n\x00'] ]]; then
        echo "[ERROR] Password in '$var' contains invalid control characters." >&2
        exit 1
    fi
done

escape_sql_literal() {
    local val="$1"
    local escaped="${val//\'/\'\'}"
    printf "'%s'" "$escaped"
}

AUTH_LIT=$(escape_sql_literal "$RR_AUTH_DB_PASSWORD")
COMM_LIT=$(escape_sql_literal "$RR_COMMERCE_DB_PASSWORD")
CUST_LIT=$(escape_sql_literal "$RR_CUSTFIN_DB_PASSWORD")
INV_LIT=$(escape_sql_literal "$RR_INVPROC_DB_PASSWORD")
PROD_LIT=$(escape_sql_literal "$RR_PRODUCTION_DB_PASSWORD")

SQL_STREAM=$(cat <<EOF
\set ON_ERROR_STOP on
SET standard_conforming_strings = on;
ALTER ROLE rr_auth WITH LOGIN PASSWORD $AUTH_LIT;
ALTER ROLE rr_commerce WITH LOGIN PASSWORD $COMM_LIT;
ALTER ROLE rr_custfin WITH LOGIN PASSWORD $CUST_LIT;
ALTER ROLE rr_invproc WITH LOGIN PASSWORD $INV_LIT;
ALTER ROLE rr_production WITH LOGIN PASSWORD $PROD_LIT;
EOF
)

# 2. Stream SQL commands into psql via stdin (never exposed in CLI args or process listings)
if [ -n "$ADMIN_PASSWORD" ]; then
    printf "%s\n" "$SQL_STREAM" | docker exec -i -e PGPASSWORD="$ADMIN_PASSWORD" "$CONTAINER_NAME" psql -U "$ADMIN_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q
else
    printf "%s\n" "$SQL_STREAM" | docker exec -i "$CONTAINER_NAME" psql -U "$ADMIN_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q
fi

echo "[SUCCESS] All 5 PostgreSQL runtime roles provisioned with LOGIN and secure passwords."

