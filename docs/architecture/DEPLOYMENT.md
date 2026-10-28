# RR JAGGERY TRADERS — PRODUCTION DEPLOYMENT RUNBOOK
# Sprint 10 Update: OVHcloud VPS | 5-Service Sprint 9 Architecture

> **CRITICAL CLARIFICATION: PRE-DEPLOYMENT vs. ACTUAL PRODUCTION DEPLOYMENT**  
> This repository is in the **PRE-DEPLOYMENT READINESS** state.  
> No public server has been provisioned, no DNS changes have been executed, and no public deployment has been pushed.  
> This runbook documents the step-by-step procedures required **when the business owner authorizes the live deployment phase**.

> **Sprint 9 Update:** The architecture has been consolidated from 8 JVM services to **5 JVM services**.  
> Redis has been decommissioned. The production Docker runtime must contain **exactly 7 active containers**.

---

## 1. Prerequisites & Target Architecture

| Component | Specification / Requirement |
| :--- | :--- |
| **Server Target** | OVHcloud VPS (VPS-1: 2 vCores, 4 GB RAM, 40 GB NVMe) |
| **Operating System** | Ubuntu 24.04 LTS (x86_64) |
| **Container Engine** | Docker Engine 26.0+ & Docker Compose v2.27+ |
| **Network & Firewall** | Ports 80 (HTTP) & 443 (HTTPS) open. **All backend ports (5432, 8081–8086) strictly BLOCKED from public ingress**. |
| **DNS & CDN** | Cloudflare Proxy (Orange-clouded A-record pointing to VPS public IP) |
| **Architecture** | All containers (`eclipse-temurin:21-jre-alpine`, `postgres:16-alpine`, `nginx:alpine`) are x86_64-compatible. |

### Required Container Inventory (Exactly 7)

| Container | Service | Internal Port |
| :--- | :--- | :--- |
| `rr-auth-service` | auth-service | 8081 |
| `rr-commerce-service` | commerce-service | 8082 |
| `rr-customer-finance-service` | customer-finance-service (customer + finance + notifications) | 8083 |
| `rr-inventory-procurement-service` | inventory-procurement-service (inventory + procurement) | 8084 |
| `rr-production-service` | production-service | 8086 |
| `rr-postgres` | PostgreSQL 16 | 5432 |
| `rr-gateway` | Nginx (public gateway) | 80/443 |

**NOT PRESENT:** Redis, finance-service, notification-service, customer-ledger-service, inventory-service, procurement-service

---

## 2. Server Provisioning & Docker Installation (Ubuntu 24.04 LTS)

Run the provisioning script as root or sudo user on the target VPS:

```bash
sudo bash /home/ubuntu/RR_Jaggery/infrastructure/scripts/vps-provision.sh
```

Or manually:

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
sudo ufw deny 5432/tcp      # PostgreSQL - NEVER public
sudo ufw deny 8081/tcp      # auth-service - NEVER public
sudo ufw deny 8082/tcp      # commerce-service - NEVER public
sudo ufw deny 8083/tcp      # customer-finance-service - NEVER public
sudo ufw deny 8084/tcp      # inventory-procurement-service - NEVER public
sudo ufw deny 8086/tcp      # production-service - NEVER public
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
mkdir -p /home/ubuntu/RR_Jaggery
cd /home/ubuntu/RR_Jaggery
```

Clone the repository and prepare `.env`:

```bash
git clone <repo-url> .
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
- `PRODUCTION_DOMAIN=rrdentalneeds.online`  ← temporary testing domain; change to `rrjaggery.com` when permanent domain is ready

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

Verify that exactly **7 containers** start:

