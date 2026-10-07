# RR JAGGERY TRADERS — PRODUCTION DEPLOYMENT RUNBOOK

> **CRITICAL CLARIFICATION: PRE-DEPLOYMENT vs. ACTUAL PRODUCTION DEPLOYMENT**  
> This repository is in the **PRE-DEPLOYMENT READINESS** state.  
> No public server has been provisioned, no DNS changes have been executed, and no public deployment has been pushed.  
> This runbook documents the step-by-step procedures required **when the business owner authorizes the live deployment phase**.

---

## 1. Prerequisites & Target Architecture

| Component | Specification / Requirement |
| :--- | :--- |
| **Server Target** | Oracle Cloud Infrastructure (OCI) Ampere A1 (ARM64) or Ubuntu 24.04 LTS (4 OCPU / 8–24 GB RAM) |
| **Operating System** | Linux (Ubuntu 24.04 LTS / 22.04 LTS aarch64 or x86_64) |
| **Container Engine** | Docker Engine 26.0+ & Docker Compose v2.27+ |
| **Network & Firewall** | Ports 80 (HTTP) & 443 (HTTPS) open. **All backend ports (5432, 6379, 8081–8088) strictly BLOCKED from public ingress**. |
| **DNS & CDN** | Cloudflare Proxy (Orange-clouded A-record pointing to VPS public IP) |
| **Architecture** | All containers (`eclipse-temurin:21-jre-alpine`, `postgres:16-alpine`, `redis:7-alpine`, `nginx:alpine`) are natively ARM64-compatible. |

---

## 2. Server Provisioning & Docker Installation (Ubuntu 24.04 LTS)

Run as root or sudo user on the target VPS:

```bash
# 1. Update OS packages
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release ufw

# 2. Configure Host Firewall (UFW)
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh          # Port 22
sudo ufw allow http         # Port 80
sudo ufw allow https        # Port 443
sudo ufw enable

# 3. Install Official Docker Engine
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 4. Enable and start Docker
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

---

## 3. Production Environment & Secrets Preparation

On the production server, create the secure project directory:

```bash
mkdir -p /opt/rr-jaggery
cd /opt/rr-jaggery
```

Clone the repository and prepare `.env`:

```bash
cp .env.example .env
chmod 600 .env
```

Generate cryptographically secure secrets and fill into `.env`:

```bash
# Generate 64-char hex JWT Secret:
openssl rand -hex 32

# Generate Database Password:
openssl rand -base64 24

# Generate First Admin Password (minimum 16 characters):
openssl rand -base64 16
```

Configure `.env` with:
- `DB_PASSWORD=<generated_db_password>`
- `JWT_SECRET=<generated_jwt_secret>`
- `BOOTSTRAP_ADMIN_USERNAME=admin@rrjaggery.com`
- `BOOTSTRAP_ADMIN_PASSWORD=<generated_admin_password>`
- `SPRING_PROFILES_ACTIVE=prod`
- `PRODUCTION_DOMAIN=rrjaggery.com`

---

## 4. Frontend & Backend Production Build

```bash
# 1. Build Frontend Static Assets
cd frontend
npm ci
npm run build
cd ..

