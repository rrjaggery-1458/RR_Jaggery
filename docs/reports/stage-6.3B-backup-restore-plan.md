# RR Jaggery — Stage 6.3B: Backup and Isolated Restore Rehearsal Plan

> **Document Status:** Authoritative Planning & Operational Rehearsal Specification  
> **Repository:** `E:\RR Jaggery`  
> **Branch:** `main`  
> **Baseline Commit:** `e582aa9ca329734f03bdf671af621e3e9decfae5`  
> **Date:** 2026-10-28  
> **Operational Boundary:** **PLANNING ONLY — STRICTLY LOCAL & READ-ONLY**. No VPS access, no production database access, no live script execution, no changes to application code, Compose files, or CI workflow.

---

## 1. Executive Summary & Objectives

Before executing any schema modification, least-privilege role assignment, or container redeployment on the live OVHcloud VPS (`15.235.246.77`), a mathematically verifiable, end-to-end database and media recovery procedure must be established and rehearsed.

Stage 6.2 confirmed that the live production database contains all five schema columns from `V1_least_privilege_hardening.sql`, but the six least-privilege roles (`rr_migrator`, `rr_auth`, `rr_commerce`, `rr_custfin`, `rr_invproc`, `rr_production`) are missing and services currently connect as superuser `postgres`. Additionally, no automated nightly backup cron job is active, and only one manual dump exists on the server (`rr_jaggery_20261027_095949.dump`, 146 KB).

This plan defines:
1. A rigorous inspection of existing backup and recovery assets in the codebase.
2. Objective backup acceptance criteria that reject incomplete or corrupt archives.
3. An isolated, zero-risk restore rehearsal design using disposable local Docker containers.
4. An isolated rehearsal procedure for the least-privilege migration (`V1_least_privilege_hardening.sql` + `provision_runtime_roles.sh`).
5. Strict recovery gates that block live production migration until all rehearsals succeed.

---

## 2. Inspection of Existing Recovery Implementation

A detailed source-level audit of the existing scripts and configurations was conducted:

