# RR Jaggery — Master Deployment Roadmap & Runbook (`guide to deploy.md`)

> **Document Status:** Authoritative Master Deployment Guide  
> **Repository:** `E:\RR Jaggery`  
> **Current Branch:** `main` (synchronized with `origin/main` at `e582aa9`)<br>
> **Baseline Commit:** `e582aa9ca329734f03bdf671af621e3e9decfae5` (*fix(ci): hard-disable production deployment pending safety review*)<br>
> **Document Last Updated:** 2026-10-28<br>
> **Operating Rule:** Every deployment-related task MUST update this document as part of that same task.

---

## 1. Safety and Deployment Gates

1. **Working Tree & Sprint Preservation:**  
   Preserve all completed sprint work (Sprints 1–10, Phase 2.2E). Never run `git reset --hard`, `git stash drop`, or discard untracked files without explicit verification.
2. **Credential & Secret Protection:**  
   Never commit `.env`, private keys, TLS/SSL private keys, or passwords. Never print raw credentials to logs, transcripts, or reports.
3. **Isolation Guarantee:**  
   Never execute tests against existing project containers (`rr-postgres`, `rr-*-service`) or any remote production server. All integration tests must execute in isolated disposable containers identified by unique test IDs.
4. **Zero Production Assumptions:**  
   Distinguish historical report claims from live verified state. Do not claim a component is deployed, healthy, or verified unless verified in the current session with reproducible commands.
5. **Domain Separation:**  
   The staging/testing domain is `rrdentalneeds.online`. The permanent brand domain `rrjaggery.com` is strictly reserved for post-validation migration and must never be modified without explicit owner authorization.
6. **No Premature CI/CD Execution:**  
   Do not trigger or implement live deployment CI/CD until Git integration is reviewed, clean, and verified.

---

## 2. Deployment Stages & Master Roadmap

| # | Stage Name | Current Status | Key Dependencies | Primary Target File(s) |
|---|---|---|---|---|
| 1 | **Application Development and Architecture** | `COMPLETED — REPORTED` | Sprint 9-10 codebases | `services/*`, `frontend/*` |
| 2 | **Production Configuration and Security** | `COMPLETED — VERIFIED` | Phase 2.2E.1–2.2E.5 | `compose.production.yaml`, `init-schemas.sql` |
| 3 | **OVHcloud VPS Deployment** | `COMPLETED — HISTORICAL (RECONCILIATION PENDING)` | Initial VPS & Nginx live (Sprint 10); Phase 2.2E pending | `infrastructure/scripts/vps-provision.sh` |
| 4 | **Backup and Operational Readiness** | `COMPLETED — VERIFIED` | Phase 2.2E.4 Drill (20/20 PASS) | `infrastructure/scripts/backup.sh`, `restore.sh` |
| 5 | **Git Integration** | `COMPLETED — VERIFIED` | Stage 5.1–5.3 & 6.1.4 (Clean push to origin/main) | Local & Remote synchronized at `e582aa9` |
| 6 | **OVHcloud CI/CD Implementation** | `BLOCKED — HARD LOCK` | Stage 6.1.3 hard lock; Stage 6.3A reconciliation plan authored | `docs/reports/stage-6.3A-reconciliation-plan.md` |
| 7 | **End-to-End Production Validation** | `PENDING` | Pending reconciliation of latest changes on VPS | Full system health & business workflows |
| 8 | **Final Release Approval** | `PENDING` | Stage 7 Sign-off | Business owner sign-off & domain migration |

---

### Stage 1: Application Development and Architecture
- **Status:** `COMPLETED — REPORTED` (Historical implementation accepted; local dev runtime active)
- **Scope & Objectives:**
  - 5-Service consolidated JVM architecture (`auth-service`, `commerce-service`, `customer-finance-service`, `inventory-procurement-service`, `production-service`).
  - React/Vite storefront and manager portal frontend bundle.
  - Removal of Redis and legacy standalone services (Sprint 9).
  - Hardening of Razorpay checkout flow, elimination of `TEST_PAYMENT` bypass (commit `0040e63`).
  - CORS origin restrictions and native forwarded headers handling (commit `2d902e1`).
  - Stock validation and rollback enforcement on order dispatch (commit `93a1259`).
- **Completed Tasks:**
  - [x] Consolidate backend microservices down to 5 JVM services (Sprint 9).
  - [x] Remove Redis caching dependency and migrate to database-backed session/token verification.
  - [x] Remove `TEST_PAYMENT` backdoor and enforce server-side signature validation.
  - [x] Enforce CORS configuration from environment variables with Vite fallback.
  - [x] Configure forward-headers-strategy native for trusted Nginx upstream proxying.
- **Remaining Tasks:**
  - None for core application development.
- **Evidence & Verification Required:**
  - `git log` verification showing commits `0040e63` and `2d902e1` on `main`.
  - Local dev container verification: 7 containers running healthy (`docker ps`).
- **Blockers & Dependencies:**
  - None.
- **Next Action:**
  - Maintain application stability during configuration hardening in Stage 2.

---

### Stage 2: Production Configuration and Security
- **Status:** `COMPLETED — VERIFIED` (Phase 2.2E.5 definitive run `36ed1200`: **21/21 PASSED, 0 FAILED** — 2026-10-28)
- **Scope & Objectives:**
  - Dedicated least-privilege PostgreSQL runtime roles (`rr_auth`, `rr_commerce`, `rr_custfin`, `rr_invproc`, `rr_production`) with `NOLOGIN` default.
  - Independent migration role (`rr_migrator`) and schema isolation across 8 application schemas.
  - Production `ddl-auto: validate` enforcement across all 5 Spring Boot services.
  - Dedicated database passwords externalized through environment variables.
  - Schema parity and column consistency verification between entity definitions and DDL migrations.
  - JVM resource limits (`-Xms64m -Xmx256m -XX:TieredStopAtLevel=1`) and container memory limits.
- **Completed Tasks:**
  - [x] Design and verify least-privilege database schemas and roles (`init-schemas.sql`, `V1_least_privilege_hardening.sql`) — Phase 2.2E.1.
  - [x] Implement secure PowerShell and Bash password provisioning scripts (`provision_runtime_roles.ps1`, `provision_runtime_roles.sh`) — Phase 2.2E.2.
  - [x] Pass 26/26 disposable least-privilege tests in `test_least_privilege.ps1` — Phase 2.2E.2.
  - [x] Conduct read-only security audit of provisioning mechanism (Phase 2.2E.2A).
  - [x] Externalize service database credentials and set `HIBERNATE_DDL_AUTO: validate` in `compose.production.yaml` and `application.yml` files — Phase 2.2E.3.
  - [x] Build all 5 service production JARs (`mvn package -DskipTests`) — Phase 2.2E.5.
  - [x] Build all 5 test Docker images (`rr-test-auth:latest`, etc.) — Phase 2.2E.5.
  - [x] Fix schema column drift discovered during startup validation (`customer_schema.offline_orders` `delivery_charges` & `order_type`; `finance_schema.employees` `notes` & `updated_at`; `inventory_schema.inventory_items` Sprint 9 column names).
  - [x] Re-verify disposable least-privilege test suite post-schema-fix (26/26 PASS).
  - [x] Restrict JPA `@EntityScan` in `InventoryProcurementApplication` to `inventory` and `procurement` packages (VERIFIED: booted cleanly in 81s on role `rr_invproc`).
  - [x] Add `hibernate.default_schema: public` to `customer-finance-service/src/main/resources/application.yml` — resolves unqualified shared `common-library` form entities to `public` schema where `rr_custfin` has grants.
  - [x] Rebuild `customer-finance-service` JAR and test Docker image (`rr-test-customer-finance:latest`) — clean build, 15.7s.
  - [x] Patch `test_services_startup.ps1`: 35s initial JVM grace period, 240s per-service timeout, 4s poll interval, wait-for-exit on negative test containers, `-e MANAGEMENT_HEALTH_MAIL_ENABLED=false` on all 3 auth-service container launches to suppress `MailHealthIndicator` in SMTP-free test environment.
  - [x] **Complete full isolated 5-service startup validation — Phase 2.2E.5 — Test Run `36ed1200`:**
    - **21/21 PASSED, 0 FAILED** (2026-10-28, exit code 0)
    - All 5 services started healthy with `HIBERNATE_DDL_AUTO=validate` on role `rr_auth`, `rr_invproc`, `rr_commerce`, `rr_custfin`, `rr_production`.
    - All 5 services returned HTTP 200 `{"status":"UP"}` on `/actuator/health`.
    - Zero `SchemaManagementException`, `PSQLException`, `permission denied`, or datasource authentication errors in any service logs.
    - Negative test — wrong password: `password authentication failed for user "rr_auth"` ✅
    - Negative test — schema mismatch: `SchemaManagementException` / `missing column [email_verified]` ✅
    - Negative test — unauthorized DDL: `permission denied for schema auth_schema` ✅
    - All 7 disposable containers and 1 network cleaned up: `rr-test-pg-36ed1200`, `rr-test-auth-36ed1200`, `rr-test-comm-36ed1200`, `rr-test-cust-36ed1200`, `rr-test-inv-36ed1200`, `rr-test-prod-36ed1200`, `rr-test-net-36ed1200` ✅
- **Remaining Tasks:**
  - None. All security hardening, schema parity, and isolated validation is complete.
- **Evidence & Verification Required:**
  - ✅ Phase 2.2E.5 Test Run `36ed1200`: **21 PASSED, 0 FAILED** (exit code 0, 2026-10-28)
  - ✅ `test_least_privilege.ps1` Run `3385ee84`: **26/26 PASSED** (Phase 2.2E.2)
  - ✅ Manual verification run `8190fdfc`: all 5 services started and schema-validated, all 5 business health endpoints HTTP 200.
  - Phase 2.2E.3 Validation Report: `phase_2_2e_3_validation_report.md`
- **Blockers & Dependencies:**
  - None remaining. Stage 2 is complete.
- **Next Action:**
  - Proceed to Stage 5 Git Integration: audit diffs, confirm no secrets, commit Phase 2.2E changes in logical atomic commits.

---