# 2. Package Backend JARs
cd services
mvn -B clean package -DskipTests
cd ..
```

---

## 5. Production Docker Compose Startup

Start the production stack using `compose.production.yaml`:

```bash
docker compose -f compose.production.yaml up -d --build
```

Verify that all 11 containers start properly:

```bash
docker compose -f compose.production.yaml ps
```

Confirm that **only port 80 and 443** are bound to host interfaces. Backend services and PostgreSQL must show NO host ports published.

---

## 6. Cloudflare & SSL/TLS Configuration

1. In Cloudflare DNS:
   - Add `A` record: `rrjaggery.com` → `<VPS_PUBLIC_IP>` (Proxy status: Proxied / Orange cloud)
   - Add `CNAME` record: `www` → `rrjaggery.com` (Proxied)
2. In Cloudflare SSL/TLS:
   - Set encryption mode to **Full (Strict)** or **Full**.
3. Origin Certificates (Option A - Recommended):
   - In Cloudflare Dashboard → SSL/TLS → Origin Server → Create Certificate.
   - Save certificate to `/opt/rr-jaggery/infrastructure/nginx/ssl/fullchain.pem`.
   - Save private key to `/opt/rr-jaggery/infrastructure/nginx/ssl/privkey.pem`.
   - Set file permissions: `chmod 600 /opt/rr-jaggery/infrastructure/nginx/ssl/*.pem`.
4. Option B (Certbot Let's Encrypt):
   - Run Certbot with webroot `/var/www/certbot` as configured in `nginx.production.conf`.

---

## 7. Service Health Checks & Verification

Verify each service is healthy through the gateway:

```bash
# Gateway public check
curl -I https://rrjaggery.com/

# Backend actuator health endpoints (routed through internal network)
curl -s https://rrjaggery.com/api/v1/auth/health | jq .
curl -s https://rrjaggery.com/api/v1/commerce/health | jq .
curl -s https://rrjaggery.com/api/v1/customers/health | jq .
curl -s https://rrjaggery.com/api/v1/inventory/health | jq .
curl -s https://rrjaggery.com/api/v1/procurement/health | jq .
curl -s https://rrjaggery.com/api/v1/production/health | jq .
curl -s https://rrjaggery.com/api/v1/finance/health | jq .
curl -s https://rrjaggery.com/api/v1/notifications/health | jq .
```

---

## 8. Database Backup & Restore Procedures

### 8.1 Automated Nightly Backup

Create `/opt/rr-jaggery/scripts/backup.sh`:

```bash
#!/usr/bin/env bash
set -eo pipefail

BACKUP_DIR="/var/backups/rr-jaggery"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/rr_jaggery_backup_${TIMESTAMP}.sql.gz"

echo "[$(date)] Starting PostgreSQL backup..."
docker exec -t rr-postgres pg_dumpall -c -U postgres | gzip -9 > "$BACKUP_FILE"
chmod 600 "$BACKUP_FILE"

echo "[$(date)] Backup completed: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# Retention: Delete backups older than 14 days
find "$BACKUP_DIR" -type f -name "rr_jaggery_backup_*.sql.gz" -mtime +14 -delete
echo "[$(date)] Retention cleanup completed."
```

Schedule via host crontab (`crontab -e`):
```cron
0 2 * * * /opt/rr-jaggery/scripts/backup.sh >> /var/log/rr_backup.log 2>&1
```

### 8.2 Database Restore Procedure

To restore from a backup file:

```bash
# 1. Stop all backend services to prevent concurrent transactions
docker compose -f compose.production.yaml stop auth-service commerce-service customer-ledger-service inventory-service procurement-service production-service finance-service notification-service

# 2. Decompress and pipe SQL dump to PostgreSQL
gunzip < /var/backups/rr-jaggery/rr_jaggery_backup_YYYYMMDD_HHMMSS.sql.gz | docker exec -i rr-postgres psql -U postgres

# 3. Restart backend services
docker compose -f compose.production.yaml start

# 4. Verify data integrity
docker exec -i rr-postgres psql -U postgres -d rr_jaggery_db -c "SELECT count(*) FROM commerce_schema.products;"
```

---

## 9. Operational Lifecycle

### 9.1 Viewing Logs
```bash
# View aggregated real-time logs:
docker compose -f compose.production.yaml logs -f --tail=100

# View specific service logs:
docker compose -f compose.production.yaml logs -f auth-service
docker compose -f compose.production.yaml logs -f gateway
```

### 9.2 Rolling Update Procedure
```bash
# 1. Pull latest code
git pull origin main

# 2. Build updated components
cd frontend && npm run build && cd ..
cd services && mvn -B clean package -DskipTests && cd ..

# 3. Graceful container restart
docker compose -f compose.production.yaml up -d --no-deps --build <service-name>
```

### 9.3 Rollback Procedure
```bash
# Rollback git checkout
git checkout <previous-stable-commit-hash>

# Rebuild and restart
cd frontend && npm run build && cd ..
cd services && mvn -B clean package -DskipTests && cd ..
docker compose -f compose.production.yaml up -d --build
```

### 9.4 Emergency Shutdown
```bash
docker compose -f compose.production.yaml down
```
*(Named volumes `postgres_data`, `redis_data`, `uploads_data` remain 100% intact on the host).*

---

## 10. Smoke Tests Checklist Before Live Handoff

- [ ] HTTPS loads cleanly with valid SSL certificate in modern browsers.
- [ ] No mixed-content warnings or insecure resource errors in console.
- [ ] First Admin account bootstrap succeeded and password changed.
- [ ] Manager account creation verified under Admin Access.
- [ ] Customer registration, login, and token generation verified.
- [ ] Product catalogue renders properly with categories.
- [ ] Cart item additions, updates, and checkout calculate 5% GST and order total authoritatively.
- [ ] Order confirmation and tax invoice generation render complete invoice details.
- [ ] Legal pages (Terms, Privacy, Refund, Shipping, About, Contact) load without login and are mobile-responsive.
- [ ] PostgreSQL port 5432 and backend ports 8081–8088 are inaccessible from outside the VPS.
