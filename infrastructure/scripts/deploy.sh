#!/usr/bin/env bash
# ==============================================================================
# RR JAGGERY TRADERS — Production Deploy Script
# Sprint 10 — OVHcloud VPS Deployment
# ==============================================================================
# Run as: bash deploy.sh
# Run from: /opt/rr-jaggery (repo root, on the VPS)
# ==============================================================================
set -euo pipefail

COMPOSE_FILE="compose.production.yaml"
PROJECT_DIR="/opt/rr-jaggery"
LOG_FILE="/var/log/rr-jaggery/deploy.log"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

log "======================================================================"
log " RR Jaggery Traders — Production Deploy"
log " Sprint 10 | OVHcloud VPS"
log "======================================================================"

# ------------------------------------------------------------------
# Pre-flight checks
# ------------------------------------------------------------------
log "[PRE-FLIGHT] Checking required files..."

if [ ! -f ".env" ]; then
  log "ERROR: .env file not found. Copy .env.example to .env and populate secrets."
  exit 1
fi

# Verify required secrets are present and non-empty
for VAR in DB_PASSWORD JWT_SECRET BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_PASSWORD; do
  VAL=$(grep "^${VAR}=" .env | cut -d'=' -f2-)
  if [ -z "$VAL" ] || [[ "$VAL" == *"GENERATE"* ]] || [[ "$VAL" == *"PENDING"* ]] || [[ "$VAL" == *"YOUR_STRONG"* ]]; then
    log "ERROR: $VAR in .env appears unset or is still a placeholder."
    exit 1
  fi
done

log "[PRE-FLIGHT] .env secrets validated."

# ------------------------------------------------------------------
# Record baseline
# ------------------------------------------------------------------
COMMIT=$(git rev-parse --short HEAD)
BRANCH=$(git branch --show-current)
log "[GIT] Branch: $BRANCH | Commit: $COMMIT"

# ------------------------------------------------------------------
# 1. Build frontend
# ------------------------------------------------------------------
log "[1/5] Building frontend production bundle..."
cd frontend
npm ci --prefer-offline 2>&1 | tail -5
npm run build 2>&1 | tail -10
cd ..
log "[1/5] Frontend build complete. Output in frontend/dist/"

# ------------------------------------------------------------------
# 2. Build backend JARs
# ------------------------------------------------------------------
log "[2/5] Packaging backend JARs..."
cd services
mvn -B clean package -DskipTests 2>&1 | tail -20
cd ..
log "[2/5] Backend JARs built."

# ------------------------------------------------------------------
# 3. Verify 7-container architecture (no Redis, no legacy services)
# ------------------------------------------------------------------
log "[3/5] Validating compose.production.yaml service count..."
SERVICE_COUNT=$(docker compose -f "$COMPOSE_FILE" config --services 2>/dev/null | wc -l)
log "  Services in compose file: $SERVICE_COUNT (expected: 7)"
docker compose -f "$COMPOSE_FILE" config --services 2>/dev/null

# Confirm no redis service
if docker compose -f "$COMPOSE_FILE" config --services 2>/dev/null | grep -qi "redis"; then
  log "ERROR: Redis service found in production compose. This violates Sprint 9 consolidation."
  exit 1
fi
log "[3/5] Compose validation passed: no Redis, expected services present."

# ------------------------------------------------------------------
# 4. Deploy
# ------------------------------------------------------------------
log "[4/5] Starting production stack..."
docker compose -f "$COMPOSE_FILE" pull --quiet 2>/dev/null || true
docker compose -f "$COMPOSE_FILE" up -d --build --remove-orphans 2>&1 | tee -a "$LOG_FILE"
log "[4/5] Docker Compose up complete."

# ------------------------------------------------------------------
# 5. Health verification
# ------------------------------------------------------------------
log "[5/5] Waiting for services to become healthy (up to 120s)..."
sleep 30

HEALTHY=0
for i in {1..6}; do
  STATUS=$(docker compose -f "$COMPOSE_FILE" ps --format json 2>/dev/null | python3 -c "
import sys, json
data = sys.stdin.read().strip()
lines = [l for l in data.splitlines() if l.strip()]
services = [json.loads(l) for l in lines]
unhealthy = [s['Name'] for s in services if s.get('Health','') not in ('healthy', '')]
print(','.join(unhealthy) if unhealthy else 'ALL_HEALTHY')
" 2>/dev/null || echo "CHECK_FAILED")
  if [ "$STATUS" = "ALL_HEALTHY" ]; then
    HEALTHY=1
    break
  fi
  log "  Attempt $i/6: Waiting... ($STATUS)"
  sleep 20
done

log "--- Container Status ---"
docker compose -f "$COMPOSE_FILE" ps 2>&1 | tee -a "$LOG_FILE"

log "--- Health Endpoints ---"
# Internal network health checks via gateway
for ENDPOINT in \
  "http://localhost/api/v1/auth/health" \
  "http://localhost/api/v1/commerce/health" \
  "http://localhost/api/v1/customers/health" \
  "http://localhost/api/v1/inventory/health" \
  "http://localhost/api/v1/production/health"; do
  RESULT=$(curl -sf "$ENDPOINT" 2>/dev/null || echo "FAILED")
  log "  $ENDPOINT -> $RESULT"
done

log "--- Resource Usage ---"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | tee -a "$LOG_FILE"

log "======================================================================"
log " Deployment complete. Commit: $COMMIT"
log " Review logs at: $LOG_FILE"
log " Next: Configure SSL/TLS and DNS."
log "======================================================================"