### Stage 3: OVHcloud VPS Deployment
- **Status:** `COMPLETED — HISTORICAL (RECONCILIATION PENDING)`
- **Scope & Objectives:**
  - Historical VPS deployment recorded in Sprint 10 documentation and `RR_Jaggery_VPS_Deployment_Security_Report_Updated_2026-10-27.docx`.
  - OVHcloud VPS-1 (2 vCores, 4 GB RAM, 40 GB NVMe, Ubuntu 24.04.5 LTS).
  - UFW firewall configured (allow 22, 80, 443; strictly block 5432 and 8081–8086).
  - Docker Engine and Docker Compose running 7-container architecture.
  - Cloudflare DNS A-records pointing `rrdentalneeds.online` and `www` to VPS IPv4 with Orange-cloud proxy.
  - Cloudflare Origin CA certificate and private key installed in `/opt/rr-jaggery/infrastructure/nginx/ssl/` (mounted in `ssl_certs` volume).
  - Nginx reverse proxy actively serving storefront and API over HTTPS with rate limiting and security headers.
  - **Reconciliation Scope:** Reconcile Phase 2.2E least-privilege database roles, schema column parity migrations, and operational scripts onto this active VPS without causing service downtime.
- **Completed Tasks (Historical Baseline):**
  - [x] Author host provisioning script (`infrastructure/scripts/vps-provision.sh`).
  - [x] Author deployment runbook (`docs/architecture/DEPLOYMENT.md`).
  - [x] Configure Nginx reverse proxy with SSL termination and WebSocket/SSE support (`infrastructure/nginx/nginx.production.conf`).
  - [x] Provision OVHcloud VPS instance (Ubuntu 24.04 LTS) — Historical baseline.
  - [x] Install Docker Engine and Docker Compose — Historical baseline.
  - [x] Configure Cloudflare DNS and install TLS Origin certificates (`rrdentalneeds.online`) — Historical baseline.
  - [x] Initial 7-container Docker Compose launch — Historical baseline.
- **Remaining Tasks (Phase 2.2E Production Reconciliation):**
  - [ ] Audit `/opt/rr-jaggery/.env` on live VPS to document current active credentials.
  - [ ] Apply `infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql` to live PostgreSQL database.
  - [ ] Execute `infrastructure/docker/postgres/provision_runtime_roles.sh` on live host to provision 5 runtime role passwords.
  - [ ] Coordinate `.env` updates on VPS to set `RR_*_DB_USER`, `RR_*_DB_PASSWORD`, and `HIBERNATE_DDL_AUTO=validate`.
  - [ ] Perform controlled container rebuild and restart (`docker compose up -d`).
  - [ ] Verify container health and schema validation without regression.
- **Evidence & Verification Required:**
  - Live server SSH connectivity and container inspection.
  - `curl -I https://rrdentalneeds.online/` returning HTTP 200.
  - Verification that PostgreSQL runs under least-privilege roles.
- **Blockers & Dependencies:**
  - Blocked on Stage 6 CI/CD safety reconciliation and coordinated database migration on the VPS.
- **Next Action:**
  - Complete Stage 6 CI/CD safety gate and prepare off-server migration rehearsal before touching the live database.

---

### Stage 4: Backup and Operational Readiness
- **Status:** `COMPLETED — VERIFIED` (Local & Disposable Integration Drill Verified; Off-host pending external storage)
- **Scope & Objectives:**
  - Reliable, non-corrupting backup script for PostgreSQL database dumps and uploaded customer/product media (`uploads_data` volume).
  - Safe restoration script with pre-restore safety snapshots, service quiescence, checksum verification, and post-restore integrity assertions.
  - Pruning safety guard (`MIN_KEEP_BACKUPS`) ensuring the system never deletes all backups.
  - Operational runbook in `BACKUP_AND_ROLLBACK.md`.
- **Completed Tasks:**
  - [x] Eliminate pseudo-TTY (`-t`) flag from `docker exec` in `backup.sh`, `restore.sh`, and documentation to prevent binary stream corruption.
  - [x] Implement atomic writes (`.tmp_*` buffering and atomic `mv`) and gzip archive integrity testing (`gzip -t`).
  - [x] Coupled database and uploaded-media backups (`rr_jaggery_uploads_*.tar.gz`).
  - [x] Add pre-flight disk headroom checks (`MIN_FREE_MB=500`).
  - [x] Add automated pre-restore safety snapshot in `restore.sh`.
  - [x] Implement 20-point automated disposable integration drill (`test_backup_restore.ps1`).
  - [x] Execute drill in disposable containers `rr-disp-bkp-src-7165cda2` and `rr-disp-rst-tgt-7165cda2`: **20/20 PASSED, 0 FAILED** (Phase 2.2E.4 report).
- **Remaining Tasks:**
  - [ ] Configure host cron job (`0 2 * * * /opt/rr-jaggery/infrastructure/scripts/backup.sh`) once VPS is provisioned.
  - [ ] Configure off-server backup replication (e.g., S3 bucket or encrypted offsite rsync) once remote storage target is provided.
- **Evidence & Verification Required:**
  - Phase 2.2E.4 Report: `phase_2_2e_4_operational_readiness_report.md`.
  - Passing execution logs from `test_backup_restore.ps1`.
- **Blockers & Dependencies:**
  - Off-server copy depends on external cloud storage credentials (to be supplied post-VPS setup).
- **Next Action:**
  - Commit hardened `backup.sh`, `restore.sh`, `BACKUP_AND_ROLLBACK.md`, and `test_backup_restore.ps1` in Stage 5.

---

### Stage 5: Git Integration
- **Status:** `COMPLETED — VERIFIED` (Stage 5.1 audit, Stage 5.2 three atomic commits, Stage 5.3 push & sync verified)
- **Scope & Objectives:**
  - Audit working tree diffs, whitespace, and untracked files.
  - Verify that no secrets or temporary credentials exist in modified files.
  - Verify `git diff --check` exits with code 0.
  - Stage and commit Phase 2.2E enhancements in logical, atomic commits.
  - Push branch `main` to `origin/main` and verify remote synchronization.
- **Completed Tasks:**
  - [x] Verify `origin` remote URL: `https://github.com/rrjaggery-1458/RR_Jaggery.git`.
  - [x] Stage 5.1: Perform read-only repository audit of 15 modified files and 5 untracked files with 0 secrets found and `git diff --check` exit code 0.
  - [x] Stage 5.2: Create atomic Commit A (`49ba2b3`): `feat(db): least-privilege runtime roles, schema parity, and DDL validation` (11 files).
  - [x] Stage 5.2: Create atomic Commit B (`c2bc613`): `feat(ops): harden backup and restore, role provisioning, and validation` (7 files).
  - [x] Stage 5.2: Create atomic Commit C (`901d824`): `docs: update operational runbook and deployment guide` (2 files).
  - [x] Stage 5.3: Push 6 commits (`0040e63`, `2d902e1`, `a97321d`, `49ba2b3`, `c2bc613`, `901d824`) to `origin/main`.
  - [x] Stage 5.3: Verify clean working tree and identical commit hashes on local and remote (`git status --short --branch` -> `## main...origin/main`).
- **Remaining Tasks:**
  - None. Stage 5 Git Integration is complete and verified.
- **Evidence & Verification Required:**
  - `git status --short --branch` clean at `## main...origin/main`.
  - Commit hashes match: `901d824617d53509557c677b0af550d66ca0e83f` on local `main` and `origin/main`.
- **Blockers & Dependencies:**
  - None. Stage 5 is complete.
- **Next Action:**
  - Proceed to Stage 6: OVHcloud CI/CD Implementation preparation (review `.github/workflows/ci.yml` and required repository secrets; requires separate review and approval before any deployment actions begin).

---

### Stage 6: OVHcloud CI/CD Implementation
- **Status:** `BLOCKED — HARD LOCK` (Stage 6.1.3: production deployment job permanently disabled via `if: false &&` until Stage 6.2 prerequisites are verified and approved)
- **Scope & Objectives:**
  - Automated CI pipeline on GitHub Actions (`.github/workflows/ci.yml`).
  - Automated CI validation jobs:
    1. Frontend build and typecheck (`npm run build`).
    2. Backend build and unit/integration tests (`mvn clean test`).
    3. Production configuration validation (check 7 containers, no Redis, no legacy services).
  - Production deployment via secure SSH (`rsync` + `deploy.sh`) — **MANUAL DISPATCH WITH CONFIRMATION INPUT on `main`**.
  - Post-deployment health verification probe.
- **Completed Tasks:**
  - [x] Existing workflow file defined in `.github/workflows/ci.yml`.
  - [x] Validation step preventing Redis from entering `compose.production.yaml`.
  - [x] Validation step asserting absence of legacy microservices.
  - [x] Stage 6.1.1: Add `workflow_dispatch` trigger with `confirm_production_deploy` boolean input (default: `false`) to `.github/workflows/ci.yml`.
  - [x] Stage 6.1.1: Gated `deploy-production` job to require manual dispatch on `main` AND explicit confirmation (`inputs.confirm_production_deploy == true`). Documented that enabling authorizes attempt only; deployment is not guaranteed safe.
  - [x] Stage 6.1.1: Hardened `Configure SSH` step: sourced secrets via step environment variables, enforced non-empty validation checks, applied restrictive permissions (`0700` directory, `0600` private key), used `printf` to prevent log leakage, added `-T 10` timeout to `ssh-keyscan`, and documented that SSH host authenticity remains unverified (pinned host-key trust remains an unresolved prerequisite).
  - [x] Stage 6.1.2: Read-only safety audit confirmed input-based gating insufficient while critical risks remain: untrustworthy host-key verification, `rsync --delete` wiping server-side TLS certificates, VPS database schema/role parity gap, and 4 GB RAM OOM risk from on-host builds.
  - [x] Stage 6.1.3: Applied `if: false &&` hard safety lock to `deploy-production` job condition. Production deployment is now statically impossible under any trigger until lock is explicitly removed after Stage 6.2 approval.
