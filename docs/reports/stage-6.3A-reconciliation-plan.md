# RR Jaggery — Stage 6.3A: Local-Only Configuration Reconciliation Plan

> **Document Status:** Standalone Planning Document & Engineering Roadmap  
> **Repository:** `E:\RR Jaggery`  
> **Branch:** `main`  
> **Baseline Commit:** `e582aa9ca329734f03bdf671af621e3e9decfae5`  
> **Document Date:** 2026-10-28  
> **Operational Boundary:** **PLANNING ONLY — STRICTLY LOCAL & READ-ONLY**. No VPS access, no deployment, no schema modifications, no credentials created.

---

## 1. Executive Summary & Objective

The Stage 6.2 read-only audit established the live baseline of the production OVHcloud VPS (`15.235.246.77`). While the live system is actively serving HTTPS traffic on `rrdentalneeds.online` across 7 containers, significant divergence exists between the committed repository and the live server.

This document defines the comprehensive, evidence-based reconciliation plan to align the repository, deployment workflows, and server state safely. It addresses all 6 blockers identified in Stage 6.2 without risking service disruption, data loss, or secret leakage.

### Summary of Live VPS Baseline (Stage 6.2 Findings)
- **Host:** OVHcloud VPS-1, Ubuntu 24.04.5 LTS, 2 vCPUs, 3.7 GiB RAM, 0 B swap, 40 GB NVMe (18% used).
- **Canonical Server Path:** `/home/ubuntu/RR_Jaggery/` (the repository scripts and CI erroneously target `/opt/rr-jaggery/`).
- **Deployed Revision:** Commit `8931e2a` (8 commits behind repository `HEAD` `e582aa9`).
- **Uncommitted Server Edits:** Server has local modifications in Compose, Nginx SSL paths, and `application.yml`.
- **Database State:** PostgreSQL 16.15 running. All 5 schema columns from `V1_least_privilege_hardening.sql` are **already present**. However, all 6 least-privilege runtime/migrator roles are **missing**, and all 5 services currently connect as superuser `postgres`.
- **Certificates & TLS:** Valid Let's Encrypt certificate expiring Jan 25, 2027 stored in Docker volume `rr_jaggery_ssl_certs` under `/etc/nginx/ssl/live/rrdentalneeds.online/`. No automated renewal cron/timer exists.
- **Backups:** No nightly cron job installed; default directory `/var/backups/rr-jaggery` absent; one manual dump exists at `/home/ubuntu/backups/`.
- **Gateway Health:** `rr-gateway` container reported `unhealthy` because internal healthcheck fails TLS validation on `localhost`.

---

## 2. Workstream Details & Proposed Solutions

### Workstream A: Deployment Directory & Transfer Safety

#### 1. Canonical Deployment Path Evaluation
- **Observed VPS Path:** `/home/ubuntu/RR_Jaggery/`
- **Repository Expected Path:** `/opt/rr-jaggery/`

| Option | Pros | Cons | Recommendation |
|---|---|---|---|
| **Option 1: Adopt `/home/ubuntu/RR_Jaggery/`** | Zero server-side file migration; existing Docker volumes and bind mounts remain undisturbed; aligns with existing user permissions (`ubuntu`). | Non-standard Linux production path compared to `/opt`. | **RECOMMENDED** |
| **Option 2: Migrate to `/opt/rr-jaggery/`** | Standard FHS convention. | High risk: requires stopping containers, copying active git tree and `.env`, resetting file ownership, recreating bind mounts, and updating crontabs. | REJECTED |

