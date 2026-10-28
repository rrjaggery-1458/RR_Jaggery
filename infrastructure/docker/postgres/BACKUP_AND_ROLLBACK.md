# RR Jaggery — PostgreSQL Backup, Verification & Rollback Runbook

**Phase:** 2.2E.4
**Target Architecture:** Multi-Schema PostgreSQL 16 Least-Privilege Role Structure + Persistent Uploads Volume
**Database Name:** `rr_jaggery_db`
**Security Classification:** Highly Sensitive Operations

---

## 1. Overview & Operational Principles

This runbook defines procedures for logical PostgreSQL backups, persistent file upload archives, integrity verification, isolated test restoration, and operational disaster recovery for the RR Jaggery platform.

### Core Safety Rules
1. **Never Stream with `-t` (Pseudo-TTY):** Allocating a pseudo-TTY corrupts binary/compressed dump streams by translating newline characters (`\n` to `\r\n`). Always use `docker exec -i` or non-interactive `docker exec`.
2. **Atomic Writes via Temporary Files:** Never stream directly to the target backup filename. Write to `.tmp_<filename>`, verify size and archive integrity, then atomically rename (`mv`).
3. **Never Delete the Sole Backup:** Backup retention pruning must verify that at least a minimum threshold of valid backups exist (default: 3) before deleting older files.
4. **Safety Snapshot Before Live Restore:** Before restoring any backup to an operational database, create a pre-restore safety snapshot of the target database state.
5. **Coupled Database & Uploads Protection:** Database rows in `commerce_schema.product_images` and order attachments depend on files stored in the Docker `uploads_data` volume. Backups must capture both components to prevent orphaned links.
6. **Off-Host Replication is Mandatory:** Local backups stored on the VPS host or inside Docker volumes cannot protect against host disk corruption, hypervisor failure, or total server loss. Local backups must be synced to an off-host vault.

---

## 2. Automated Backup Execution