- **Remaining Tasks:**
  - [ ] Verify secret availability at the `production` GitHub Environment scope (`VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `PRODUCTION_DOMAIN`).
  - [ ] Verify trusted SSH host-key pinning strategy (ssh-keyscan retrieves presented key only; pinned trust remains prerequisite).
  - [ ] Review `rsync --delete` transfer exclusions to prevent wiping server-side runtime data (SSL certs, logs, uploads).
  - [ ] Safe non-destructive test of SSH connectivity without deployment.
- **Evidence & Verification Required:**
  - Green CI run on PR/push without executing deployment.
  - Successful dry-run SSH connectivity check via manual dispatch.
- **Blockers & Dependencies:**
  - Investigation of GitHub secret configuration at `production` environment scope.
  - Pre-deployment database migration and `.env` reconciliation on VPS.
- **Next Action:**
  - Audit GitHub repository settings for secret presence in `environment: production`.

---

### Stage 7: End-to-End Production Validation
- **Status:** `PENDING` (Pending reconciliation and verification of latest database-hardening, runtime-role, migration, and backup/restore changes on existing VPS)
- **Scope & Objectives:**
  - Exhaustive smoke test matrix on the live production environment (`https://rrdentalneeds.online`).
  - Functional validation of all business flows across storefront and management portals.
- **Completed Tasks:**
  - [x] Local end-to-end regression tests passed during Sprints 9 & 10.
  - [x] Local health endpoints verified (`/api/v1/*/health`).
- **Remaining Tasks:**
  - [ ] **HTTPS & Gateway:** Verify SSL/TLS certificate validity, HTTP-to-HTTPS redirection, and security headers.
  - [ ] **Admin Bootstrap:** Verify initial login with `BOOTSTRAP_ADMIN_USERNAME` and mandatory password change.
  - [ ] **Staff / Manager Management:** Create manager account, verify role-based access control (RBAC).
  - [ ] **Customer Onboarding:** Register new customer, test login, verify JWT issuance and refresh.
  - [ ] **Product Catalogue:** Verify category browsing, jaggery product listings, pricing, and stock badges.
  - [ ] **Cart & Checkout:** Add products, update quantities, verify order calculation (no GST applied).
  - [ ] **Payment & COD:** Place order via Cash on Delivery; verify order creation and invoice generation.
  - [ ] **Order Fulfillment & Stock:** Transition order to Dispatched and Delivered; assert stock deduction.
  - [ ] **Dynamic Forms:** Submit wholesale order inquiry and internal expense form; verify attachment upload.
  - [ ] **Production Batch Tracking:** Create production batch, record output yield, check inventory sync.
  - [ ] **Live Backup & Restore Drill:** Run `backup.sh` on live host and test non-destructive verification.
- **Evidence & Verification Required:**
  - Full smoke test execution log signed off by QA / Lead Engineer.
- **Blockers & Dependencies:**
  - Depends on Stage 3 (Live VPS Deployment).
- **Next Action:**
  - Execute smoke test checklist immediately after live container startup.

---

### Stage 8: Final Release Approval
- **Status:** `PENDING`
- **Scope & Objectives:**
  - Formal sign-off by business owner and stakeholders.
  - Transition from temporary staging domain (`rrdentalneeds.online`) to production domain (`rrjaggery.com`).
  - Final operational handover.
- **Completed Tasks:**
  - None (Pending all previous stages).
- **Remaining Tasks:**
  - [ ] Review security audit findings and sign off on least-privilege database posture.
  - [ ] Review resource utilization under nominal traffic (RAM < 2.5 GB on VPS-1).
  - [ ] Business owner review and acceptance of live storefront.
  - [ ] Formal authorization for domain migration to `rrjaggery.com`.
  - [ ] Update DNS records, Cloudflare settings, and Nginx domain configuration for `rrjaggery.com`.
  - [ ] Issue final TLS certificates for `rrjaggery.com`.
- **Evidence & Verification Required:**
  - Signed Release Acceptance Document.
  - DNS resolution confirmation for `rrjaggery.com`.
- **Blockers & Dependencies:**
  - Blocked on Stage 7 (E2E Validation).
- **Next Action:**
  - Schedule release review meeting upon completion of Stage 7.

---

## 3. Mandatory Status-Update Rule for Every Future Task

> **CRITICAL PROTOCOL:**  
> **Every time a deployment-related task is performed, `guide to deploy.md` MUST be updated as part of that same task.**

### Task Execution Workflow:
1. **Before Starting:**
   - Read `guide to deploy.md` to identify the current stage, active blockers, and safety constraints.
   - Run `git status --short` and `git branch -v` to ensure awareness of working tree state.
2. **During Execution:**
   - Follow all safety gates. Run tests only in isolated disposable environments.
   - Record exact commands run, exit codes, and output artifacts.
3. **After Completing the Task:**
   - Update affected stage statuses (`COMPLETED — VERIFIED`, `COMPLETED — REPORTED`, `IN PROGRESS`, `PENDING`, or `BLOCKED`).
   - Update task checkboxes (`[ ]` &rarr; `[x]`).
   - Append a new entry to the **Task-Update Log** below (preserving all prior entries).
   - Verify `git diff --check` passes on `guide to deploy.md`.
   - Summarize the updated guide state in the final response to the user.

---

## 4. Task-Update Log

### Entry 1 — 2026-10-27 | Baseline Creation & Phase 2.2E Audit Integration
- **Engineer / Agent:** Antigravity Agent
- **Git Branch:** `main` (ahead of `origin/main` by 3 commits: `0040e63`, `2d902e1`, `a97321d`)
- **Baseline Commit:** `a97321d`
- **Work Performed:**
  - Inspected repository history, Sprint 9 service consolidation, Sprint 10 deployment reports, and Phase 2.2E artifacts.
  - Verified working tree status: 14 modified files, 4 untracked operational scripts, 0 whitespace violations.
  - Verified Phase 2.2E.4 operational readiness status: 20/20 tests passed in isolated disposable containers.
  - Identified Phase 2.2E.5 in-progress status: 5-service startup validation in disposable environment, pending entity-scan scope resolution for `inventory-procurement-service`.
  - Created master deployment roadmap covering all 8 stages with clear operational gates and blockers.
- **Files Changed / Created:**
  - `guide to deploy.md` (Created in repository root)
- **Commands & Tests Run:**
  - `git status --short`
  - `git branch -v`
  - `git log origin/main..main --oneline`
  - `git diff --check` (Exit code: 0)
- **Actual Results:**
  - Master guide created. Stage 1 reported complete, Stage 2 in progress, Stage 3 blocked on VPS, Stage 4 verified complete, Stage 5 in progress, Stages 6-8 pending.
- **Blockers Identified:**
  - Stage 2: `inventory-procurement-service` entity scan includes unused `common.forms.*` entities.
  - Stage 3: OVHcloud VPS-1 not yet purchased by owner.
- **Next Recommended Action:**
  - Fix `@EntityScan` in `InventoryProcurementApplication` to restrict JPA scan, then execute `test_services_startup.ps1` to complete Stage 2.

### Entry 2 — 2026-10-27 | Phase 2.2E.5 Startup Failure Investigation & Root Cause Diagnosis
- **Engineer / Agent:** Antigravity Agent
- **Git Branch:** `main` (ahead of `origin/main` by 3 commits: `0040e63`, `2d902e1`, `a97321d`)
- **Baseline Commit:** `a97321d`
- **Work Performed:**
  - Evaluated execution of `test_services_startup.ps1` (Test run `309f7975`).
  - Confirmed 4 out of 5 microservices initialized and validated schemas cleanly:
    - `inventory-procurement-service`: STARTED in 81s, healthy HTTP 200 UP on role `rr_invproc`.
    - `commerce-service`: STARTED in 91s, healthy HTTP 200 UP on role `rr_commerce`.
    - `production-service`: STARTED in 77s, healthy HTTP 200 UP on role `rr_production`.
    - `auth-service`: STARTED in 90s, validated schema cleanly on role `rr_auth`.
  - Investigated `customer-finance-service` failure (`Schema-validation: missing table [form_attachments]`):
    - Confirmed `public.form_attachments` is already present, owned by `rr_migrator`, and granted to `rr_custfin` in `init-schemas.sql`.
    - Found root cause: `customer-finance-service` lacks `hibernate.default_schema: public`. Because `rr_custfin`'s search_path begins with `customer_schema`, Hibernate defaults unqualified entities (`FormAttachment`, `FormDefinition`, etc.) to `customer_schema`, where they do not exist.
    - Verified all domain entities in `customer-finance-service` explicitly specify `schema = "customer_schema"`, `finance_schema`, or `notification_schema`, meaning setting `default_schema: public` will solely resolve the unqualified shared `common-library` form entities without affecting domain tables.
  - Investigated JVM startup time (approx. 80-90s per service, 3-4 mins total):
    - Concurrency of 5 JVMs booting simultaneously inside Docker Desktop on Windows creates heavy JIT compilation and disk I/O contention.
    - Continuous spawning of `docker run --rm alpine wget` every 2s exacerbated host CPU and Docker daemon contention.
    - Probe timeout for `auth-service` occurred because the probe loop began immediately at second 0 while all 5 JVMs were compiling classes.
- **Files Inspected & Modified:**
  - `infrastructure/docker/postgres/init-schemas.sql` (Added `offline_orders.order_type`, `employees.notes`, `employees.updated_at`)
  - `infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql` (Added column parity guards)
  - `services/inventory-procurement-service/src/main/java/com/rrjaggery/InventoryProcurementApplication.java` (Added explicit `@EntityScan` / `@EnableJpaRepositories`)
  - `guide to deploy.md` (Updated Stage 2 status, completed tasks, and added Entry 2)
- **Commands & Tests Run:**
  - `test_least_privilege.ps1` (26/26 PASSED)
  - `test_services_startup.ps1` run `309f7975` (16 passed, 5 failed due to `customer-finance-service` schema resolution and probe timeouts)
  - `git diff --check` (Exit code: 0)
- **Proposed Correction:**
  - Add `hibernate.default_schema: public` to `services/customer-finance-service/src/main/resources/application.yml`.
  - Rebuild `customer-finance-service` JAR and test image.
  - Update `test_services_startup.ps1` with 30s initial startup grace period, 240s timeout, and wait-for-exit on negative test containers.
- **Next Recommended Action:**
  - Apply the proposed correction and re-run `test_services_startup.ps1` to achieve 100% pass across all 5 services.

---

### Entry 3 — 2026-10-28 | Phase 2.2E.5 — Definitive Validation PASSED (21/21) — Stage 2 COMPLETE

- **Engineer / Agent:** Antigravity Agent
- **Git Branch:** `main` (ahead of `origin/main` by 3 commits: `0040e63`, `2d902e1`, `a97321d`)
- **Baseline Commit:** `a97321d`
- **Test Run ID:** `36ed1200`
- **Result:** **21 PASSED, 0 FAILED** — Exit code 0

#### Work Performed

**Fix applied to `test_services_startup.ps1`:**
Added `-e MANAGEMENT_HEALTH_MAIL_ENABLED=false` to three auth-service container launches:
1. Main auth-service startup (line 157) — suppresses Spring `MailHealthIndicator` so `/actuator/health` returns `{"status":"UP"}` in the SMTP-free test environment.
2. Wrong-password negative test container — same reason; ensures the failure is DB authentication, not mail.
3. Schema-mismatch negative test container — same reason; ensures the failure is `SchemaManagementException`, not mail.

This is a minimal, non-security-weakening change. `HIBERNATE_DDL_AUTO=validate`, least-privilege DB roles, and all schema checks remain unchanged.

#### Test Run `36ed1200` — Full Results

| Service | Role | Actuator Health | Spring Boot Start | Permission Errors |
|---|---|---|---|---|
| `auth-service` | `rr_auth` | ✅ HTTP 200 UP | ✅ Started cleanly | ✅ None |
| `inventory-procurement-service` | `rr_invproc` | ✅ HTTP 200 UP | ✅ Started cleanly | ✅ None |
| `commerce-service` | `rr_commerce` | ✅ HTTP 200 UP | ✅ Started cleanly | ✅ None |
| `customer-finance-service` | `rr_custfin` | ✅ HTTP 200 UP | ✅ Started cleanly | ✅ None |
| `production-service` | `rr_production` | ✅ HTTP 200 UP | ✅ Started cleanly | ✅ None |

#### Negative Security Tests

| Test | Expected | Result |
|---|---|---|
| Wrong DB password for `rr_auth` | `password authentication failed for user "rr_auth"` | ✅ PASS |
| Schema mismatch (`email_verified` column dropped) | `SchemaManagementException` / `missing column [email_verified]` | ✅ PASS |
| Unauthorized DDL (`CREATE TABLE`) under `rr_auth` | `permission denied for schema auth_schema` | ✅ PASS |

#### Cleanup

All 7 disposable resources removed cleanly (label `rrtestid=36ed1200`):
`rr-test-pg-36ed1200`, `rr-test-auth-36ed1200`, `rr-test-comm-36ed1200`, `rr-test-cust-36ed1200`, `rr-test-inv-36ed1200`, `rr-test-prod-36ed1200`, `rr-test-net-36ed1200`.

#### Cumulative Fixes Applied Across Phase 2.2E.5

| Error | Root Cause | Fix | Status |
|---|---|---|---|
| `missing table [form_attachments]` in `inventory-procurement-service` | Root `InventoryProcurementApplication` scanned `common.forms.*` packages | Added `@EntityScan` + `@EnableJpaRepositories` restricting to `inventory.*` and `procurement.*` | ✅ Fixed |
| `missing column [delivery_charges]` and `[order_type]` in `offline_orders` | DDL missing Sprint 9 entity columns | Added to `init-schemas.sql` and `V1_least_privilege_hardening.sql` | ✅ Fixed |
| `missing column [notes]` and `[updated_at]` in `finance_schema.employees` | Entity fields added in Sprint 9, DDL not updated | Added to both DDL files with idempotent `ADD COLUMN IF NOT EXISTS` guards | ✅ Fixed |
| `missing table [form_attachments]` in `customer-finance-service` | No `hibernate.default_schema`; Hibernate resolved unqualified form entities to `customer_schema` instead of `public` | Added `default_schema: public` to `customer-finance-service/src/main/resources/application.yml` | ✅ Fixed |
| Auth-service `/actuator/health` returns HTTP 503 in test env | Spring Mail `MailHealthIndicator` registers as DOWN with empty SMTP host | Added `-e MANAGEMENT_HEALTH_MAIL_ENABLED=false` to test script (test-only setting) | ✅ Fixed |
| False timeouts and early probe failures | 5 JVMs booting simultaneously; probe started at second 0 | Added 35s initial grace period, 240s per-service timeout, 4s poll interval | ✅ Fixed |
| Negative test containers polled too early | 5s sleep insufficient under concurrent JVM load | Changed to `docker inspect` exit polling loop, up to 30s | ✅ Fixed |

#### Files Modified in This Entry

- `infrastructure/docker/postgres/test_services_startup.ps1` — Added `MANAGEMENT_HEALTH_MAIL_ENABLED=false` to 3 auth-service container launches (3 lines added)
- `guide to deploy.md` — Stage 2 status → `COMPLETED — VERIFIED`, Stage 5 status → `READY TO START`, Entry 3 appended

#### Stage Status Changes

| Stage | Previous | Updated |
|---|---|---|
| Stage 2 (Production Configuration & Security) | `IN PROGRESS` | **`COMPLETED — VERIFIED`** |
| Stage 5 (Git Integration) | `IN PROGRESS` | **`READY TO START`** |

- **Commands & Tests Run:**
  - `test_services_startup.ps1` run `36ed1200` → **21/21 PASSED, 0 FAILED** (exit code 0)
  - `git diff --check` → Exit code 0 (zero violations)
  - `git status --short` → 15 modified, 5 untracked, no staged changes

- **Next Recommended Action:**
  - **Stage 5 Git Integration** — Review `git diff` for all 15 modified files, confirm no secrets, create logical atomic commits for Phase 2.2E work, and push to `origin/main`.

---

### Entry 4 — 2026-10-28 | Stage 5 Git Integration & Remote Synchronization COMPLETE

- **Engineer / Agent:** Antigravity Agent
- **Git Branch:** `main` (synchronized with `origin/main` at `901d824617d53509557c677b0af550d66ca0e83f`)
- **Remote Repository:** `https://github.com/rrjaggery-1458/RR_Jaggery.git`
- **Result:** **STAGE 5 COMPLETE & VERIFIED** — 6 commits pushed, working tree clean, local and remote in full parity

#### Work Performed

1. **Stage 5.1 — Read-Only Git Integration Audit:**
   - Evaluated working tree: 15 modified tracked files and 5 untracked operational files.
   - Performed strict pattern search for secrets, tokens, private keys, or passwords across all files: 0 findings/blockers.
   - Confirmed `git diff --check` passed cleanly (exit code 0).
   - Confirmed staging index was empty and isolated development containers were completely unaffected.

2. **Stage 5.2 — Three Atomic Local Commits Created:**
   - **Commit A (`49ba2b3`):** `feat(db): least-privilege runtime roles, schema parity, and DDL validation` (11 files)
     - Hardened `.env.example`, `compose.production.yaml`, `init-schemas.sql` (`NOLOGIN` roles, column parity), `V1_least_privilege_hardening.sql`, `verify_schema_parity.sql`, all 5 service `application.yml` files (parameterized `ddl-auto`, default role users, `default_schema: public` for customer-finance), and restricted JPA entity scanning in `InventoryProcurementApplication.java`.
   - **Commit B (`c2bc613`):** `feat(ops): harden backup and restore, role provisioning, and validation` (7 files)
     - `backup.sh`, `restore.sh`, `test_least_privilege.ps1`, `provision_runtime_roles.ps1`, `provision_runtime_roles.sh`, `test_backup_restore.ps1`, and `test_services_startup.ps1`.
   - **Commit C (`901d824`):** `docs: update operational runbook and deployment guide` (2 files)
     - `BACKUP_AND_ROLLBACK.md` and initial commit of `guide to deploy.md`.

3. **Stage 5.3 — Push and Remote Synchronization Verified:**
   - Successfully pushed all 6 commits to `origin/main`:
     1. `0040e63` (*fix(payment): harden Razorpay checkout and remove TEST_PAYMENT*)
     2. `2d902e1` (*fix(security): harden CORS and forwarded headers*)
     3. `a97321d` (*feat(infra): implement PostgreSQL least-privilege migration infrastructure*)
     4. `49ba2b3` (*feat(db): least-privilege runtime roles, schema parity, and DDL validation*)
     5. `c2bc613` (*feat(ops): harden backup and restore, role provisioning, and validation*)
     6. `901d824` (*docs: update operational runbook and deployment guide*)
   - Executed `git status --short --branch` verifying `## main...origin/main` with clean working tree.
   - Verified commit hash on both local HEAD and remote `origin/main`: `901d824617d53509557c677b0af550d66ca0e83f`.

#### Safeguards Verified
- No VPS or production server accessed or modified.
- No claim of OVHcloud purchase, provisioning, or deployment made.
- No secrets, tokens, or environment files committed or exposed.
- All Sprint 1–10 and Phase 2.1–2.2 historical work intact.

#### Stage Status Changes

| Stage | Previous | Updated |
|---|---|---|
| Stage 5 (Git Integration) | `READY TO START` | **`COMPLETED — VERIFIED`** |
| Stage 6 (OVHcloud CI/CD Implementation) | `PENDING / BLOCKED` | **`PENDING (READY FOR PREPARATION)`** |

- **Commands & Tests Run:**
  - `git branch --show-current` → `main`
  - `git status --short --branch` → `## main...origin/main`
  - `git log -6 --oneline --decorate`
  - `git rev-parse HEAD` & `git rev-parse origin/main` → `901d824617d53509557c677b0af550d66ca0e83f`
  - `git diff --check` → Exit code 0

- **Next Recommended Action:**
  - **Stage 6 OVHcloud CI/CD Implementation Preparation:** Conduct read-only review of `.github/workflows/ci.yml`, verify automated validation and build stages, and document required GitHub Actions repository secrets (`VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, etc.). Requires explicit review and approval before any deployment actions begin.
---

### Entry 5 — 2026-10-28 | Stage 6.1.1: CI/CD Safety Gate & Deployment Guide Reconciliation (Final Safety Review)

- **Engineer / Agent:** Antigravity Agent
- **Git Branch:** `main` (baseline commit `ce19dea9469f8b2f1112f2aab650c2c104048ece`)
- **Files Modified:** `.github/workflows/ci.yml`, `guide to deploy.md`
- **Result:** **STAGE 6.1.1 COMPLETE (LOCAL DIFF PREPARED FOR REVIEW)** — Production deployment gated behind manual confirmation input, SSH configuration hardened with unverified host authenticity noted, failure diagnosis clarified as unconfirmed, and guide reconciled with historical VPS reality

#### Work Performed

1. **Clarified GitHub Actions Failure Diagnosis:**
   - Evaluated failed run `https://github.com/rrjaggery-1458/RR_Jaggery/actions/runs/40644510552/job/122671589738`.
   - The exact failing command in the `Configure SSH` step remains unconfirmed. Because GitHub Actions labels the step in runner execution logs using the first line of the multi-line script (`Run mkdir -p ~/.ssh`), the failing command cannot be identified with certainty from the available error output alone.
   - Candidate failure mechanisms include:
     - `ssh-keyscan -H ""` exiting with status code 1 if `VPS_HOST` is unset or empty in GitHub repository / environment (`production`) secrets.
     - `ssh-keyscan` failing to connect or timing out if port 22 on the VPS host is unreachable from GitHub runner IPs.
     - Malformed `VPS_SSH_KEY` or runner filesystem permissions.
   - The hypothesis that `mkdir -p ~/.ssh` itself failed is unlikely on standard `ubuntu-latest` runners, but the definitive failing line cannot be proven without deeper diagnostic runner logs.

2. **Implemented Manual Confirmation Gate (`ci.yml`):**
   - Configured manual trigger isolation with explicit confirmation:
     - Added `workflow_dispatch` trigger with a required `confirm_production_deploy` boolean input (default: `false`).
     - Gated Job 4 (`deploy-production`) with condition:
       `if: github.ref == 'refs/heads/main' && github.event_name == 'workflow_dispatch' && (inputs.confirm_production_deploy == true || inputs.confirm_production_deploy == 'true')`
     - Preserved automated CI triggers (`push` to `main`, `arch/consolidate-5-services`, and `pull_request` to `main`) for automated build/check jobs (Jobs 1–3: `frontend-check`, `backend-check`, `config-check`).
   - Explicitly documented that setting `confirm_production_deploy: true` authorizes a deployment attempt only; it does NOT ensure that the deployment is safe.
   - Removed absolute safety claims (avoiding statements like "can NEVER deploy accidentally"); safety relies on runner input validation and human review before manual triggering.

3. **Hardened `Configure SSH` Step (`ci.yml`):**
   - Passed `VPS_HOST` and `VPS_SSH_KEY` via step-level `env:` mappings rather than inline command interpolation.
   - Added non-empty validation checks before executing SSH operations.
   - Enforced restrictive permissions (`chmod 700 ~/.ssh` and `chmod 600 ~/.ssh/id_rsa`).
   - Used `printf '%s\n'` to write the private key safely without printing secret values to console logs.
   - Added explicit timeout (`-T 10`) to `ssh-keyscan` with non-empty check on `known_hosts` and error reporting.
   - Documented that `ssh-keyscan` only fetches the presented host key over the network and does NOT verify SSH host authenticity. Verified, pinned host-key trust remains an unresolved prerequisite before production deployment can be authorized.

4. **Reconciled `guide to deploy.md` with Historical VPS Reality:**
   - Corrected outdated claims stating VPS-1 has not been purchased or deployed.
   - Reconciled Stage 3 to reflect that the OVHcloud VPS (Ubuntu 24.04.5 LTS), Docker Compose stack, and Cloudflare SSL termination on `rrdentalneeds.online` are already deployed historically (Sprint 10 baseline).
   - Emphasized that the deployment is NOT ready for release: recent Phase 2.2E least-privilege database roles, schema column additions, and backup/restore procedures have not yet been deployed or verified on the live VPS.
   - Updated Stage 6 status to reflect CI/CD failure analysis and safety gate installation.
   - Updated Stage 7 status to reflect pending reconciliation verification.
   - Updated repository baseline to confirmed Git commit `ce19dea9469f8b2f1112f2aab650c2c104048ece`.

5. **Identified Critical Remaining Production Risks:**
   - **Transfer Asset Deletion Risk Unresolved:** The directory layout on the live VPS cannot be assumed without inspection. Running `rsync -az --delete` risks wiping untracked files, logs, SSL certificates, or persistent volumes on the server if remote directory structures diverge.
   - **SSH Host Authenticity Unverified:** `ssh-keyscan` dynamically trusts the remote key presented over the network on each run, leaving connections open to MITM risks if DNS or IP is intercepted. Pinned host-key verification remains an unresolved prerequisite.
   - **Database Role Mismatch:** Active VPS database lacks Phase 2.2E `rr_*` runtime roles; services will fail authentication if started with new Compose file before migration.
   - **Schema Parity Failure:** Active VPS database lacks new columns (`email_verified`, `order_type`, `delivery_charges`, `notes`, `updated_at`); services with `HIBERNATE_DDL_AUTO=validate` will fail startup.
   - **Server Resource Constraints:** 4 GB RAM VPS risks out-of-memory crashes if `deploy.sh` attempts Maven and npm builds on the host while Docker containers run.

#### Safeguards Verified
- No deployment or remote SSH connection executed.
- No production database, DNS, SSL certificates, or server files touched.
- No secrets or credentials exposed.
- All Sprint 1–10 and Phase 2.1–2.2 historical work intact.
- No files committed or pushed (local diff ready for review).
---

### Entry 6 — 2026-10-28 | Stage 6.1.2–6.1.3: Deployment Safety Audit and Hard Lock

- **Engineer / Agent:** Antigravity Agent
- **Git Branch:** `main` (baseline commit `ce19dea9469f8b2f1112f2aab650c2c104048ece`)
- **Files Modified:** `.github/workflows/ci.yml`, `guide to deploy.md`
- **Result:** **STAGE 6.1.3 COMPLETE (LOCAL DIFF PREPARED FOR REVIEW)** — Production deployment job hard-locked via `if: false &&`; automated CI checks unaffected

#### Stage 6.1.2: Read-Only Safety Audit Findings

1. **Baseline Confirmed:** HEAD commit `ce19dea9469f8b2f1112f2aab650c2c104048ece` on `main`. Working tree contained exactly 2 modified files: `.github/workflows/ci.yml` and `guide to deploy.md`. 0 staged, 0 untracked.

2. **Push/PR Isolation Verified:** For `push` and `pull_request` events, `github.event_name` is never `'workflow_dispatch'`, so the `deploy-production` job condition evaluated to `false` for all automated triggers. Only Jobs 1–3 (`frontend-check`, `backend-check`, `config-check`) ran automatically.

3. **`rsync --delete` Risk — Unresolved:**
   - Source: `./` (GitHub runner workspace root, containing checkout + downloaded `frontend/dist/` and `services/` JARs).
   - Destination: `/opt/rr-jaggery/` on the live VPS.
   - Exclusions: Only `.git`, `node_modules`, `services/*/target/classes`, `.env`.
   - **Critical**: Server-side Cloudflare Origin CA TLS certificates (`infrastructure/nginx/ssl/`), runtime logs, and any files present on the VPS but absent from the repository root would be **deleted** by `rsync --delete`. Directory layout on VPS cannot be confirmed without on-host inspection.

4. **SSH Host-Key Trust — Not Trustworthy:**
   - `ssh-keyscan -T 10 -H "$VPS_HOST"` retrieves whatever key is presented by the IP at the moment of the runner; there is no comparison against a trusted, pinned fingerprint.
   - The mechanism is Trust-On-First-Use (TOFU) over the public internet on every workflow run. A compromised DNS, BGP route, or network path would allow acceptance of a rogue host key without warning.
   - **Verdict:** SSH host authenticity is NOT verified. Pinned host-key trust remains an unresolved prerequisite.

5. **Deployment Still Executable with Confirmation — Safety Gap:**
   - With the Stage 6.1.1 gating, any repository collaborator could trigger `workflow_dispatch` on `main` with `confirm_production_deploy: true`, executing the full production deployment despite all outstanding risks above.
   - This was identified as insufficient protection while the four blocking prerequisites remain unresolved.

6. **`deploy.sh` On-Host Build Risk:**
   - `deploy.sh` runs `npm ci && npm run build` and `mvn -B clean package -DskipTests` on the live 4 GB RAM VPS in addition to already-downloaded CI artifacts.
   - Redundant and resource-exhaustive; poses severe OOM risk while Docker containers are active.
   - `deploy.sh` line 48 also executes `git rev-parse --short HEAD`, which fails under `set -euo pipefail` because `.git/` is excluded by `rsync`.

#### Stage 6.1.3: Hard Safety Lock Applied

- Changed `deploy-production` job condition:
  ```yaml
  # Before (Stage 6.1.1):
  if: github.ref == 'refs/heads/main' && github.event_name == 'workflow_dispatch' && (inputs.confirm_production_deploy == true || inputs.confirm_production_deploy == 'true')

  # After (Stage 6.1.3):
  if: false && github.ref == 'refs/heads/main' && github.event_name == 'workflow_dispatch' && (inputs.confirm_production_deploy == true || inputs.confirm_production_deploy == 'true')
  ```
- `false &&` short-circuits to `false` statically. GitHub Actions skips the job unconditionally under any trigger.
- Jobs 1–3 (`frontend-check`, `backend-check`, `config-check`) remain completely unaffected.
- To re-enable production deployment: remove `false &&` ONLY after all four prerequisites are verified and explicitly approved.

#### Prerequisites Required Before Hard Lock May Be Removed (Stage 6.2)

| # | Prerequisite | Status |
|---|---|---|
| 1 | Trusted SSH host-key pinning (pinned fingerprint, not TOFU `ssh-keyscan`) | `UNRESOLVED` |
| 2 | `rsync --delete` exclusion audit — protect TLS certs, logs, runtime data | `UNRESOLVED` |
| 3 | Phase 2.2E DB roles and schema migration rehearsed and verified on live VPS | `UNRESOLVED` |
| 4 | Redundant on-host npm/Maven builds removed or isolated in `deploy.sh` | `UNRESOLVED` |

#### Safeguards Verified (Stage 6.1.3)
- No deployment or remote SSH connection executed.
- No production database, DNS, SSL certificates, or server files touched.
- No secrets or credentials exposed.
- All Sprint 1–10 and Phase 2.1–2.2 historical work intact.
- No files committed or pushed (local diff ready for review).

---

### Entry 7 — 2026-10-28 | Stage 6.2: Read-Only OVHcloud VPS Reconciliation Audit

- **Engineer / Agent:** Antigravity Agent
- **Audit Date:** 2026-10-28
- **Local Git Baseline:** `e582aa9ca329734f03bdf671af621e3e9decfae5` on `main` (synchronized with `origin/main`)
- **Live Server Target:** OVHcloud VPS-1 (`15.235.246.77`, hostname: `vps-a4f1c1ef`, Ubuntu 24.04.5 LTS, 2 vCPU, 4 GB RAM, 40 GB NVMe)
- **Public Domains:** `rrdentalneeds.online` and `www.rrdentalneeds.online` (direct A-record DNS pointing to `15.235.246.77`)
- **Result:** **STAGE 6.2 AUDIT COMPLETE** — Live production state fully inventoried and reconciled; 6 critical deployment blockers confirmed

#### Work Performed & Evidence Collected

1. **SSH Identity & Access Verification:**
   - Target host `15.235.246.77` port 22 verified open.
   - Verified host key against pre-existing client `~/.ssh/known_hosts` (ED25519 `SHA256:Qv5L5HzAl22I//3L5n5oB5h0cZO1arcWLOIaVxtxwmg`).
   - Strict host key checking maintained (no `StrictHostKeyChecking=no` used).
   - Authenticated successfully using dedicated key `~/.ssh/id_ed25519` (comment: `rr-jaggery-vps`) as user `ubuntu`.
   - Verified passwordless `sudo -n` available for read-only Docker inspection.

2. **Host & Resource Inventory:**
   - **OS:** Ubuntu 24.04.5 LTS (Noble Numbat), Linux kernel `6.8.0-147-generic` (x86_64).
   - **Uptime & Load:** Up 23+ hours, load average `0.03, 0.01, 0.00`.
   - **CPU:** 2 vCores (`nproc` = 2).
   - **Memory:** Total 3.7 GiB, Used 2.3 GiB, Available 1.4 GiB, Free 129 MiB. **Swap: 0 B** (no swap configured).
   - **Disk:** `/dev/sda1` 38 GB total, 6.5 GB used (18%), 32 GB available. Inodes: 166,138 / 5,111,808 used (4%).
   - **Docker:** Docker Engine 29.1.3, containerd 2.2.1, Compose plugin installed.

3. **Live Directory Structure Discrepancy (CRITICAL FINDING):**
   - The application repository on the VPS is located at `/home/ubuntu/RR_Jaggery/`, **NOT `/opt/rr-jaggery/`**.
   - `/opt/rr-jaggery` does not exist on the live server.
   - Repository scripts (`vps-provision.sh`, `deploy.sh`, `backup.sh`) and CI/CD workflow (`ci.yml`) erroneously hardcode `/opt/rr-jaggery/`. Any rsync to `/opt/rr-jaggery` would deploy to a dead path and leave active containers untouched.

4. **Deployed Version & Local Working Tree on Server:**
   - VPS checkout is on branch `main` at commit `8931e2aae6062b3df978cf24b6e95c724a5a320d` (**8 commits behind repository HEAD `e582aa9`**).
   - The server has **uncommitted modifications** in `/home/ubuntu/RR_Jaggery/`:
     - `compose.production.yaml`: modified environment quoting (`SMTP_USERNAME: "${SMTP_USERNAME}"`).
     - `infrastructure/nginx/nginx.production.conf`: modified SSL cert paths (`/etc/nginx/ssl/live/rrdentalneeds.online/`).
     - `services/auth-service/src/main/resources/application.yml`: added `bootstrap.admin` credentials config.
     - `frontend/package.json` & `package-lock.json`: local dependency lock updates.
     - Untracked files: `compose.http-test.override.yaml`, `infrastructure/nginx/nginx.http-test.conf`.
   - Live frontend asset is `index-tQPXQ6Zy.js`, confirming the running build predates current local build (`index-BCELE11u.js`).

5. **Container Inventory & Runtime Health:**
   - Exactly 7 containers running: `rr-gateway`, `rr-auth-service`, `rr-commerce-service`, `rr-customer-finance-service`, `rr-inventory-procurement-service`, `rr-production-service`, `rr-postgres`.
   - Zero container restarts (`RestartCount=0`), zero OOM kills (`OOMKilled=false`).
   - Combined container memory footprint: ~1.82 GiB / 3.73 GiB.
   - **`rr-gateway` status:** `Up (unhealthy)`. Healthcheck command (`wget --spider -q http://localhost/`) fails because port 80 redirects to 443, and internal wget cannot verify the domain-specific Let's Encrypt certificate on `localhost`. (Fixed in current repo `compose.production.yaml` with `--no-check-certificate`, but not yet deployed).
   - All 5 backend microservices verified **UP and healthy** via public HTTPS endpoints (`/api/v1/auth/health`, etc.).

6. **PostgreSQL & Database Parity Findings:**
   - PostgreSQL 16.15 running in `rr-postgres` container on database `rr_jaggery_db`.
   - All 9 schemas present (`public`, `auth_schema`, `commerce_schema`, `customer_schema`, `inventory_schema`, `procurement_schema`, `production_schema`, `finance_schema`, `notification_schema`).
   - **Schema Column Parity:** All 5 required columns from `V1_least_privilege_hardening.sql` are **CONFIRMED PRESENT** (`auth_schema.users.email_verified`, `customer_schema.offline_orders.delivery_charges`, `customer_schema.offline_orders.order_type`, `finance_schema.employees.notes`, `finance_schema.employees.updated_at`).
   - **Runtime Role Parity:** Least-privilege roles (`rr_auth`, `rr_commerce`, `rr_custfin`, `rr_invproc`, `rr_production`, `rr_app_migrator`) are **CONFIRMED MISSING**. Only superuser `postgres` exists.
   - Active microservices currently connect using legacy `DB_USER=postgres` superuser credentials.

7. **Configuration & Secrets Inventory (`.env`):**
   - Legacy variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — **PRESENT**.
   - Least-privilege variables: `RR_*_DB_USER` and `RR_*_DB_PASSWORD` — **MISSING**.
   - Payment configuration: `RAZORPAY_ENABLED=false` — **CONFIRMED DISABLED** on production.
   - Admin bootstrap credentials: `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` — **PRESENT**.
   - Spring profile: `SPRING_PROFILES_ACTIVE=prod` — **PRESENT**.

8. **Networking, Firewall & TLS:**
   - UFW firewall: **ACTIVE** and strictly configured. Only ports 22, 80, 443 allowed. Port 5432 and microservice ports (8081–8086) are strictly blocked from public ingress.
   - Live TLS Certificate: Valid Let's Encrypt certificate issued Oct 27, 2026, expiring Jan 25, 2027 (`CN=rrdentalneeds.online`, SANs include `www.rrdentalneeds.online`).
   - Certificate stored in volume `rr_jaggery_ssl_certs` at `/etc/nginx/ssl/live/rrdentalneeds.online/`. Note: repository `nginx.production.conf` erroneously references `/etc/nginx/ssl/live/fullchain.pem` without the domain subfolder.
   - **Certbot Renewal:** **MISSING**. No systemd timer or cron job is configured for automated certificate renewal.

9. **Backups & Live Assets:**
   - Default backup location `/var/backups/rr-jaggery` does not exist on the server.
   - One historical manual database dump exists at `/home/ubuntu/backups/rr_jaggery_20261027_095949.dump` (146 KB, custom pg_dump format).
   - Nightly backup cron job (`backup.sh`) is **MISSING** from both root and ubuntu crontabs.
   - Uploaded assets exist in volume `rr_jaggery_uploads_data` (`products/prod_42d8a27098a4.jpg`, `prod_80b57274b249.jpg`, ~4.2 MB total).

#### Confirmed Blockers Prior to Production Deployment

| # | Blocker | Severity | Description | Required Resolution |
|---|---|---|---|---|
| **B1** | **Server Directory Path Mismatch** | **CRITICAL** | Repo/CI/scripts assume `/opt/rr-jaggery/`, but server runs from `/home/ubuntu/RR_Jaggery/`. | Reconcile target path in CI workflow, runbook, and provisioning scripts. |
| **B2** | **Destructive `rsync --delete` Risk** | **CRITICAL** | Running `rsync -az --delete` to the server would wipe live `.env`, server modifications, backups, and SSL volume data. | Explicitly configure `--exclude` rules for `.env*`, `backups/`, and SSL volumes before running rsync. |
| **B3** | **Nginx SSL Certificate Path Divergence** | **HIGH** | Repo `nginx.production.conf` points to `/etc/nginx/ssl/live/fullchain.pem`, while Let's Encrypt stores at `.../live/rrdentalneeds.online/fullchain.pem`. | Align Nginx certificate paths in the repository to match server volume layout. |
| **B4** | **Database Runtime Roles Missing** | **HIGH** | `compose.production.yaml` references `RR_*_DB_USER` and `PASSWORD`, but roles do not exist in PostgreSQL. Deploying new Compose would crash all services. | Rehearse and execute role provisioning (`provision_runtime_roles.sh`) and populate `.env` on VPS before updating Compose. |
| **B5** | **Uncommitted Server-Side Edits** | **MEDIUM** | VPS has local changes in Compose and application.yml (`SMTP_USERNAME`, `bootstrap.admin`). | Merge necessary environment variable improvements back into repository. |
| **B6** | **Automated Backups & Certbot Renewal Missing** | **MEDIUM** | No crontab or timers exist on the VPS for nightly database backups or 90-day Let's Encrypt certificate renewal. | Install host crontab entries for `backup.sh` and Certbot renewal. |

#### Safeguards Verified
- Strictly read-only commands executed on live server.
- Zero production containers, networks, volumes, or files restarted, stopped, modified, or deleted.
- Zero database DDL or DML writes performed.
- Zero credentials or secrets printed or exposed in logs or documentation.
- Production deployment hard lock in `ci.yml` remains fully intact.
- All Sprint 1–10 and Phase 2.1–2.2 historical work preserved.

---

### Entry 8 — 2026-10-28 | Stage 6.3A: Local-Only Configuration Reconciliation Plan

- **Engineer / Agent:** Antigravity Agent
- **Date:** 2026-10-28
- **Local Git Baseline:** `e582aa9ca329734f03bdf671af621e3e9decfae5` on `main`
- **Result:** **STAGE 6.3A COMPLETE** — Comprehensive reconciliation plan authored; zero production or repository code changes made

#### Summary of Work Performed

1. **Local Repository Verification:**
   - Local `HEAD` and `origin/main` confirmed at `e582aa9ca329734f03bdf671af621e3e9decfae5`.
   - Production deployment hard lock verified intact (`if: false && ...` in `.github/workflows/ci.yml`).
   - Entries 1 through 7 in `guide to deploy.md` confirmed present and preserved.

2. **Standalone Reconciliation Plan Authored:**
   - Created comprehensive engineering roadmap at [`docs/reports/stage-6.3A-reconciliation-plan.md`](file:///e:/RR%20Jaggery/docs/reports/stage-6.3A-reconciliation-plan.md).
   - Addressed all 6 core workstreams:
     - **Workstream A (Deployment Directory & Transfer Safety):** Recommends adopting `/home/ubuntu/RR_Jaggery/` as canonical path; eliminates destructive `--delete` flag on initial sync; establishes strict exclusions (`.env*`, `backups/`, test configs); specifies server-side backup before any file sync.
     - **Workstream B (Nginx & TLS Reconciliation):** Resolves path mismatch (`/etc/nginx/ssl/live/rrdentalneeds.online/`); modernizes Nginx HTTP/2 directives; plans Certbot renewal cron/timer before 90-day expiry (Jan 25, 2027).
     - **Workstream C (PostgreSQL Least-Privilege Migration):** Proposes a safe 7-step sequence (Snapshot -> Isolated Drill -> DDL & Grants -> Role Password Provisioning -> `.env` Population -> Direct Role Login Check -> Service Switch) with instant rollback to `DB_USER=postgres` if needed.
     - **Workstream D (Backup & Restore Readiness):** Plans backup path standardization to `/home/ubuntu/backups/`, automated nightly cron setup, and isolated restore rehearsal to verify table counts and sequence alignment.
     - **Workstream E (Resource & Application Health Readiness):** Recommends 2 GB swap on VPS, explicit container `mem_limit` (~2.7 GiB total cap), removal of redundant on-host Maven/npm builds from `deploy.sh`, and updating gateway healthcheck to `--no-check-certificate`.
     - **Workstream F (GitHub Actions Deployment Safety):** Recommends pinned host-key verification (`ED25519 SHA256:Qv5L5HzAl22I...`); maintains hard lock throughout local preparation and rehearsal.

3. **Prioritized Decision Register Created:**
   - 12 decisions logged covering canonical paths, transfer safety, role provisioning, resource limits, and verification criteria.

4. **Safeguards Verified:**
   - Zero remote SSH connections made to the VPS.
   - Zero application code, Compose files, Dockerfiles, or CI workflows modified.
   - No files staged, committed, or pushed.
   - Production deployment hard lock remains fully enabled.

5. **Next Recommended Stage:**
   - **Stage 6.3B: Local Repository Reconciliation:** Implement local documentation, path, Nginx, and Compose adjustments in the local working tree without modifying the live server.

---

### Entry 9 — 2026-10-28 | Stage 6.3A Review Amendment

- **Engineer / Agent:** Antigravity Agent
- **Date:** 2026-10-28
- **Local Git Baseline:** `e582aa9ca329734f03bdf671af621e3e9decfae5` on `main`
- **Result:** **STAGE 6.3A REVIEW AMENDMENT COMPLETE** — Reconciliation plan corrected in 7 areas; zero production or code changes made

#### Summary of Work Performed

1. **Repository and Hard Lock Verified:**
   - Local `HEAD` and `origin/main` confirmed at `e582aa9ca329734f03bdf671af621e3e9decfae5`.
   - Production deployment hard lock confirmed intact: `if: false &&` in `.github/workflows/ci.yml` line 143.
   - Entries 1 through 8 in `guide to deploy.md` confirmed present and preserved.

2. **Reconciliation Plan Amended:**
   - `docs/reports/stage-6.3A-reconciliation-plan.md` updated with a new dated amendment section (Section 6).
   - Seven targeted corrections applied:

   | Ref | Area | Correction |
   |---|---|---|
   | C1 / D07 | Gateway Healthcheck | REJECTED `--no-check-certificate`. Recommended internal backend probe: `http://auth-service:8081/api/v1/auth/health` via Docker internal network — avoids TLS entirely and requires only a Compose change. |
   | C2 / D04 | DB Migration Sequence | Clarified two-phase dependency: V1 DDL creates roles as NOLOGIN (must run first); `provision_runtime_roles.sh` sets LOGIN+PASSWORD (must run second). Idempotency boundaries documented. Pre-validation query against `information_schema.role_table_grants` added. Special `pg_roles` grant requirement for auth-service noted. |
   | C3 / D06 | Backup Minimum Criteria | Defined minimum criteria: non-empty gzip dump, `gzip -t` integrity check, SHA256 sidecar, size materially > 146 KB, isolated restore rehearsal (schema + row-count verification) required before any live migration. |
   | C4 / D08 | Memory Budget | Budgets revised to observed-peak × 1.5 (totalling ~3.0 GiB). Swap is a transient spike buffer only. Limits applied via Compose environment block (not Dockerfile) for per-environment flexibility. 48-hour monitoring required before finalising. |
   | C5 / D11 | SSH Host-Key Pinning | Clarified that `VPS_KNOWN_HOSTS` must contain the full known_hosts public key line, not a fingerprint. `ssh-keyscan` replaced with direct write to `~/.ssh/known_hosts`; step fails closed if secret is empty. |
   | C6 / D05 | Server-Only Changes | Added 3-step preservation protocol: server snapshot → per-file diff → classify each change (discard / port to repo / exclude) before any rsync. Rule: do not sync any file until the repository version incorporates or deliberately supersedes the server's change. |
   | C7 / D01, D02 | Path & Transfer | Full inventory of all `/opt/rr-jaggery/` references enumerated (8 files / 14 lines). Non-destructive 5-step transfer sequence documented: dry run → no-`--delete` sync → server inspection → `nginx -t` → gated `--delete`. |

3. **Safeguards Confirmed:**
   - Zero remote SSH connections made to the VPS.
   - Zero application code, Compose files, Dockerfiles, CI workflows, migrations, or secrets modified.
   - No files staged, committed, or pushed.
   - Production deployment hard lock remains fully enabled.
   - All Sprint 1–10, Phase 2.1–2.2, and all prior deployment guide history preserved.

4. **Outstanding Prerequisites Before Stage 6.3B:**
   - Trusted SSH host-key verification: update `VPS_KNOWN_HOSTS` secret to full known_hosts entry format.
   - Safe review of `rsync --delete` and server-only file preservation per C6.
   - Isolated DB role migration rehearsal with backup criteria per C3 fully met.
   - Memory limits must be measurement-driven (48-hour post-deployment monitoring).
   - Gateway healthcheck corrected to internal backend probe per C1 (no `--no-check-certificate`).

5. **Next Recommended Stage:**
   - **Stage 6.3B: Local Repository Reconciliation:** Implement the local repository edits identified in the decision register (D01, D02, D03, D05, D07, D09, D11) as individual, reviewable changes against the `e582aa9` baseline, with the production hard lock remaining in place throughout.

---

### Entry 10 — 2026-10-28 | Stage 6.3B: Backup and Isolated Restore Rehearsal Planning

- **Engineer / Agent:** Antigravity Agent
- **Date:** 2026-10-28
- **Local Git Baseline:** `e582aa9ca329734f03bdf671af621e3e9decfae5` on `main`
- **Result:** **STAGE 6.3B COMPLETE** — Comprehensive backup & isolated restore rehearsal plan created; zero production or code changes made; rehearsal safely halted due to absence of local test data

#### Summary of Work Performed

1. **Inspection of Existing Recovery Infrastructure:**
   - Performed deep source-level inspection across:
     - `infrastructure/scripts/backup.sh`: Analyzed `pg_dumpall` pipeline, pseudo-TTY prevention (`docker exec -i`), `gzip -t` check, `sha256sum` generation, and retention pruning rules.
     - `infrastructure/scripts/restore.sh`: Audited pre-flight checks, 10s countdown, automatic safety snapshot, service quiescence, stream restoration, and post-restore schema checks.
     - `infrastructure/docker/postgres/provision_runtime_roles.sh` (located under `docker/postgres/`, not `scripts/`): Audited 5-role password pre-validation, SQL escaping, stdin streaming, and `ON_ERROR_STOP=1`.
     - `infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql`: Audited DDL role creation, schema ownership transfer to `rr_migrator`, and preflight column drift checks.
     - `compose.production.yaml` & `docker-compose.yml`: Audited network isolation, secret injection, and port mappings.
     - `infrastructure/docker/postgres/verify_schema_parity.sql`: Audited 8-domain schema checks, table count (>= 58), table ownership, and role permission isolation matrix.

2. **Defined Multi-Dimensional Acceptance Criteria:**
   - Replaced simple file-size checks with an objective verification matrix:
     - Exit-code verification: `${PIPESTATUS[0]} -eq 0` and `${PIPESTATUS[1]} -eq 0`.
     - Archive stream integrity: `gzip -t` and `tar -tzf`.
     - Cryptographic verification: `sha256sum -c`.
     - Completeness checks: verification of all 8 application schemas, extensions, and `public.form_definitions`.
     - Row-count and sequence state comparison: pre-backup vs post-restore parity query across critical business tables.

3. **Formulated Isolated Restore Rehearsal Architecture:**
   - Designed disposable local container rehearsal workflow using `postgres:16-alpine` with random drill token, labeled `rrdisposable=true`.
   - Guaranteed zero production exposure: strictly unmapped port (no `-p 5432:5432`), ephemeral volumes, and disposable test passwords.
   - Designed validation of media uploads archive in an isolated throwaway Docker volume.

4. **Identified Local Test Data Status & Safety Halt:**
   - Verified that zero `*.sql.gz`, `*.dump`, or uploads archives currently exist in the local workspace.
   - In strict compliance with instructions ("Do not run the rehearsal yet unless all necessary local backup artifacts already exist and can be conclusively identified as non-production test data. If they do not exist, report exactly what is missing and stop"), the physical execution of the restore drill was halted.

5. **Formulated 9 Production Migration Recovery Gates:**
   - Documented explicit prerequisites blocking any live server schema or role migration until fresh verified backups exist, isolated restore succeeds, least-privilege permissions pass audit, rollback runbook is verified, and CI hard lock is maintained.

6. **Safeguards Verified:**
   - Zero remote SSH connections made to the VPS.
   - Zero production containers, files, or database objects modified.
   - Zero credentials exposed or printed.
   - Production deployment hard lock in `.github/workflows/ci.yml` remains fully intact (`if: false &&`).
   - Entries 1 through 9 and all prior deployment history preserved without modification.

7. **Next Recommended Stage:**
   - **Stage 6.3C: Local Repository Reconciliation & Configuration Hardening:** Implement the local repository adjustments identified in the Stage 6.3A Decision Register (canonical path alignment, transfer exclusions, Nginx SSL path corrections, gateway healthcheck backend probe, and pinned SSH host key), keeping the CI deployment hard lock strictly engaged.

---

### Entry 11 — 2026-10-28 | Stage 6.3B.1: Recovery Plan Corrections

- **Engineer / Agent:** Antigravity Agent
- **Date:** 2026-10-28
- **Local Git Baseline:** `e582aa9ca329734f03bdf671af621e3e9decfae5` on `main`
- **Result:** **STAGE 6.3B.1 COMPLETE** — Recovery plan amended with 7 targeted corrections; zero production or code changes made

#### Summary of Work Performed

1. **Backup Acceptance Criteria Corrected:**
   - Eliminated the fixed `> 250 KB` size threshold. Backup file size is designated as strictly informational.
   - Acceptance gate defined exclusively by stream exit codes (`PIPESTATUS`), non-empty check (`-s`), compression verification (`gzip -t`), SHA-256 sidecar validation, schema and object completeness, row-count and sequence state parity, and isolated restore success.

2. **Schema Counting & Parity Audit Scope Reconciled:**
   - Clearly distinguished the 8 domain schemas (`auth_schema`, `commerce_schema`, `customer_schema`, `finance_schema`, `notification_schema`, `inventory_schema`, `procurement_schema`, `production_schema`) from the shared system schema `public` (which holds `form_definitions` and `form_submissions`). Total application schema count = 9.
   - Clarified from SQL source that `verify_schema_parity.sql` checks the 8 domain schemas and specific `public.form_%` tables owned by `rr_migrator`, and does not audit default PostgreSQL system tables in `public`.

3. **Auth-Service Health Check Query Verified:**
   - Inspected source of `HealthCheckController.java` (`/api/v1/auth/health`) and `SystemHealthService.java` (`/api/v1/auth/system-health`).
   - Confirmed that `/api/v1/auth/health` performs zero database operations (returns in-memory map), and `SystemHealthService` queries only `SELECT version()` and `pg_stat_user_tables` / `information_schema.tables`.
   - Verified that `pg_catalog.pg_roles` is **not queried** by auth-service health endpoints; no special catalog or `pg_monitor` grant is required. Documented isolated verification test.

4. **Rollback Duration Reclassified:**
   - Under-30-second rollback reclassified as a **target to validate**, not an assumed guarantee.
   - Rehearsal scope expanded to measure the full cycle: environment restoration (`.env`), service container restart, stabilization, and endpoint health verification.

5. **Gateway Health Check Separation Established:**
   - Corrected recommendation: Keep Nginx reverse proxy health probe (checking Nginx liveness directly via a dedicated non-redirecting local endpoint like `/healthz` without TLS) decoupled from backend microservice health.
   - End-to-end routing check through Nginx to backend treated as an additional synthetic check, not a substitute. Neither configuration modified in this stage.

6. **Accurate File Accounting Documented:**
   - Confirmed Git status: 1 tracked modified file (`guide to deploy.md`), 1 pre-existing untracked report from Stage 6.3A (`docs/reports/stage-6.3A-reconciliation-plan.md`), and 1 report from Stage 6.3B (`docs/reports/stage-6.3B-backup-restore-plan.md`).
   - Zero files staged; no files removed or committed.

7. **No-Production-Access Boundary Reaffirmed:**
   - Reasserted strict boundary: zero VPS or production DB access.
   - Restore rehearsal status remains **DESIGNED & READY, PENDING TEST ARTIFACTS**; cannot be marked passed until known-safe non-production test archives are available.

8. **Next Recommended Stage:**
   - **Stage 6.3C: Local Repository Reconciliation & Configuration Hardening:** Prepare local repository adjustments as reviewable changes against baseline `e582aa9`, with CI hard lock strictly engaged.

---

### Entry 12 — 2026-10-28 | Stage 6.3C: Local Reconciliation, Test Verification & Pipeline Hardening

- **Engineer / Agent:** Antigravity Agent
- **Date:** 2026-10-28
- **Local Git Baseline:** `e582aa9ca329734f03bdf671af621e3e9decfae5` on `main`
- **Result:** **STAGE 6.3C COMPLETE** — All local deployment files reconciled; isolated migration rehearsal (26/26) and backup/restore drill (20/20) PASSED; frontend and backend builds verified; ready for commit and live server reconciliation

#### Summary of Work Performed

1. **Deployment Path Standardization (Workstream 2):**
   - Updated `infrastructure/scripts/deploy.sh` (`PROJECT_DIR="${PROJECT_DIR:-/home/ubuntu/RR_Jaggery}"`).
   - Updated `infrastructure/scripts/restore.sh` (`COMPOSE_FILE="${COMPOSE_FILE:-/home/ubuntu/RR_Jaggery/compose.production.yaml}"`).
   - Updated `docs/architecture/DEPLOYMENT.md` across all 8 deployment command references.
   - Preserved backup destination `/var/backups/rr-jaggery` in scripts while keeping deployment paths distinct.

2. **Nginx, TLS & Gateway Health Reconciliation (Workstream 3):**
   - Configured dedicated non-redirecting local health endpoint in `infrastructure/nginx/nginx.production.conf` under port 80 block (`location = /healthz { access_log off; default_type text/plain; return 200 "OK\n"; }`), placed before `location /`.
   - Updated gateway health check in `compose.production.yaml` to `test: ["CMD-SHELL", "wget -q --spider http://127.0.0.1/healthz || exit 1"]`.
   - Reconciled SSL certificate directives to match Certbot layout: `/etc/nginx/ssl/live/rrdentalneeds.online/fullchain.pem` and `privkey.pem`.
   - Modernized HTTPS server block syntax from deprecated `listen 443 ssl http2;` to `listen 443 ssl;` and `http2 on;`.
   - Wrapped `SMTP_USERNAME` and `SMTP_PASSWORD` in quotes in Compose to match live server settings.

3. **PostgreSQL Migration Rehearsal (Workstream 4):**
   - Executed full automated least-privilege verification suite (`infrastructure/docker/postgres/test_least_privilege.ps1`) against an isolated disposable PostgreSQL 16 container.
   - Verified 8 domain schemas + `public` application tables, `email_verified` column presence, `rr_migrator` table ownership, identity sequences, positive/negative role isolation matrix, search_path resolution, and script idempotency.
   - **Result:** **26/26 PASSED, 0 FAILED**. Disposable container cleaned up cleanly.

4. **Backup & Restore Integration Drill (Workstream 5):**
   - Executed disposable backup and restore drill (`infrastructure/docker/postgres/test_backup_restore.ps1`) across disposable source and target containers.
   - Verified cluster dump streaming without pseudo-TTY, gzip integrity, SHA-256 sidecar generation/matching, uploads archiving, restore into clean target, schema/table/record verification, and archive corruption rejection.
   - **Result:** **20/20 PASSED, 0 FAILED**. All disposable containers and test directories pruned.

5. **Resource Budgeting & Deploy Script Resilience (Workstream 6):**
   - Updated `deploy.sh` to retrieve Git commit/branch with fallback (`${DEPLOY_COMMIT:-$(git rev-parse ... || echo 'unknown')}`), removing strict `.git` dependency.
   - Made `log()` function in `deploy.sh` resilient to missing or non-writable log directories.
   - Added conditional compilation in `deploy.sh`: skips on-host `npm run build` if `frontend/dist/index.html` exists; skips on-host Maven packaging if all 5 service JARs are present, avoiding OOM risks on the 4 GB VPS.

6. **GitHub Actions CI/CD Pipeline Hardening (Workstream 7):**
   - Replaced unpinned Trust-On-First-Use `ssh-keyscan` in `.github/workflows/ci.yml` with pinned `VPS_KNOWN_HOSTS` verification. Fails closed if the secret is missing or host key does not match.
   - Replaced `rsync -az --delete` with safe, non-destructive synchronization targeting `/home/ubuntu/RR_Jaggery/`.
   - Added explicit exclusions for `.git`, `.env*`, `backups/`, `node_modules`, `target/classes`, `compose.http-test.override.yaml`, and `infrastructure/nginx/nginx.http-test.conf`.
   - Maintained production deployment hard lock (`if: false && ...`) intact.

7. **Local Code Compilation & Verification (Workstream 8):**
   - `git diff --check`: **PASSED** (0 formatting/whitespace issues).
   - Frontend build & TypeScript typecheck: **PASSED** (`tsc -b && vite build` built in 27.01s).
   - Backend Maven compilation: **PASSED** across all 7 reactor modules in 13.58s.