```bash
docker compose -f compose.production.yaml ps
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Confirm that **only port 80 and 443** are bound to public host interfaces. Backend services and PostgreSQL must show NO host ports published.

**Verify NO Redis:**
```bash
docker ps | grep redis   # Must return empty
```

---

## 6. Cloudflare & SSL/TLS Configuration

1. In Cloudflare DNS:
   - Add `A` record: `rrdentalneeds.online` → `<VPS_PUBLIC_IP>` (Proxy status: Proxied / Orange cloud)
   - Add `CNAME` record: `www` → `rrdentalneeds.online` (Proxied)
2. In Cloudflare SSL/TLS:
   - Set encryption mode to **Full (Strict)** or **Full**.
3. Origin Certificates (Option A - Recommended):
   - In Cloudflare Dashboard → SSL/TLS → Origin Server → Create Certificate.
   - Save certificate to `/home/ubuntu/RR_Jaggery/infrastructure/nginx/ssl/fullchain.pem`.
   - Save private key to `/home/ubuntu/RR_Jaggery/infrastructure/nginx/ssl/privkey.pem`.
   - Set file permissions: `chmod 600 /home/ubuntu/RR_Jaggery/infrastructure/nginx/ssl/*.pem`.
4. Option B (Certbot Let's Encrypt):
   - Run Certbot with webroot `/var/www/certbot` as configured in `nginx.production.conf`.

---

## 7. Service Health Checks & Verification

Verify each service is healthy through the gateway:

```bash
# Gateway public check
curl -I https://rrdentalneeds.online/

# Backend health endpoints (routed through Nginx)
curl -s https://rrdentalneeds.online/api/v1/auth/health | python3 -m json.tool
curl -s https://rrdentalneeds.online/api/v1/commerce/health | python3 -m json.tool
curl -s https://rrdentalneeds.online/api/v1/customers/health | python3 -m json.tool
curl -s https://rrdentalneeds.online/api/v1/inventory/health | python3 -m json.tool
curl -s https://rrdentalneeds.online/api/v1/production/health | python3 -m json.tool
```

**REMOVED (legacy):** `/api/v1/finance`, `/api/v1/notifications`, `/api/v1/procurement` now route through consolidated services.

---

## 8. Database Backup & Restore Procedures

### 8.1 Automated Nightly Backup

Install cron job on the host:
```bash
chmod +x /home/ubuntu/RR_Jaggery/infrastructure/scripts/backup.sh
crontab -e
# Add:
0 2 * * * /home/ubuntu/RR_Jaggery/infrastructure/scripts/backup.sh >> /var/log/rr-jaggery/backup.log 2>&1
```

The backup script:
- Dumps all PostgreSQL data using `pg_dumpall`
- Compresses with gzip (-9)
- Stores in `/var/backups/rr-jaggery/`
- Retains 14 days of backups
- Verifies file is non-empty

### 8.2 Database Restore Procedure

```bash
# Usage: bash restore.sh /path/to/backup.sql.gz
bash /home/ubuntu/RR_Jaggery/infrastructure/scripts/restore.sh \
  /var/backups/rr-jaggery/rr_jaggery_backup_YYYYMMDD_HHMMSS.sql.gz
```

The restore script:
1. Stops all 5 JVM services (prevents concurrent writes)
2. Decompresses and pipes SQL dump to PostgreSQL
3. Verifies all 8 schemas are present
4. Restarts JVM services
5. Reports container status

---

## 9. Operational Lifecycle

### 9.1 Viewing Logs
```bash
# View aggregated real-time logs:
docker compose -f compose.production.yaml logs -f --tail=100

# View specific service logs:
docker compose -f compose.production.yaml logs -f auth-service
docker compose -f compose.production.yaml logs -f gateway
docker compose -f compose.production.yaml logs -f customer-finance-service
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
*(Named volumes `postgres_data`, `uploads_data` remain 100% intact on the host).*

### 9.5 Resource Monitoring
```bash
docker stats --no-stream
free -h
df -h
docker system df
```

### 9.6 Docker Cleanup (Safe)
```bash
# Remove unused images (NOT active container images or volumes)
docker image prune -af --filter "until=168h"
docker container prune -f
# NEVER: docker volume prune (would delete postgres_data)
```

---

## 10. Smoke Tests Checklist Before Live Handoff

- [ ] HTTPS loads cleanly with valid SSL certificate in modern browsers.
- [ ] No mixed-content warnings or insecure resource errors in console.
- [ ] First Admin account bootstrap succeeded and password changed.
- [ ] Manager account creation verified under Admin Access.
- [ ] Customer registration, login, and token generation verified.
- [ ] Product catalogue renders properly with categories.
- [ ] Cart item additions, updates, and checkout calculate order total authoritatively (no GST — RR Jaggery is NOT GST registered).
- [ ] Order confirmation and tax invoice generation render complete invoice details.
- [ ] Legal pages (Terms, Privacy, Refund, Shipping, About, Contact) load without login and are mobile-responsive.
- [ ] PostgreSQL port 5432 and backend ports 8081–8086 are inaccessible from outside the VPS.
- [ ] `docker ps | grep redis` returns empty (no Redis container).
- [ ] System Health page shows 5 services / 7 containers.