The primary production backup script is located at:
[`infrastructure/scripts/backup.sh`](file:///e:/RR%20Jaggery/infrastructure/scripts/backup.sh)

### 2.1 Running an Ad-Hoc Backup
Execute as the `deploy` or `root` user on the host VPS:

```bash
cd /opt/rr-jaggery
bash infrastructure/scripts/backup.sh
```

### 2.2 Environment Configuration Overrides
The script accepts optional environment variable overrides:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `BACKUP_DIR` | `/var/backups/rr-jaggery` | Directory where compressed archives and `.sha256` files are stored. |
| `RETENTION_DAYS` | `14` | Threshold (in days) beyond which old archives are pruned. |
| `MIN_KEEP_BACKUPS` | `3` | Minimum number of archives to preserve regardless of age. |
| `MIN_FREE_MB` | `500` | Pre-flight disk space requirement (MB) on `BACKUP_DIR`. |
| `BACKUP_UPLOADS` | `true` | When `true`, also archives `/app/uploads` from `commerce-service`. |
| `PG_CONTAINER` | `rr-postgres` | Container name of the PostgreSQL instance. |
| `PG_USER` | `postgres` | Superuser / administrative database user. |
| `PG_DATABASE` | `rr_jaggery_db` | Primary database name. |

### 2.3 Output Artifacts
A completed backup produces four files in `$BACKUP_DIR`:
1. `rr_jaggery_db_YYYYMMDD_HHMMSS.sql.gz` — Gzip-compressed cluster-wide PostgreSQL logical dump (including roles, schemas, tables, and sequence states).
2. `rr_jaggery_db_YYYYMMDD_HHMMSS.sql.gz.sha256` — Cryptographic SHA256 checksum of the database dump.
3. `rr_jaggery_uploads_YYYYMMDD_HHMMSS.tar.gz` — Compressed tar archive of customer and product uploads.
4. `rr_jaggery_uploads_YYYYMMDD_HHMMSS.tar.gz.sha256` — Cryptographic SHA256 checksum of the uploads archive.

---

## 3. Manual / Selective Backup Procedures

For migration checkpoints or plain SQL reference schemas:

### 3.1 Custom Binary Archive (`.dump`)
Custom binary format (`-Fc`) allows parallel table restoration, selective schema recovery, and internal table-of-contents validation:

```bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/rr-jaggery"
mkdir -p "${BACKUP_DIR}"

# Stream directly to host without pseudo-TTY
docker exec -i rr-postgres pg_dump \
    -U postgres \
    -d rr_jaggery_db \
    -Fc \
    --blobs \
    --verbose > "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump"

# Verify Table of Contents (TOC)
pg_restore -l "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump" > /dev/null
sha256sum "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump" > "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump.sha256"
```

### 3.2 Schema-Only Reference Dump
```bash
docker exec -i rr-postgres pg_dump \
    -U postgres \
    -d rr_jaggery_db \
    --schema-only | gzip -c > "${BACKUP_DIR}/rr_jaggery_db_schema_${TIMESTAMP}.sql.gz"
```

---

## 4. Database & Uploads Restoration Procedure

The primary restore script is located at:
[`infrastructure/scripts/restore.sh`](file:///e:/RR%20Jaggery/infrastructure/scripts/restore.sh)

> [!CAUTION]
> Restoring a backup overwrites the existing database and media files. Never execute a restore against production without an explicit incident ticket or approved disaster-recovery plan.

### 4.1 Production Restoration Syntax
```bash
# Syntax: bash restore.sh <path_to_db_dump> [<path_to_uploads_archive>]
bash infrastructure/scripts/restore.sh \
    /var/backups/rr-jaggery/rr_jaggery_db_20261027_120000.sql.gz \
    /var/backups/rr-jaggery/rr_jaggery_uploads_20261027_120000.tar.gz
```

### 4.2 Automated Safety Steps Executed by `restore.sh`
1. **Pre-flight Integrity:** Verifies `gzip -t` and tests SHA256 against `.sha256` sidecar file before touching any service.
2. **Pre-restore Safety Snapshot:** Automatically generates `pre_restore_safety_snapshot_<timestamp>.sql.gz` from the live database.
3. **Microservice Quiescence:** Stops all five Spring Boot microservices (`auth-service`, `commerce-service`, etc.) to prevent dirty writes.
4. **Clean Stream Restoration:** Streams the uncompressed SQL into PostgreSQL via `docker exec -i` without terminal allocation.
5. **Post-Restore Schema Verification:**
   - Validates presence of all 8 application schemas (`auth_schema`, `commerce_schema`, `customer_schema`, `finance_schema`, `inventory_schema`, `notification_schema`, `procurement_schema`, `production_schema`).
   - Validates total table count (expected: 58 tables).
   - Validates `email_verified` column presence on `auth_schema.users`.
6. **Uploads Volume Restoration:** Extracts files into `uploads_data` volume with clean directory wipe.
7. **Microservice Restart:** Restarts application services and displays health status.

### 4.3 Post-Restore Mandatory Checks
After restoring to a fresh or secondary server:
1. **Re-provision Runtime Roles:** Role passwords may need re-synchronization with `.env`:
   ```bash
   bash infrastructure/docker/postgres/provision_runtime_roles.sh
   ```
2. **Run Parity Check:**
   ```bash
   docker exec -i rr-postgres psql -U postgres -d rr_jaggery_db -f /path/to/verify_schema_parity.sql
   ```

---

## 5. Restoration Drill in a Disposable Container

To prove backup validity without touching production:

```bash
TEST_ID=$(cat /proc/sys/kernel/random/uuid | cut -c1-8)
TEST_CONTAINER="rr-disposable-restore-${TEST_ID}"
RESTORE_FILE="/var/backups/rr-jaggery/rr_jaggery_db_YYYYMMDD_HHMMSS.sql.gz"

# 1. Spin up clean throwaway container
docker run -d \
    --name "${TEST_CONTAINER}" \
    --label "rrdisposable=true" \
    --label "rrtestid=${TEST_ID}" \
    -e POSTGRES_DB=rr_jaggery_db \
    -e POSTGRES_PASSWORD=disposable_test_password \
    postgres:16-alpine

# Wait for startup
until docker exec "${TEST_CONTAINER}" pg_isready -U postgres -d rr_jaggery_db >/dev/null 2>&1; do
    sleep 1
done

# 2. Run restore targeting only the disposable container
TARGET_CONTAINER="${TEST_CONTAINER}" SKIP_SAFETY_SNAPSHOT=true \
    bash infrastructure/scripts/restore.sh "${RESTORE_FILE}"

# 3. Clean up disposable container
docker rm -f "${TEST_CONTAINER}"
```

---

## 6. Off-Host Backup Storage Strategy

Local backups on `/var/backups/rr-jaggery` protect against accidental data corruption or botched migrations, but **do not protect against VPS loss, datacenter fire, or hypervisor outage**.

### 6.1 Off-Host Synchronization Runbook
Configure a nightly cron job to replicate backups to an off-host destination:

#### Option A: S3 / Cloudflare R2 / Backblaze B2 (Recommended)
```bash
# Using AWS CLI / Rclone with private bucket credentials
rclone sync /var/backups/rr-jaggery/ r2:rr-jaggery-backups/ --fast-list
```

#### Option B: Secure Remote SSH Server / Storage Box
```bash
# Nightly rsync over SSH key
rsync -avz -e "ssh -i /root/.ssh/id_backup_vault" \
    /var/backups/rr-jaggery/ backup-vault@storage.domain.internal:/var/vault/rr-jaggery/
```

---

## 7. Troubleshooting & Disaster Recovery FAQs

### Q1: The backup fails with `Insufficient disk space`.
- **Cause:** Free space on `BACKUP_DIR` is less than `MIN_FREE_MB` (default 500 MB).
- **Remedy:** Check disk usage with `df -h /var/backups/rr-jaggery`. Clean up unneeded files or attach additional block storage.

### Q2: Restore fails with `Checksum mismatch`.
- **Cause:** The backup archive was corrupted during transfer or disk degradation occurred.
- **Remedy:** Re-fetch the archive and its `.sha256` sidecar from the off-host backup vault. Do not proceed with corrupted archives.

### Q3: Application fails startup after restore with `permission denied for schema auth_schema`.
- **Cause:** Restoring onto a new PostgreSQL instance reset role passwords or privileges.
- **Remedy:** Execute `bash infrastructure/docker/postgres/provision_runtime_roles.sh` to re-assign passwords from `.env` to the runtime roles.