**Action Items for Future Implementation (Local Repo):**
1. Update [.github/workflows/ci.yml](file:///e:/RR%20Jaggery/.github/workflows/ci.yml) (lines 197, 202, 221) from `/opt/rr-jaggery/` to `/home/ubuntu/RR_Jaggery/`.
2. Update [infrastructure/scripts/deploy.sh](file:///e:/RR%20Jaggery/infrastructure/scripts/deploy.sh) (`PROJECT_DIR="/home/ubuntu/RR_Jaggery"`).
3. Update [infrastructure/scripts/restore.sh](file:///e:/RR%20Jaggery/infrastructure/scripts/restore.sh) (`COMPOSE_FILE` default to `/home/ubuntu/RR_Jaggery/compose.production.yaml`).
4. Update [infrastructure/scripts/vps-provision.sh](file:///e:/RR%20Jaggery/infrastructure/scripts/vps-provision.sh) and [docs/architecture/DEPLOYMENT.md](file:///e:/RR%20Jaggery/docs/architecture/DEPLOYMENT.md).

#### 2. Transfer Safety & `rsync --delete` Elimination
- **Risk:** `rsync -az --delete` to `/home/ubuntu/RR_Jaggery/` would wipe:
  - Live `.env`, `.env.save`, `.env.save.1`.
  - Manual backups in `/home/ubuntu/backups/`.
  - Server-side untracked configurations (`compose.http-test.override.yaml`, `infrastructure/nginx/nginx.http-test.conf`).
  - Active runtime log files.
- **Proposed Safe Transfer Policy:**
  1. **Strict Exclusion List:**
     ```bash
     --exclude '.git' \
     --exclude '.env*' \
     --exclude 'node_modules' \
     --exclude 'services/*/target/classes' \
     --exclude 'backups/' \
     --exclude 'compose.http-test.override.yaml' \
     --exclude 'infrastructure/nginx/nginx.http-test.conf'
     ```
  2. **Mandatory Dry-Run Review:** For any future file sync, run `rsync -azvn ...` first and require explicit approval of the dry-run diff.
  3. **No `--delete` on Initial Sync:** For the first reconciliation deployment, completely omit `--delete` to guarantee no server-side files are removed.

#### 3. Reconciliation of Server-Side Modifications
The VPS contains 4 specific modifications that must be merged locally into the repository so that repository checkouts cleanly supersede the server state without conflicts:
1. `compose.production.yaml`: Server wraps `SMTP_USERNAME: "${SMTP_USERNAME}"` and `SMTP_PASSWORD: "${SMTP_PASSWORD}"` in quotes. Update local `compose.production.yaml` to match.
2. `infrastructure/nginx/nginx.production.conf`: Server points certificate directives to `/etc/nginx/ssl/live/rrdentalneeds.online/`. Merge into local config (see Workstream B).
3. `services/auth-service/src/main/resources/application.yml`: Server added `bootstrap.admin` properties. Merge into local configuration.
4. Backup existing server files: Prior to syncing, create a server-side timestamped safety copy: `cp -a /home/ubuntu/RR_Jaggery /home/ubuntu/RR_Jaggery_pre_stage6_backup`.

---

### Workstream B: Nginx & TLS Reconciliation

#### 1. Mismatch Analysis
- **Live Server Path:** In Docker volume `rr_jaggery_ssl_certs`, Certbot places files in:
  `/etc/nginx/ssl/live/rrdentalneeds.online/fullchain.pem`
  `/etc/nginx/ssl/live/rrdentalneeds.online/privkey.pem`
- **Repository Configuration:** [infrastructure/nginx/nginx.production.conf](file:///e:/RR%20Jaggery/infrastructure/nginx/nginx.production.conf#L184-L185) currently points to:
  `ssl_certificate /etc/nginx/ssl/live/fullchain.pem;`
  `ssl_certificate_key /etc/nginx/ssl/live/privkey.pem;`

#### 2. Safe Correction
- **Local Edit Plan:** In `infrastructure/nginx/nginx.production.conf`:
  ```nginx
  # Production Let's Encrypt TLS Certificates
  ssl_certificate     /etc/nginx/ssl/live/rrdentalneeds.online/fullchain.pem;
  ssl_certificate_key /etc/nginx/ssl/live/rrdentalneeds.online/privkey.pem;
  ```
- **Modernize Nginx Directives:** Replace deprecated `listen 443 ssl http2;` (lines 178-179) with:
  ```nginx
  listen 443 ssl;
  listen [::]:443 ssl;
  http2 on;
  ```
- **Pre-Reload Verification:** Mandate running `docker exec rr-gateway nginx -t` before any container reload or restart.
- **Certbot Renewal Automation:**
  - The live certificate expires **January 25, 2027**.
  - Plan a host-level systemd timer or cron job for Certbot renewal using webroot `/var/www/certbot` (already mapped to `rr_jaggery_certbot_data` volume in `rr-gateway`):
    ```bash
    0 3 * * * certbot renew --webroot -w /var/lib/docker/volumes/rr_jaggery_certbot_data/_data --post-hook "docker exec rr-gateway nginx -s reload"
    ```

---

### Workstream C: PostgreSQL Least-Privilege Migration

#### 1. Audit Findings & Gap Analysis
- **Columns Already Present:** The 5 columns from `V1_least_privilege_hardening.sql` (`users.email_verified`, `offline_orders.delivery_charges`, `offline_orders.order_type`, `employees.notes`, `employees.updated_at`) were verified present in the database.
- **Roles Missing:** Dedicated runtime roles (`rr_auth`, `rr_commerce`, `rr_custfin`, `rr_invproc`, `rr_production`) and `rr_migrator` are completely absent. All 5 services currently connect as `postgres`.
- **Compose Requirement:** Current repository [compose.production.yaml](file:///e:/RR%20Jaggery/compose.production.yaml) has mandatory environment variables:
  `DB_PASSWORD: ${RR_AUTH_DB_PASSWORD:?RR_AUTH_DB_PASSWORD must be provided}`
  Deploying latest Compose before provisioning roles will crash all containers immediately.

#### 2. Step-by-Step Migration Sequence
To execute in a future maintenance window:

```
[Step 1: Snapshot] ──> [Step 2: Isolated Drill] ──> [Step 3: DDL & Grants] ──> [Step 4: Provision Passwords] ──> [Step 5: .env Config] ──> [Step 6: Role Login Check] ──> [Step 7: Service Switch]
```

1. **Step 1: Production Safety Snapshot**
   - Execute a fresh `pg_dumpall` backup:
     `docker exec -i rr-postgres pg_dumpall -U postgres | gzip -c > /home/ubuntu/backups/pre_role_migration_$(date +%Y%m%d_%H%M%S).sql.gz`
   - Archive `/var/lib/docker/volumes/rr_jaggery_uploads_data/_data` into `/home/ubuntu/backups/`.
   - Verify archive sizes and gzip integrity.
2. **Step 2: Isolated Rehearsal**
   - Rehearse `V1_least_privilege_hardening.sql` and `provision_runtime_roles.sh` against an isolated PostgreSQL container or test database.
   - Confirm that existing columns are skipped idempotently without errors (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS`).
3. **Step 3: Execute `V1_least_privilege_hardening.sql` on Live PostgreSQL**
   - Connect as `postgres` superuser:
     `docker exec -i rr-postgres psql -U postgres -d rr_jaggery_db < infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql`
   - This creates `rr_migrator` and the 5 runtime roles as `NOLOGIN`, transfers schema ownership, and grants schema `USAGE`, table `SELECT, INSERT, UPDATE, DELETE`, and sequence `USAGE`.
   - Grants special native SQL permissions needed by `production-service` and `auth-service`.
4. **Step 4: Execute `provision_runtime_roles.sh`**
   - Generate 5 cryptographically strong random passwords (`openssl rand -base64 24`).
   - Run `infrastructure/docker/postgres/provision_runtime_roles.sh` with the generated passwords passed via process environment.
   - Script runs `ALTER ROLE rr_* WITH LOGIN PASSWORD '...';`.
5. **Step 5: Populate Live `.env`**
   - Append `RR_AUTH_DB_USER=rr_auth`, `RR_AUTH_DB_PASSWORD=...`, etc. into `/home/ubuntu/RR_Jaggery/.env`.
   - Set `HIBERNATE_DDL_AUTO=validate`.
   - Set file permissions: `chmod 600 .env`.
6. **Step 6: Pre-Flight Role Login Verification**
   - Query PostgreSQL directly as each runtime role *before* restarting applications:
     ```bash
     docker exec -i rr-postgres psql -U rr_auth -d rr_jaggery_db -c "SELECT current_user, count(*) FROM auth_schema.users;"
     docker exec -i rr-postgres psql -U rr_commerce -d rr_jaggery_db -c "SELECT current_user, count(*) FROM commerce_schema.orders;"
     ```
   - Must return HTTP/SQL success for all 5 roles.
7. **Step 7: Service Restart & Schema Validation**
   - Restart containers sequentially: `docker compose -f compose.production.yaml up -d`.
   - Tail service logs to verify Hibernate schema validation passes cleanly:
     `docker compose -f compose.production.yaml logs -f | grep -i "schema-validation"`
8. **Rollback Plan:**
   - If any service fails startup validation, revert `.env` variables to `DB_USER=postgres` and `DB_PASSWORD=<postgres_password>`, then restart containers. Service returns to legacy working state in < 30 seconds.

---

### Workstream D: Backup & Restore Readiness

#### 1. Audit Findings
- Default path `/var/backups/rr-jaggery` absent; no automated backup crontab installed.
- One manual dump (`rr_jaggery_20261027_095949.dump`, 146 KB) located in `/home/ubuntu/backups/`.
- Live upload volume `rr_jaggery_uploads_data` contains active product images (~4.2 MB).

#### 2. Reconciliation Plan
1. **Directory Standardization:**
   - Standardize backup path to `/home/ubuntu/backups/` or create `/var/backups/rr-jaggery/` owned by `ubuntu` (`chmod 700`).
   - Update `BACKUP_DIR` default in [infrastructure/scripts/backup.sh](file:///e:/RR%20Jaggery/infrastructure/scripts/backup.sh#L14) and [infrastructure/scripts/restore.sh](file:///e:/RR%20Jaggery/infrastructure/scripts/restore.sh#L30) to match.
2. **Cron Automation:**
   - Install user crontab entry for user `ubuntu`:
     ```cron
     0 2 * * * /home/ubuntu/RR_Jaggery/infrastructure/scripts/backup.sh >> /home/ubuntu/backups/backup.log 2>&1
     ```
3. **Restore Script Compatibility:**
   - Update `COMPOSE_FILE` default in `restore.sh` (line 27) from `/opt/rr-jaggery/compose.production.yaml` to `/home/ubuntu/RR_Jaggery/compose.production.yaml`.
4. **Distinction Between Checksum & Restore Testing:**
   - Validating `sha256sum -c` verifies file integrity only.
   - Plan a full restore rehearsal on an isolated container to verify table count (8 schemas, 30+ tables) and sequence alignment.

---

### Workstream E: Resource & Application Health Readiness

#### 1. Audit Findings
- VPS has **0 B swap space**. Available RAM is 1.4 GiB out of 3.7 GiB.
- The 7 containers currently consume ~1.82 GiB.
- [infrastructure/docker/Dockerfile.service](file:///e:/RR%20Jaggery/infrastructure/docker/Dockerfile.service#L12) specifies:
  `ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"`
  Because [compose.production.yaml](file:///e:/RR%20Jaggery/compose.production.yaml) sets no container memory limits (`mem_limit`), each JVM calculates 75% against host RAM (2.8 GiB). A spike in one service could exhaust all available host memory and trigger the kernel OOM killer.
- [infrastructure/scripts/deploy.sh](file:///e:/RR%20Jaggery/infrastructure/scripts/deploy.sh) compiles npm and Maven on the VPS (lines 56–68), risking OOM crashes while Docker is running.

#### 2. Resource Hardening Plan
1. **Configure Swap Space on VPS (Future Server Action):**
   - Create a 2 GB swapfile on the NVMe disk to provide a memory safety buffer:
     ```bash
     sudo fallocate -l 2G /swapfile
     sudo chmod 600 /swapfile
     sudo mkswap /swapfile
     sudo swapon /swapfile
     echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
     ```
2. **Container Memory Limits (Local Repo Plan):**
   - Add explicit `mem_limit` and JVM heap limits in `compose.production.yaml` or container environment:
     - `auth-service`: `mem_limit: 450m`, `-Xmx350m`
     - `commerce-service`: `mem_limit: 500m`, `-Xmx400m`
     - `customer-finance-service`: `mem_limit: 550m`, `-Xmx450m`
     - `inventory-procurement-service`: `mem_limit: 450m`, `-Xmx350m`
     - `production-service`: `mem_limit: 450m`, `-Xmx350m`
     - `postgres`: `mem_limit: 256m`
     - `gateway`: `mem_limit: 64m`
     - Total capped footprint: ~2.7 GiB, leaving 1.0 GiB headroom for OS and buffers.
3. **Eliminate Redundant On-Host Builds:**
   - Update `deploy.sh` to use pre-built JARs and frontend bundle from GitHub Actions or transfer artifacts directly, removing `npm run build` and `mvn package` from the live server.
4. **Fix Gateway Healthcheck:**
   - In `compose.production.yaml` (line 231), update `gateway` healthcheck:
     ```yaml
     healthcheck:
       test: ["CMD-SHELL", "wget -q --spider https://127.0.0.1/api/v1/auth/health --no-check-certificate || exit 1"]
     ```

---

### Workstream F: GitHub Actions Deployment Safety

#### 1. Audit Findings
- [ci.yml](file:///e:/RR%20Jaggery/.github/workflows/ci.yml) has an active hard lock: `if: false && ...`.
- `Configure SSH` step uses `ssh-keyscan -T 10 -H "$VPS_HOST"` (Trust-On-First-Use) without comparing against a pinned fingerprint.
- The verified host fingerprint on the VPS is:
  `ED25519 SHA256:Qv5L5HzAl22I//3L5n5oB5h0cZO1arcWLOIaVxtxwmg`

#### 2. Future Pipeline Hardening Plan
1. **Maintain Hard Lock:** Retain `if: false && ...` throughout planning and rehearsal.
2. **Pinned Host-Key Verification:**
   - Store the verified public host key in a GitHub secret `VPS_KNOWN_HOSTS` or compare the fingerprint in the step:
     ```bash
     mkdir -p ~/.ssh
     chmod 700 ~/.ssh
     printf '%s\n' "$VPS_KNOWN_HOSTS" > ~/.ssh/known_hosts
     chmod 644 ~/.ssh/known_hosts
     ```
3. **Correct Target Path:** Update rsync target to `${{ secrets.VPS_USER }}@${{ secrets.VPS_HOST }}:/home/ubuntu/RR_Jaggery/`.
4. **Isolated Manual Approval:** Retain `workflow_dispatch` with `confirm_production_deploy: true` as the sole trigger mechanism once the hard lock is eventually lifted.

---

## 3. Prioritized Decision Register

| ID | Priority | Finding / Evidence | Affected Repository File(s) | Proposed Resolution | Prerequisites / Dependencies | Validation Criteria | Rollback Strategy | Scope |
|---|---|---|---|---|---|---|---|---|
| **D01** | **CRITICAL** | Server repository is at `/home/ubuntu/RR_Jaggery/`, but repo/CI hardcode `/opt/rr-jaggery/`. | `.github/workflows/ci.yml`, `deploy.sh`, `restore.sh`, `vps-provision.sh`, `DEPLOYMENT.md` | Adopt `/home/ubuntu/RR_Jaggery/` as canonical path across all repository files. | None (local edits). | `grep -rn "/opt/rr-jaggery"` returns 0 active hits in codebase. | Revert local edits. | Local Repo |
| **D02** | **CRITICAL** | `rsync -az --delete` risks wiping server `.env`, backups, and untracked configs. | `.github/workflows/ci.yml` | Remove `--delete` on initial deployment; add explicit exclusions for `.env*`, `backups/`, and test configs. | D01 | Dry-run `rsync -azvn` demonstrates zero deleted files. | Revert rsync flags. | Local Repo |
| **D03** | **HIGH** | Repo Nginx config references `/etc/nginx/ssl/live/fullchain.pem`, while Let's Encrypt volume uses `.../live/rrdentalneeds.online/fullchain.pem`. | `infrastructure/nginx/nginx.production.conf` | Update certificate paths to match the domain-specific directory; modernize `http2` directives. | None | `docker exec rr-gateway nginx -t` returns success. | Revert Nginx config edit. | Local Repo |
| **D04** | **HIGH** | Least-privilege roles missing from PostgreSQL; `.env` lacks `RR_*_DB_PASSWORD`. | `compose.production.yaml`, `init-schemas.sql`, `V1_least_privilege_hardening.sql` | Execute role DDL, provision role passwords via `provision_runtime_roles.sh`, and populate `.env` before updating Compose. | D06 (fresh backup), D05 | `psql -U rr_*` connects successfully for all 5 roles. | Revert `.env` to `DB_USER=postgres`. | Future Server Action |
| **D05** | **HIGH** | Server contains uncommitted edits in Compose and `application.yml` (`SMTP_USERNAME`, `bootstrap.admin`). | `compose.production.yaml`, `services/auth-service/.../application.yml` | Merge server environment improvements into local repository files. | None | `git diff` against server files cleanly reconciled. | Keep server backups. | Local Repo |
| **D06** | **HIGH** | No active automated backup schedule; default backup path missing. | `infrastructure/scripts/backup.sh`, `restore.sh` | Update backup path to `/home/ubuntu/backups/`; install user crontab for nightly backups. | D01 | `backup.sh` runs manually and outputs valid `.sql.gz` and `.sha256`. | Remove crontab entry. | Local & Server |
| **D07** | **HIGH** | Gateway container reported unhealthy due to localhost TLS check failure. | `compose.production.yaml` | Update gateway healthcheck test to spider with `--no-check-certificate` or query HTTP directly. | None | `docker ps` reports `rr-gateway` status `healthy`. | Revert healthcheck syntax. | Local Repo |
| **D08** | **MEDIUM** | VPS has 0 B swap; JVM services lack memory limits, risking host OOM crash. | `compose.production.yaml`, `Dockerfile.service` | Create 2 GB swap on VPS; configure container `mem_limit` and JVM heap caps (~2.7 GiB total). | None | `free -h` shows 2.0 GiB swap; container stats show capped limits. | `swapoff /swapfile`; remove Compose limits. | Local & Server |
| **D09** | **MEDIUM** | `deploy.sh` executes npm and Maven builds on 4 GB VPS concurrently with Docker. | `infrastructure/scripts/deploy.sh` | Update deployment procedure to utilize pre-built CI artifacts; skip compilation on host. | D01 | `deploy.sh` starts stack without invoking `mvn` or `npm`. | Fallback to manual build if necessary. | Local Repo |
| **D10** | **MEDIUM** | No automated Certbot renewal schedule before 90-day expiry (Jan 25, 2027). | `docs/architecture/DEPLOYMENT.md` | Configure systemd timer or cron job for Certbot webroot renewal and Nginx reload. | D03 | `certbot renew --dry-run` succeeds. | Remove renewal cron. | Future Server Action |
| **D11** | **HIGH** | CI/CD SSH configuration uses unpinned `ssh-keyscan` (TOFU). | `.github/workflows/ci.yml` | Pin verified host key (`ED25519 SHA256:Qv5L5HzAl22I...`) in pipeline configuration or secret. | D01 | Pipeline SSH succeeds strictly against pinned key. | Fallback to keyscan timeout if host rotated. | Local Repo |
| **D12** | **CRITICAL** | Production deployment must remain locked until all prerequisites verified. | `.github/workflows/ci.yml` | Retain `if: false && ...` hard lock until Stages 6.3B, 6.3C, and maintenance window approval. | D01–D11 | Verification of CI run skipping `deploy-production`. | Re-add `false &&` if unlocked prematurely. | Local Repo |

---

## 4. Proposed Execution Phases (Roadmap)

1. **Stage 6.3B: Local Repository Reconciliation (Next Immediate Step)**
   - Implement only local repository documentation, path, Nginx, and Compose adjustments (D01, D02, D03, D05, D07, D09, D11).
   - Validate with `git diff --check`, local Compose linter, and unit test suites.
   - Retain `if: false && ...` in `ci.yml`.
2. **Stage 6.3C: Off-Host Migration & Restore Rehearsal**
   - In an isolated environment (local Docker), restore the server database dump (`rr_jaggery_20261027_095949.dump`).
   - Rehearse running `V1_least_privilege_hardening.sql` and `provision_runtime_roles.sh`.
   - Verify that all 5 Spring Boot services start cleanly with `HIBERNATE_DDL_AUTO=validate` under least-privilege users.
3. **Stage 6.4: Scheduled Production Maintenance Window (Live Server Changes)**
   - Take full pre-maintenance snapshot (DB + uploads volume).
   - Create 2 GB swap on VPS.
   - Execute role migration and password provisioning on PostgreSQL.
   - Update server `.env` with least-privilege credentials.
   - Sync reconciled repository files (without `--delete`).
   - Restart containers and verify healthy status and public endpoints.
   - Install backup and Certbot crontabs.

---

## 5. Safeguards & Verification Checklist

- [x] Zero remote SSH commands or connections executed during Stage 6.3A.
- [x] Zero application code, Compose files, or configuration files modified on the server or in the local git index.
- [x] Production deployment hard lock in `ci.yml` remains fully intact (`if: false && ...`).
- [x] All 7 historical entries in `guide to deploy.md` preserved without modification.
- [x] Standalone plan created at `docs/reports/stage-6.3A-reconciliation-plan.md`.

---

## 6. Stage 6.3A Review Amendment — 2026-10-28

> **Amendment Status:** Corrective review applied to original plan (above).
> **Reviewer:** Antigravity Agent, following explicit user direction.
> **Baseline Commit at Review:** `e582aa9ca329734f03bdf671af621e3e9decfae5`
> **Scope:** Documentation corrections only. No VPS connections, no code changes, no commits, no pushes.

The following seven areas of the original plan required correction or clarification. Each correction is referenced by the workstream and decision ID it amends.

---

### Amendment C1 — Gateway Healthcheck (Amends Workstream E §4 / D07)

**Original proposal (REJECTED):**
```yaml
test: ["CMD-SHELL", "wget -q --spider https://127.0.0.1/api/v1/auth/health --no-check-certificate || exit 1"]
```

**Reason for rejection:** `--no-check-certificate` disables TLS certificate validation inside the production container. Even on an internal loopback call this sets a bad precedent, masks real TLS misconfiguration, and is explicitly contrary to the user's safety requirements.

**Corrected approach — Option A (Recommended): Probe an internal backend service directly, bypassing Nginx entirely.**

The Docker Compose internal network makes each service reachable by its service name. The auth-service exposes a health endpoint on its native port 8081. The gateway healthcheck can be changed to:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://auth-service:8081/api/v1/auth/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**Trade-offs:**
- Avoids TLS entirely (uses Docker internal HTTP only — no public exposure).
- Avoids the HTTP→HTTPS redirect loop that caused the original failure.
- Provides a stronger application health signal (proves auth-service is UP and reachable from the gateway container).
- Dependency: healthcheck now fails if auth-service is DOWN even if Nginx itself is healthy. This is accepted; a gateway behind a dead auth-service is not meaningfully healthy.
- No certificate config weakened.

**Alternative — Option B:** Add a dedicated `/healthz` location in the Nginx HTTP server block that returns 200 without redirect, then probe with `wget -q --spider http://localhost/healthz`. This requires a change to both the Nginx config and the gateway healthcheck. Preferred if an Nginx-only liveness signal (independent of backend services) is needed in future.

**Decision register D07 correction:** Replace the proposed test line with the auth-service probe (Option A) in any future Compose edit. Do not use `--no-check-certificate`.

---

### Amendment C2 — Database Migration Sequence (Amends Workstream C §2 / D04)

The original 7-step sequence is structurally correct. The following clarifications are added to prevent misapplication.

**Two-phase dependency (MUST be respected — cannot be reversed):**
- **Phase 1 — DDL (`V1_least_privilege_hardening.sql` via `psql` as superuser):**
  Creates `rr_migrator`, `rr_auth`, `rr_commerce`, `rr_custfin`, `rr_invproc`, `rr_production` as `NOLOGIN` roles (they have no password and cannot authenticate). Transfers schema ownership to `rr_migrator`. Grants `USAGE` on schemas, `SELECT/INSERT/UPDATE/DELETE` on tables, and `USAGE` on sequences to the appropriate runtime roles.
- **Phase 2 — Password provisioning (`provision_runtime_roles.sh`):**
  Reads 5 environment variables (`RR_AUTH_DB_PASSWORD`, `RR_COMMERCE_DB_PASSWORD`, `RR_CUSTFIN_DB_PASSWORD`, `RR_INVPROC_DB_PASSWORD`, `RR_PRODUCTION_DB_PASSWORD`). Runs `ALTER ROLE rr_* WITH LOGIN PASSWORD '...'` on already-existing NOLOGIN roles. This script MUST run AFTER Phase 1 — it will fail if the roles do not exist.

**Idempotency boundaries:**
- Column additions (`ADD COLUMN IF NOT EXISTS`) are safe to re-run.
- `ALTER SCHEMA auth_schema OWNER TO rr_migrator` will ERROR if `rr_migrator` does not yet exist and will silently succeed if it already exists. The whole script is wrapped in a transaction; use `psql --set ON_ERROR_STOP=1` to ensure an error aborts the transaction rather than being silently swallowed.
- Existing columns in the live database do NOT prove that schema ownership was transferred or that grants were applied. Those statements must be re-executed to guarantee grants are in place.

**Pre-validation requirement (add to Step 6):**
Before switching services to least-privilege credentials, query `information_schema.role_table_grants` for each `rr_*` role to confirm all expected grants are present:
```sql
SELECT grantee, table_schema, table_name, privilege_type
FROM information_schema.role_table_grants
WHERE grantee IN ('rr_auth','rr_commerce','rr_custfin','rr_invproc','rr_production')
ORDER BY grantee, table_schema, table_name;
```

**Special grant note:**
The `auth-service` health endpoint executes `SELECT` against `pg_catalog.pg_roles`. This requires either the `pg_monitor` role or a direct `GRANT SELECT ON pg_catalog.pg_roles TO rr_auth`. Verify this is included in the V1 DDL or add it before switching the auth-service.

---

### Amendment C3 — Backup Minimum Criteria (Amends Workstream D §2 / D06)

The original plan states "verify archive sizes and gzip integrity." This is insufficient. The following minimum criteria MUST be met before any production database change (including role migration):

**Minimum database backup criteria:**
1. Run `pg_dumpall -U postgres | gzip -c > /path/to/backup.sql.gz` — non-zero exit code fails the backup procedure.
2. Verify the file exists and is non-empty: `[ -s /path/to/backup.sql.gz ]`
3. Verify gzip integrity: `gzip -t /path/to/backup.sql.gz` — must exit 0.
4. Write a SHA256 sidecar: `sha256sum /path/to/backup.sql.gz > /path/to/backup.sql.gz.sha256`
5. Size must be materially larger than the existing manual dump (146 KB). A dump of a live database with active sessions and schema grants is expected to be substantially larger. If it is not, investigate before proceeding.

**Minimum uploads backup criteria:**
1. Create a tar archive of the uploads volume: `tar -czf /path/to/uploads_backup.tar.gz /path/to/volume/_data`
2. Verify non-empty and tar integrity: `tar -tzf /path/to/uploads_backup.tar.gz > /dev/null`
3. Write SHA256 sidecar.

**Isolated restore rehearsal (required before Phase 2 of the migration):**
1. Spin up a separate `postgres:16-alpine` container locally (not the production container).
2. Restore the dump: `psql -f backup.sql.gz` (after decompression).
3. Verify all 9 schemas (`auth_schema`, `commerce_schema`, `customer_finance_schema`, `inventory_procurement_schema`, `production_schema`, plus system schemas) are present.
4. Spot-check row counts in at least 3 application tables per schema against values recorded before the backup.
5. Verify sequence nextval values are not reset to 1.
6. Only after the restore rehearsal passes: proceed to live migration.

**Do NOT proceed with the role migration until a verified, tested backup exists and the restore rehearsal has passed.**

---

### Amendment C4 — Memory Budget (Amends Workstream E §2 / D08)

**Problem with original proposal:**
The original plan proposes a fixed aggregate limit of ~2.7 GiB without grounding it in the actual observed consumption. It also does not address the JVM heap interaction with container limits.

**Observed consumption (Stage 6.2 audit, approximate):**
| Container | Observed Usage |
|---|---|
| rr-gateway | ~7 MiB |
| auth-service | ~335 MiB |
| commerce-service | ~376 MiB |
| customer-finance-service | ~417 MiB |
| inventory-procurement-service | ~303 MiB |
| production-service | ~305 MiB |
| rr-postgres | ~81 MiB |
| **Total** | **~1.82 GiB** |

**JVM heap interaction:**
`MaxRAMPercentage=75.0` with NO container `mem_limit` means each JVM queries the host total RAM (3.7 GiB × 75% = 2.8 GiB per JVM as the theoretical heap ceiling). However, actual observed JVM RSS is ~348 MiB average — they are sharing the host pool, not each independently claiming 2.8 GiB. This is only safe while memory pressure is low; a spike in any single JVM could trigger host-level OOM.

**Corrected conservative budget (observed peak × 1.5 headroom):**
| Container | Observed | Proposed `mem_limit` | Proposed JVM `-Xmx` |
|---|---|---|---|
| auth-service | 335 MiB | 512 MiB | `-Xmx410m` |
| commerce-service | 376 MiB | 512 MiB | `-Xmx410m` |
| customer-finance-service | 417 MiB | 640 MiB | `-Xmx512m` |
| inventory-procurement-service | 303 MiB | 512 MiB | `-Xmx410m` |
| production-service | 305 MiB | 512 MiB | `-Xmx410m` |
| rr-postgres | 81 MiB | 256 MiB | N/A |
| rr-gateway | 7 MiB | 64 MiB | N/A |
| **Total** | **~1.82 GiB** | **~3.0 GiB** | — |

**Budget assessment:**
A 3.0 GiB aggregate cap on a 3.7 GiB host leaves ~0.7 GiB for the OS kernel, Compose daemon, and buffers. This is tight on a no-swap host. Combined with a planned 2 GiB swapfile, 0.7 GiB headroom becomes viable as a transient buffer during spikes. Swap is a safety buffer for transient spikes — it is NOT a design allowance for sustained memory pressure.

**Implementation rules:**
1. Do NOT add `mem_limit` entries without first running `docker stats` on the live server for at least 48 hours post-deployment to capture a real peak usage profile.
2. The figures above are starting-point proposals based on observed values + headroom. Review them against actual 48-hour peaks and adjust before finalising.
3. Set `-Xmx` in the Compose service `environment` block (appended to `JAVA_OPTS`) rather than modifying `Dockerfile.service`, so it can be adjusted per-environment without rebuilding images.

---

### Amendment C5 — SSH Host-Key Pinning (Amends Workstream F §2 / D11)

**Problem with original proposal:**
The original plan states "store the verified public host key in a GitHub secret `VPS_KNOWN_HOSTS` or compare the fingerprint." A fingerprint string alone CANNOT be used by SSH. The `known_hosts` file requires a full public key line in the format:

```
<hostname> <key-type> <base64-public-key>
```

**Correct implementation:**

The GitHub Secret `VPS_KNOWN_HOSTS` must contain the complete known_hosts entry (verified from local `~/.ssh/known_hosts` during the Stage 6.2 audit):

```
15.235.246.77 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIIMWgx2wFJcmaWgyuJiuvoSCuu2Z4H3CwtT5jj5aYrCU
```

> **Security note:** This public key value is safe to document — it is the server's public identity, not a private key or secret credential.

**Replacement CI step (replaces the current `ssh-keyscan` block):**

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
if [ -z "${VPS_KNOWN_HOSTS:-}" ]; then
  echo "::error::VPS_KNOWN_HOSTS secret is empty or missing. Cannot verify host identity."
  exit 1
fi
printf '%s\n' "$VPS_KNOWN_HOSTS" > ~/.ssh/known_hosts
chmod 644 ~/.ssh/known_hosts
# SSH uses StrictHostKeyChecking=yes (the default).
# If the server's key does not match, SSH fails with a fatal error (fails closed).
```

**Fail-closed behaviour:**
SSH's default `StrictHostKeyChecking=yes` means the connection is refused with a fatal error if the server presents a key not in `~/.ssh/known_hosts`. No additional configuration is needed to enforce this. The step above fails closed automatically.

**Key rotation handling:**
If the VPS host key is ever rotated (e.g., VPS rebuild), the `VPS_KNOWN_HOSTS` secret must be updated manually by verifying the new fingerprint directly on the VPS console. Automatic rotation would defeat the purpose of pinning.

**Decision register D11 correction:** Remove `ssh-keyscan` from the Configure SSH step. Write `VPS_KNOWN_HOSTS` directly to `~/.ssh/known_hosts`. Fail if the secret is empty.

---

### Amendment C6 — Server-Only Changes Preservation Process (Amends Workstream A §3 / D05)

The original plan mentions merging server edits locally but does not specify the process for capturing and reviewing them before any sync. The following 3-step preservation protocol is required:

**Step 1 — Server-side snapshot before any file sync:**
```bash
cp -a /home/ubuntu/RR_Jaggery /home/ubuntu/RR_Jaggery_pre_stage63b_backup
```
This preserves the server's current state including `.env`, all local edits, and untracked files.

**Step 2 — Capture diffs of each modified tracked file:**
```bash
cd /home/ubuntu/RR_Jaggery
git diff compose.production.yaml            > /home/ubuntu/server_diffs/compose_diff.txt
git diff infrastructure/nginx/nginx.production.conf  > /home/ubuntu/server_diffs/nginx_diff.txt
git diff services/auth-service/src/main/resources/application.yml > /home/ubuntu/server_diffs/auth_yml_diff.txt
git status --short                          > /home/ubuntu/server_diffs/untracked.txt
```
The `untracked.txt` captures `compose.http-test.override.yaml` and `infrastructure/nginx/nginx.http-test.conf`.

**Step 3 — Review and classify each server change:**
Each server-side modification must be explicitly classified as one of:
- **Discard** — The corresponding local repository version is already correct (e.g., Nginx certificate path was corrected in D03).
- **Port to repository** — The server change represents a valid improvement that must be incorporated into the local repo before the next rsync (e.g., `SMTP_USERNAME` quoting, `bootstrap.admin` block). Only then will the rsync not overwrite it.
- **Preserve via rsync exclusion** — The file should never be synced (e.g., `.env*`, HTTP test configs). Add to the permanent `--exclude` list.

**Rule:** Do NOT sync any file to the server until the corresponding repository version has incorporated or deliberately superseded the server's change. Running rsync without this review risks silently overwriting production configuration with a stale repository version.

---

### Amendment C7 — Path and Transfer Reference Inventory (Amends Workstream A §1, §2 / D01, D02)

The original plan correctly identifies the need to update path references. The following is the complete enumerated inventory of all `/opt/rr-jaggery/` occurrences that must be updated in a future local repository edit:

| File | Line(s) | Current Value | Proposed Replacement |
|---|---|---|---|
| `.github/workflows/ci.yml` | 197 (rsync target) | `/opt/rr-jaggery/` | `/home/ubuntu/RR_Jaggery/` |
| `.github/workflows/ci.yml` | 202 (SSH deploy cmd) | `/opt/rr-jaggery/` | `/home/ubuntu/RR_Jaggery/` |
| `.github/workflows/ci.yml` | 221 (comment) | `/opt/rr-jaggery/` | `/home/ubuntu/RR_Jaggery/` |
| `infrastructure/scripts/deploy.sh` | 12 (`PROJECT_DIR`) | `"/opt/rr-jaggery"` | `"/home/ubuntu/RR_Jaggery"` |
| `infrastructure/scripts/deploy.sh` | 13 (`LOG_FILE`) | `/var/log/rr-jaggery/deploy.log` | keep or redirect to `/home/ubuntu/logs/` |
| `infrastructure/scripts/restore.sh` | 27 (`COMPOSE_FILE` default) | `/opt/rr-jaggery/compose.production.yaml` | `/home/ubuntu/RR_Jaggery/compose.production.yaml` |
| `infrastructure/scripts/vps-provision.sh` | 79, 83 (`mkdir`, `chown`) | `/opt/rr-jaggery` | `/home/ubuntu/RR_Jaggery` |
| `docs/architecture/DEPLOYMENT.md` | 46, 90, 91, 175–177, 209, 212, 226 | `/opt/rr-jaggery/` | `/home/ubuntu/RR_Jaggery/` |

**Non-destructive transfer sequence (replaces any assumed default):**
1. **Dry run only:** `rsync -azvn [exclusions] local/ server:/home/ubuntu/RR_Jaggery/` — review the output completely and obtain explicit approval before proceeding.
2. **First live sync — no `--delete`:** `rsync -az [exclusions] local/ server:/home/ubuntu/RR_Jaggery/` — files transferred, nothing deleted.
3. **Post-sync server inspection:** On the server, run `git status` and `git diff` to verify what changed.
4. **Nginx config test:** `docker exec rr-gateway nginx -t` — must return success before any container operation.
5. `--delete` may only be considered AFTER the server-only change review (C6) is complete and all critical server files are either ported to the repo or permanently excluded.

---

### Amendment Summary — Decision Register Updates

| ID | Area | Correction Summary |
|---|---|---|
| D07 | Gateway Healthcheck | REJECT `--no-check-certificate`. Use internal backend probe `http://auth-service:8081/api/v1/auth/health` instead. |
| D04 | DB Migration Sequence | Clarify two-phase dependency (DDL creates NOLOGIN; provision_runtime_roles.sh sets LOGIN+PASSWORD — must run in order). Idempotency boundaries documented. Pre-validation query against `role_table_grants` required. Special `pg_roles` grant for auth-service noted. |
| D06 | Backup Criteria | Minimum criteria: non-empty gzip dump, gzip -t passes, SHA256 sidecar, size > 146 KB, isolated restore rehearsal with schema and row-count verification. |
| D08 | Memory Budget | Budgets are measurement-driven (observed × 1.5). Total cap ~3.0 GiB. Swap is a spike buffer, not a design allowance. Limits set via Compose environment block, not Dockerfile. |
| D11 | SSH Host-Key Pinning | `VPS_KNOWN_HOSTS` secret must contain the full known_hosts line (not just a fingerprint). `ssh-keyscan` replaced entirely. Fails closed if secret is empty. |
| D05 | Server-Only Changes | 3-step preservation protocol: server snapshot → per-file diff → classify each change as discard/port/exclude before any rsync. |
| D01/D02 | Path & Transfer | Complete enumerated inventory of all `/opt/rr-jaggery/` references. Non-destructive 5-step transfer sequence: dry run → no-`--delete` sync → server inspection → nginx -t → gated `--delete`. |

---

### Amendment Safeguards Confirmed

- [x] Zero remote SSH connections made to the VPS.
- [x] Zero application code, Compose files, Dockerfiles, CI workflows, migrations, or secrets modified.
- [x] No files staged, committed, or pushed.
- [x] Production deployment hard lock (`if: false &&`) in `.github/workflows/ci.yml` remains fully intact.
- [x] All Sprint 1–10, Phase 2.1–2.2, and all historical guide entries preserved without modification.
- [x] Existing plan content (Sections 1–5 above) preserved without alteration; this amendment section appended only.