| Component | Repository Path | Key Commands / Logic | Critical Dependencies & Environment Variables | Error Handling & Safeguards | Identified Operational Risks |
|---|---|---|---|---|---|
| **Backup Script** | [`infrastructure/scripts/backup.sh`](file:///e:/RR%20Jaggery/infrastructure/scripts/backup.sh) | `docker exec -i "${CONTAINER_NAME}" pg_dumpall -U "${DB_USER}" | gzip -c > "${TMP_DB_FILE}"`<br>Tar of `UPLOADS_DIR` (`/app/uploads`) from commerce container or volume mount. | `BACKUP_DIR` (`/var/backups/rr-jaggery`)<br>`PG_CONTAINER` (`rr-postgres`)<br>`PG_USER` (`postgres`)<br>`PG_DATABASE` (`rr_jaggery_db`)<br>`COMMERCE_CONTAINER` (`rr-commerce-service`)<br>`MIN_FREE_MB` (500 MB) | `set -euo pipefail`<br>`trap cleanup_tmp EXIT INT TERM`<br>Atomic rename (`.tmp_*` to target)<br>`gzip -t` verification<br>`sha256sum` sidecar generation<br>Pruning threshold (`MIN_KEEP_BACKUPS=3`) | Hardcodes `/var/backups/rr-jaggery` (absent on VPS).<br>Uses `pg_dumpall` which dumps entire cluster (including existing roles/tablespaces), requiring superuser to restore.<br>Uploads tar uses fallback to `docker volume ls` which may miss custom volume prefixes. |
| **Restore Script** | [`infrastructure/scripts/restore.sh`](file:///e:/RR%20Jaggery/infrastructure/scripts/restore.sh) | `gunzip -c "$BACKUP_FILE" | docker exec -i "${TARGET_CONTAINER}" psql -U "${TARGET_DB_USER}"`<br>Uploads tar extraction via temporary Alpine container into `uploads_data` volume. | `BACKUP_FILE` (arg 1)<br>`UPLOADS_FILE` (arg 2)<br>`TARGET_CONTAINER` (`rr-postgres`)<br>`TARGET_DB_USER` (`postgres`)<br>`TARGET_DB_NAME` (`rr_jaggery_db`)<br>`COMPOSE_FILE` (`/opt/rr-jaggery/compose.production.yaml`)<br>`SKIP_SAFETY_SNAPSHOT` (false)<br>`RESTORE_FORCE` (false) | `gzip -t` check<br>`sha256sum -c` verification<br>10s safety abort countdown<br>Automatic pre-restore snapshot generation<br>Stops 5 microservices before restore<br>Validates 8 domain schemas and table count (>= 58). | Hardcodes `COMPOSE_FILE` to `/opt/rr-jaggery/compose.production.yaml` (fails on VPS path `/home/ubuntu/RR_Jaggery/`).<br>Extracts uploads by running `rm -rf /data/*` (destructive wipe).<br>Table check allows table count down to 58 but lacks row count or sequence checks. |
| **Role Provisioning Script** | [`infrastructure/docker/postgres/provision_runtime_roles.sh`](file:///e:/RR%20Jaggery/infrastructure/docker/postgres/provision_runtime_roles.sh)<br>*(Note: Located under `docker/postgres/`, not `scripts/`)* | Streams SQL to stdin:<br>`ALTER ROLE rr_* WITH LOGIN PASSWORD '<escaped_password>';`<br>via `docker exec -i ... psql -v ON_ERROR_STOP=1` | `RR_AUTH_DB_PASSWORD`<br>`RR_COMMERCE_DB_PASSWORD`<br>`RR_CUSTFIN_DB_PASSWORD`<br>`RR_INVPROC_DB_PASSWORD`<br>`RR_PRODUCTION_DB_PASSWORD`<br>`CONTAINER_NAME` (default `rr-postgres`)<br>`ADMIN_USER` (`postgres`) | Pre-validates all 5 variables are non-empty.<br>Rejects control characters (`\r\n\0`).<br>Escapes single quotes.<br>Streams via stdin to prevent passwords showing in process list (`ps -ef`).<br>`ON_ERROR_STOP=1`. | Script assumes roles ALREADY exist as `NOLOGIN`. If executed before `V1_least_privilege_hardening.sql`, `ALTER ROLE` fails.<br>Requires admin password passed via `DB_PASSWORD` or environment. |
| **Migration SQL** | [`infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql`](file:///e:/RR%20Jaggery/infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql) | Creates `rr_migrator` and 5 runtime roles as `NOLOGIN`.<br>Creates 8 schemas; transfers ownership to `rr_migrator`.<br>Grants `USAGE` on schemas and `SELECT, INSERT, UPDATE, DELETE` on tables.<br>Grants sequence permissions. | Executed as database superuser (`postgres`) against `rr_jaggery_db`. | Wrapped in `BEGIN ... COMMIT;`<br>`\set ON_ERROR_STOP on`<br>Preflight column drift checks.<br>`CREATE ROLE IF NOT EXISTS`<br>`CREATE SCHEMA IF NOT EXISTS`<br>`ADD COLUMN IF NOT EXISTS`. | Re-running ownership change (`ALTER SCHEMA ... OWNER TO rr_migrator`) succeeds only if `rr_migrator` exists.<br>Does not grant `pg_roles` catalog access to `rr_auth`, which auth-service healthcheck requires. |
| **Compose Production** | [`compose.production.yaml`](file:///e:/RR%20Jaggery/compose.production.yaml) | Defines 7 services (`postgres`, 5 JVM services, `gateway`). Mounts `init-schemas.sql` into `/docker-entrypoint-initdb.d/`. | Environment variables `RR_*_DB_USER` and `RR_*_DB_PASSWORD:?...` for all 5 services.<br>`HIBERNATE_DDL_AUTO: validate`. | Port 5432 is private to `rr-network` (not published). | Any missing `RR_*_DB_PASSWORD` variable prevents Compose stack startup immediately (`:?` constraint). |
| **Compose Development** | [`docker-compose.yml`](file:///e:/RR%20Jaggery/docker-compose.yml) | Development stack for local iteration. Publishes port 5432 to host. Uses default password `postgres_dev_password`. | `DB_PASSWORD:-postgres_dev_password`. | Default credentials enabled for local developer convenience. | Publishes port `5432:5432` to host; must NEVER be used for production rehearsal without port isolation. |
| **Parity Audit SQL** | [`infrastructure/docker/postgres/verify_schema_parity.sql`](file:///e:/RR%20Jaggery/infrastructure/docker/postgres/verify_schema_parity.sql) | Comprehensive plpgsql validation script. | Executed via `psql` as superuser. | Checks 8 schemas, 6 roles, >= 58 tables, `email_verified` column, table ownership by `rr_migrator`, sequence existence, and negative/positive role permission isolation matrix. | Throws fatal exception on ANY single failure. 100% deterministic pass/fail signal. |

---

## 3. Backup Acceptance Criteria

Backup validation must never rely solely on file existence or file size. A 146 KB dump may contain only DDL or may have terminated prematurely due to network timeouts, buffer exhaustion, or unhandled SQL exceptions.

### 3.1 Database Backup Acceptance Specification

To be accepted as a valid production recovery artifact, a PostgreSQL backup must satisfy all six of the following criteria:

| Check # | Requirement | Validation Method / Command | Acceptance Gate |
|---|---|---|---|
| **DB-1** | **Process Exit Code** | Capturing `PIPESTATUS` from stream: `docker exec -i rr-postgres pg_dumpall -U postgres | gzip -c > dump.sql.gz` | `${PIPESTATUS[0]} -eq 0` AND `${PIPESTATUS[1]} -eq 0`. Any non-zero exit code fails immediately. |
| **DB-2** | **File Existence & Non-Trivial Size** | `[ -s dump.sql.gz ]` and size comparison. | File exists, size > 0 bytes, and size is consistent with active data volume (minimum acceptable threshold: > 250 KB for current schema + seeded datasets). |
| **DB-3** | **Archive Stream Integrity** | `gzip -t dump.sql.gz` | Exits with 0. Confirms gzip magic numbers, CRC-32 checksum, and uncompressed stream integrity. |
| **DB-4** | **Cryptographic Verification** | `sha256sum -c dump.sql.gz.sha256` | Output indicates `OK`. Guarantees zero byte corruption during disk write or transfer. |
| **DB-5** | **Schema & Object Completeness** | Grep decompress stream or TOC inspection: | Archive MUST contain DDL statements for all 8 application schemas (`auth_schema`, `commerce_schema`, `customer_schema`, `finance_schema`, `notification_schema`, `inventory_schema`, `procurement_schema`, `production_schema`), `public.form_definitions`, and extensions `uuid-ossp` and `pgcrypto`. |
| **DB-6** | **Pre/Post Row Count & Sequence Snapshot** | Execute pre-backup row-count SQL query (see §3.3 below) and save to `pre_backup_counts.json`. | Post-restore row counts across all key tables match pre-backup counts with 0 discrepancy; sequence `last_value` states are identical. |

### 3.2 Uploads Backup Acceptance Specification

| Check # | Requirement | Validation Method / Command | Acceptance Gate |
|---|---|---|---|
| **UP-1** | **Process Exit Code** | Capturing pipe exit: `tar -czf uploads.tar.gz -C /volume/_data .` | Exit code 0. |
| **UP-2** | **Archive Listing & Structure** | `tar -tzf uploads.tar.gz` | Exits with 0. Contains directory structure `products/` and files `prod_*.jpg`. |
| **UP-3** | **Cryptographic Verification** | `sha256sum -c uploads.tar.gz.sha256` | Output indicates `OK`. |
| **UP-4** | **Media Fidelity** | Count extracted files and calculate collective hash: `find . -type f -exec sha256sum {} + | sort | sha256sum` | Matches pre-backup collective hash. |

### 3.3 Method for Capturing Pre-Backup Row Counts & Sequence States

Before taking a live backup or initiating a migration, execute the following query via `psql` and store the output:

```sql
-- Capture Table Row Counts Across All Domains
SELECT 
    schemaname, 
    relname AS tablename, 
    n_live_tup AS estimated_rows
FROM pg_stat_user_tables
WHERE schemaname IN (
    'auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
    'inventory_schema', 'notification_schema', 'procurement_schema', 'production_schema'
)
ORDER BY schemaname, tablename;

-- Exact Row Counts for Critical Business Tables
SELECT 'auth_schema.users' AS tbl, count(*) FROM auth_schema.users
UNION ALL SELECT 'auth_schema.roles', count(*) FROM auth_schema.roles
UNION ALL SELECT 'commerce_schema.products', count(*) FROM commerce_schema.products
UNION ALL SELECT 'commerce_schema.orders', count(*) FROM commerce_schema.orders
UNION ALL SELECT 'commerce_schema.order_items', count(*) FROM commerce_schema.order_items
UNION ALL SELECT 'customer_schema.customers', count(*) FROM customer_schema.customers
UNION ALL SELECT 'inventory_schema.inventory_items', count(*) FROM inventory_schema.inventory_items
UNION ALL SELECT 'procurement_schema.purchase_orders', count(*) FROM procurement_schema.purchase_orders
UNION ALL SELECT 'production_schema.production_runs', count(*) FROM production_schema.production_runs;

-- Capture Identity Sequence States
SELECT 
    schemaname,
    sequencename,
    last_value,
    start_value,
    is_cycled
FROM pg_sequences
WHERE schemaname IN (
    'auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
    'inventory_schema', 'notification_schema', 'procurement_schema', 'production_schema'
)
ORDER BY schemaname, sequencename;
```

---

## 4. Isolated Restore Rehearsal Design

### 4.1 Rehearsal Architecture & Safety Isolation

The restore rehearsal must execute in a completely disposable local environment that cannot affect running containers or the host network:

```
+-----------------------------------------------------------------------------------+
| Host System (Local Developer Machine)                                             |
|                                                                                   |
|  [Backup Artifacts]                                                               |
|    |- test_db.sql.gz + .sha256                                                    |
|    |- test_uploads.tar.gz + .sha256                                               |
|           |                                                                       |
|           v (Streamed via docker exec -i)                                         |
|  +-----------------------------------------------------------------------------+  |
|  | Disposable Docker Container: rr-disp-rst-<TEST_ID>                          |  |
|  | Image: postgres:16-alpine                                                   |  |
|  | Labels: rrdisposable=true, rrtestid=<TEST_ID>                                |  |
|  | Ports: NONE PUBLISHED TO HOST (Port 5432 unmapped)                         |  |
|  | Auth: POSTGRES_PASSWORD=disposable_test_pass_<TEST_ID>                      |  |
|  | Storage: Ephemeral anonymous volume                                          |  |
|  +-----------------------------------------------------------------------------+  |
|           |                                                                       |
|           v                                                                       |
|  [Automated Post-Restore Audit: verify_schema_parity.sql + Row Count Parity]      |
|           |                                                                       |
|           v                                                                       |
|  [Container Teardown & Volume Prune: docker rm -f -v rr-disp-rst-<TEST_ID>]       |
+-----------------------------------------------------------------------------------+
```

### 4.2 Step-by-Step Rehearsal Execution Procedure

1. **Generate Unique Drill Session Token:**
   ```bash
   DRILL_ID=$(openssl rand -hex 4)
   CONTAINER_NAME="rr-disp-rst-${DRILL_ID}"
   TEST_DB="rr_jaggery_db"
   TEST_PASS="drill_pass_${DRILL_ID}"
   ```

2. **Launch Unexposed Disposable PostgreSQL 16 Container:**
   ```bash
   docker run -d \
       --name "${CONTAINER_NAME}" \
       --label "rrdisposable=true" \
       --label "rrtestid=${DRILL_ID}" \
       -e POSTGRES_DB="${TEST_DB}" \
       -e POSTGRES_PASSWORD="${TEST_PASS}" \
       postgres:16-alpine
   ```
   *Crucial:* No `-p 5432:5432` flag is passed. The database is strictly isolated from the local host network.

3. **Wait for Database Readiness:**
   ```bash
   until docker exec "${CONTAINER_NAME}" pg_isready -U postgres -d "${TEST_DB}" >/dev/null 2>&1; do
       sleep 1
   done
   ```

4. **Verify Backup Archive Checksum & Integrity:**
   ```bash
   sha256sum -c "${BACKUP_FILE}.sha256"
   gzip -t "${BACKUP_FILE}"
   ```

5. **Stream Database Restoration:**
   ```bash
   # Stream without pseudo-TTY (-t) to prevent binary corruption
   gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" psql -U postgres -d "${TEST_DB}" -v ON_ERROR_STOP=1
   ```

6. **Post-Restore Schema Object Verification:**
   ```bash
   # Check all 8 domain schemas exist
   EXPECTED_SCHEMAS="auth_schema commerce_schema customer_schema finance_schema inventory_schema notification_schema procurement_schema production_schema"
   for s in $EXPECTED_SCHEMAS; do
       docker exec -i "${CONTAINER_NAME}" psql -U postgres -d "${TEST_DB}" -tAc \
           "SELECT 1 FROM information_schema.schemata WHERE schema_name='$s';" | grep -q "1" || {
           echo "FAIL: Missing schema $s"
           exit 1
       }
   done
   ```

7. **Table Count & Column Parity Audit:**
   ```bash
   TABLE_COUNT=$(docker exec -i "${CONTAINER_NAME}" psql -U postgres -d "${TEST_DB}" -tAc "
       SELECT count(*) FROM information_schema.tables
       WHERE table_schema IN ('auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
                              'inventory_schema', 'notification_schema', 'procurement_schema', 'production_schema')
          OR (table_schema = 'public' AND table_name LIKE 'form_%');
   ")
   if [ "${TABLE_COUNT}" -lt 58 ]; then
       echo "FAIL: Restored table count (${TABLE_COUNT}) < expected 58"
       exit 1
   fi
   ```

8. **Row Count & Sequence Comparison:**
   Compare the restored tables and sequences against the captured pre-backup snapshot. All counts must be 100% identical.

9. **Uploads Volume Restoration Drill:**
   ```bash
   UPLOADS_VOL="rr-disp-upl-${DRILL_ID}"
   docker volume create "${UPLOADS_VOL}"
   docker run --rm -v "${UPLOADS_VOL}:/data" -v "$(dirname "${UPLOADS_FILE}")":/backup alpine \
       sh -c "tar -xzf /backup/$(basename "${UPLOADS_FILE}") -C /data"
   # Verify media files
   docker run --rm -v "${UPLOADS_VOL}:/data:ro" alpine ls -la /data/products/
   docker volume rm "${UPLOADS_VOL}"
   ```

10. **Cleanup & Teardown:**
    ```bash
    docker rm -f "${CONTAINER_NAME}"
    ```

### 4.3 Current Status of Local Test Artifacts

A comprehensive search across `E:\RR Jaggery` confirmed:
- Zero `*.sql.gz` or `*.dump` backup archives exist locally.
- Zero `*uploads*.tar.gz` archive files exist locally.
- The only historical backup artifact is located on the remote VPS host at `/home/ubuntu/backups/rr_jaggery_20261027_095949.dump` (146 KB), which cannot be retrieved in this stage due to the strict zero-remote-connection safety rule.

> [!IMPORTANT]
> **OPERATIONAL STOP MANDATE:**
> Because no verified non-production local backup archives currently exist in the repository, **the restore rehearsal cannot and will not be executed during Stage 6.3B**.
> Attempting to run a restore drill without verified test data would either require generating synthetic dummy data (which does not validate the production schema state) or connecting to the VPS (which violates the safety rules). The rehearsal procedure is fully specified here and ready to execute once a verified test archive is made available in an authorized follow-up stage.

---

## 5. Least-Privilege Migration Rehearsal Plan

Once a restored database is verified in the disposable container, the least-privilege migration sequence must be rehearsed against it to validate role creation, privileges, and Hibernate application startup.

### 5.1 Step-by-Step Migration Rehearsal

1. **Execute Migration DDL (`V1_least_privilege_hardening.sql`):**
   ```bash
   docker exec -i "${CONTAINER_NAME}" psql -U postgres -d "${TEST_DB}" -v ON_ERROR_STOP=1 \
       < infrastructure/docker/postgres/migrations/V1_least_privilege_hardening.sql
   ```
   *Verify:*
   - Output logs confirmation of `CREATE ROLE ... (NOLOGIN)` for `rr_migrator` and the 5 service roles.
   - Schema ownership transferred to `rr_migrator`.
   - Idempotent column additions report no errors.

2. **Execute Runtime Role Password Provisioning:**
   ```bash
   export RR_AUTH_DB_PASSWORD="test_auth_pwd_${DRILL_ID}"
   export RR_COMMERCE_DB_PASSWORD="test_comm_pwd_${DRILL_ID}"
   export RR_CUSTFIN_DB_PASSWORD="test_cust_pwd_${DRILL_ID}"
   export RR_INVPROC_DB_PASSWORD="test_invp_pwd_${DRILL_ID}"
   export RR_PRODUCTION_DB_PASSWORD="test_prod_pwd_${DRILL_ID}"

   bash infrastructure/docker/postgres/provision_runtime_roles.sh \
       "${CONTAINER_NAME}" "${TEST_DB}" postgres
   ```
   *Verify:*
   - All 5 roles are updated with `WITH LOGIN PASSWORD ...`.
   - Passwords are never logged or exposed in process arguments.

3. **Execute Comprehensive Schema & Permission Parity Audit:**
   ```bash
   docker exec -i "${CONTAINER_NAME}" psql -U postgres -d "${TEST_DB}" -v ON_ERROR_STOP=1 \
       < infrastructure/docker/postgres/verify_schema_parity.sql
   ```
   *Verify:*
   - All 8 domain schemas confirmed.
   - 100% of application tables owned by `rr_migrator`.
   - Identity sequences verified.
   - Positive privileges confirmed (e.g. `rr_auth` can read `auth_schema.users`).
   - Negative privileges confirmed (e.g. `rr_auth` has NO access to `commerce_schema.orders`; `rr_production` has NO insert on `purchase_order_lines`).

4. **Verify Repeatability (Idempotency Test):**
   Re-run `V1_least_privilege_hardening.sql` a second time against the hardened database.
   *Acceptance:* The script MUST execute cleanly from start to finish without aborting or altering existing data.

5. **Application Schema Validation Simulation:**
   Spin up a temporary service container (or execute Hibernate schema validation check via Maven test) configured with `HIBERNATE_DDL_AUTO=validate` and connecting as `rr_auth` / `rr_commerce`.
   *Acceptance:* Hibernate completes schema validation without throwing `SchemaManagementException`.

---

## 6. Production Migration Recovery Gates

No schema migration, least-privilege role provisioning, or Compose redeployment on the live OVHcloud VPS is permitted until the following nine gates have been formally checked and approved:

- [ ] **Gate 1: Fresh Live Database Backup Generated & Validated**  
  Full `pg_dumpall` taken on the VPS; non-empty; `gzip -t` passes; SHA-256 sidecar written; size exceeds the 146 KB baseline.
- [ ] **Gate 2: Fresh Live Uploads Volume Backup Generated & Validated**  
  Tar archive of `rr_jaggery_uploads_data` generated; non-empty; `tar -tzf` passes; SHA-256 sidecar written.
- [ ] **Gate 3: Off-Host Safety Copy Secured**  
  Both archives safely downloaded to local/off-host storage prior to initiating any live changes.
- [ ] **Gate 4: Isolated Restore Rehearsal PASSED**  
  Disposable container restore of the fresh backup passes 100% of checks (8 schemas, >= 58 tables, row count parity, sequence integrity).
- [ ] **Gate 5: Least-Privilege Migration Drill PASSED**  
  `V1_least_privilege_hardening.sql` and `provision_runtime_roles.sh` execute cleanly on restored data; `verify_schema_parity.sql` reports 100% pass.
- [ ] **Gate 6: Rapid Rollback Procedure Rehearsed & Ready**  
  Rollback procedure (reverting `.env` variables to `DB_USER=postgres` and restarting containers) is documented with a target recovery time < 30 seconds.
- [ ] **Gate 7: Server Configuration Drift Reconciled**  
  Server-side changes in Compose, Nginx, and `application.yml` reviewed and classified (discard, port, or exclude).
- [ ] **Gate 8: Host Resource Safety Buffer Configured**  
  2 GB swapfile created on the VPS to prevent kernel OOM killer during container recreation.
- [ ] **Gate 9: Pipeline Hard Lock Maintained**  
  GitHub Actions workflow `.github/workflows/ci.yml` line 143 retains `if: false && ...` until final post-migration signoff.

---

## 7. Rollback Runbook (Production Emergency Recovery)

If any microservice fails during production migration (e.g. Hibernate schema validation error or database authentication failure):

### Scenario A: Rollback Least-Privilege Service Configuration (< 30 seconds)
If the database schema is intact but services cannot authenticate:
```bash
# 1. Update /home/ubuntu/RR_Jaggery/.env
sed -i 's/^RR_AUTH_DB_USER=.*/RR_AUTH_DB_USER=postgres/' .env
sed -i 's/^RR_COMMERCE_DB_USER=.*/RR_COMMERCE_DB_USER=postgres/' .env
sed -i 's/^RR_CUSTFIN_DB_USER=.*/RR_CUSTFIN_DB_USER=postgres/' .env
sed -i 's/^RR_INVPROC_DB_USER=.*/RR_INVPROC_DB_USER=postgres/' .env
sed -i 's/^RR_PRODUCTION_DB_USER=.*/RR_PRODUCTION_DB_USER=postgres/' .env
# Re-set service passwords to the superuser DB_PASSWORD
# 2. Restart services
docker compose -f compose.production.yaml up -d
```

### Scenario B: Full Database Snapshot Restoration (< 3 minutes)
If DDL or data corruption occurred:
```bash
# Execute restore targeting the pre-migration snapshot
bash infrastructure/scripts/restore.sh \
    /home/ubuntu/backups/pre_migration_safety_snapshot.sql.gz \
    /home/ubuntu/backups/pre_migration_uploads_snapshot.tar.gz
```

---

## 8. Summary of Findings & Next Stage Recommendation

### Summary of Completed Stage 6.3B Work
- Completed detailed source-level audit of backup, restore, provisioning, migration, Compose, and parity audit files.
- Defined explicit, multi-dimensional backup acceptance criteria (exit codes, checksums, gzip integrity, schema completeness, row-count parity, and sequence preservation) replacing simplistic file-size checks.
- Formulated an isolated restore rehearsal procedure running in unexposed, labeled disposable containers with test-only credentials.
- Formulated a least-privilege migration rehearsal procedure with 100% parity verification.
- Established nine mandatory recovery gates that strictly prevent premature live database operations.
- Confirmed that zero local backup archives exist in the repository, correctly halting rehearsal execution per safety instructions.

### Recommended Next Stage
**Stage 6.3C: Local Repository Reconciliation & Configuration Hardening**
Implement the safe local repository adjustments identified in the Stage 6.3A Decision Register (canonical path alignment to `/home/ubuntu/RR_Jaggery/`, transfer exclusions, Nginx SSL path corrections, gateway health check backend probe, and pinned SSH host key), keeping the CI deployment hard lock strictly engaged.

---

## 9. Stage 6.3B.1: Recovery Plan Corrections — 2026-10-28

> **Correction Status:** Formal engineering review and correction applied to Stage 6.3B plan.  
> **Reviewer:** Antigravity Agent  
> **Date:** 2026-10-28  
> **Scope:** Documentation-only corrections. No VPS connection, no production backup or restore, no code/Compose edits, no staging, committing, or pushing. CI hard lock unchanged.

The following seven corrections are formally incorporated into this recovery plan:

---

### Correction 1 — Removal of Fixed `> 250 KB` Backup-Size Pass/Fail Criterion (Amends §3.1 / DB-2)

- **Prior Issue:** §3.1 (table entry DB-2) suggested a fixed minimum threshold of `> 250 KB` as a pass/fail criterion.
- **Correction:** A fixed size threshold is **eliminated** as an acceptance gate. File size is **strictly informational**.
- **Objective Acceptance Criteria:** Acceptance is determined exclusively by:
  1. Exit code validation: `${PIPESTATUS[0]} -eq 0` and `${PIPESTATUS[1]} -eq 0` on the dump-and-compress stream.
  2. Non-empty check: `[ -s dump.sql.gz ]` (file exists and size > 0).
  3. Compression integrity: `gzip -t dump.sql.gz` returns 0.
  4. Cryptographic checksum: `sha256sum -c dump.sql.gz.sha256` validates successfully.
  5. Object & schema completeness: Verified presence of all 8 application domain schemas, `public.form_definitions`, and extensions.
  6. Row count and sequence state parity: Pre-backup snapshot query matches post-restore counts with 100% parity.
  7. Successful isolated restore: Successful execution of restoration in a disposable container.

---

### Correction 2 — Schema Counting & Coverage Reconciliation (Amends §2, §3, §4)

- **Prior Ambiguity:** Various sections alternately referenced "8 schemas", "9 schemas", or "8 domain schemas + public".
- **Precise Classification:**
  - **Domain Schemas (8):** `auth_schema`, `commerce_schema`, `customer_schema`, `finance_schema`, `notification_schema`, `inventory_schema`, `procurement_schema`, `production_schema`.
  - **System / Shared Schema (1):** `public`, which hosts shared application entities (`form_definitions`, `form_submissions`).
  - **Total Schema Count:** 8 domain schemas + `public` = 9 total schemas containing application objects.
- **What `verify_schema_parity.sql` Actually Audits:**
  Source inspection of [`infrastructure/docker/postgres/verify_schema_parity.sql`](file:///e:/RR%20Jaggery/infrastructure/docker/postgres/verify_schema_parity.sql) confirms:
  1. Specifically iterates over `expected_schemas` (the 8 domain schemas only) via `information_schema.schemata`.
  2. Validates existence of 6 roles (`rr_migrator`, `rr_auth`, `rr_commerce`, `rr_custfin`, `rr_invproc`, `rr_production`).
  3. Counts tables in the 8 domain schemas **plus** tables in `public` where `tablename LIKE 'form_%'` (asserting total count $\ge 58$).
  4. Asserts that 100% of tables across the 8 domain schemas and `public.form_%` are owned by `rr_migrator`.
  5. Verifies identity sequences in `commerce_schema` and `customer_schema`.
  6. Verifies specific role-level positive and negative permissions (e.g. `rr_auth` cannot read `commerce_schema.orders`; `rr_custfin` has SELECT on `public.form_definitions`).
  The audit script does NOT check or require ownership of unrelated or default PostgreSQL objects in `public`.

---

### Correction 3 — Verification of Auth-Service Health-Check Query & Privileges (Amends §2 / Migration SQL)

- **Prior Unverified Assertion:** Prior reports suggested auth-service health-check queries `pg_catalog.pg_roles` and might require a `pg_monitor` grant.
- **Codebase Source Evidence:**
  1. [`HealthCheckController.java`](file:///e:/RR%20Jaggery/services/auth-service/src/main/java/com/rrjaggery/auth/controller/HealthCheckController.java) (endpoint `/api/v1/auth/health`):
     Contains **zero database queries**. It returns an immediate in-memory Map:
     ```java
     Map.of("service", "auth-service", "status", "UP", "port", 8081, "schema", "auth_schema", "version", "1.0.0-SNAPSHOT")
     ```
  2. [`SystemHealthService.java`](file:///e:/RR%20Jaggery/services/auth-service/src/main/java/com/rrjaggery/auth/service/SystemHealthService.java) (endpoint `/api/v1/auth/system-health`):
     Executes only:
     - `SELECT version()`
     - `SELECT schemaname, relname, n_live_tup FROM pg_stat_user_tables WHERE schemaname IN (...) ORDER BY schemaname, relname`
     - Fallback: `SELECT table_schema AS schemaname, table_name AS relname, 0 AS n_live_tup FROM information_schema.tables WHERE ...`
  3. **Findings:** The auth-service does **NOT** query `pg_catalog.pg_roles` in any health endpoint. Standard PostgreSQL runtime connection permissions allow querying `version()`, `pg_stat_user_tables`, and `information_schema.tables`.
- **Isolated Verification Test:**
  To guarantee runtime compatibility without modifying production migration SQL now, test in the disposable rehearsal container:
  ```bash
  docker exec -i "${CONTAINER_NAME}" psql -U rr_auth -d "${TEST_DB}" -c "
      SELECT version();
      SELECT schemaname, relname, n_live_tup FROM pg_stat_user_tables WHERE schemaname = 'auth_schema' LIMIT 1;
  "
  ```
  If this succeeds, no extra catalog grants or `pg_monitor` role memberships are required.

---

### Correction 4 — Reclassification of Rollback Duration as a Target to Validate (Amends §6 Gate 6, §7)

- **Prior Issue:** Previous text stated "target recovery time < 30 seconds" as if it were a guaranteed property.
- **Correction:** Under-30-second rollback is explicitly reclassified as a **target to validate**, not an assumed guarantee.
- **Comprehensive Rollback Rehearsal Scope:**
  A true rollback test in the isolated environment must include and measure the full sequence:
  1. Reverting the environment configuration (`.env` restoration from pre-migration backup).
  2. Sequential or parallel microservice container restarts (`docker compose up -d`).
  3. Waiting for services to stabilize.
  4. Executing health verification probes against all service endpoints.
  The total elapsed time must be measured and recorded during the rehearsal.

---

### Correction 5 — Gateway Health-Check Separation (Amends §8 / Next Stages)

- **Prior Proposal:** Stage 6.3A review proposed changing the gateway healthcheck to probe the internal backend `http://auth-service:8081/api/v1/auth/health`.
- **Correction:** Keep Nginx health and backend service health as **separate concerns**:
  - **Gateway / Nginx Healthcheck:** Must test the Nginx reverse proxy itself using a dedicated non-redirecting local endpoint (e.g. `http://127.0.0.1/healthz` or stub_status on an internal port/location that returns 200 without TLS and without a 301 redirect).
  - **Backend Health:** Backend microservice health is monitored independently via individual service healthchecks (e.g. `auth-service:8081`).
  - **End-to-End Routing Probe:** An end-to-end check testing that Nginx successfully proxies traffic to backends should be treated as an **additional higher-level synthetic check**, not a substitute for Nginx's basic container liveness.
- **Rule:** Neither configuration is modified in this stage. Both options will be formally structured in Stage 6.3C planning.

---

### Correction 6 — Accurate File Status Accounting (Amends §7 / Stage 6.3B Documentation)

- **Clarification of Repository Changes:**
  - **Tracked & Modified:**
    - [`guide to deploy.md`](file:///e:/RR%20Jaggery/guide%20to%20deploy.md): Tracked master deployment guide, updated with historical entries 8, 9, 10, and 11.
  - **Pre-Existing Untracked (from Stage 6.3A):**
    - [`docs/reports/stage-6.3A-reconciliation-plan.md`](file:///e:/RR%20Jaggery/docs/reports/stage-6.3A-reconciliation-plan.md): Authored in Stage 6.3A and amended in Stage 6.3A Review; remained untracked throughout.
  - **Created in Stage 6.3B:**
    - [`docs/reports/stage-6.3B-backup-restore-plan.md`](file:///e:/RR%20Jaggery/docs/reports/stage-6.3B-backup-restore-plan.md): Authored in Stage 6.3B; amended in Stage 6.3B.1.
  - **Staged Files:** **Zero files staged** (`git diff --cached` is empty).
  - No untracked files have been removed, staged, or committed.

---

### Correction 7 — Reaffirmation of No-Production-Access Boundary (Amends §4.3, §6)

- **Absolute Operational Boundary:**
  - Zero connections to the live OVHcloud VPS (`15.235.246.77`).
  - Zero extraction of production databases or production uploads.
- **Restore Rehearsal Prerequisite:**
  The restore rehearsal **CANNOT be marked as PASSED** until a known-safe, non-production test backup archive and test uploads archive are available and successfully restored in an isolated disposable environment.
  Until such test artifacts exist, the rehearsal remains in status: **DESIGNED & READY, PENDING TEST ARTIFACTS**.
