# RR Jaggery — PostgreSQL Backup, Verification & Rollback Runbook

**Phase:** 2.2C
**Target Architecture:** Multi-Schema PostgreSQL 16 Least-Privilege Role Structure
**Database Name:** `rr_jaggery_db`
**Security Classification:** Highly Sensitive Operations

---

## 1. Logical Database Backup Procedure

Before executing any database migration or privilege restructuring, a full logical backup must be generated.

### 1.1 Creating Custom Format (`.dump`) and Plain SQL Backups

Execute logical dumps using `pg_dump` with custom compressed archive format (`-Fc`), which allows selective table restoration, parallel restore, and internal checksum verification:

```bash
# Set timestamp and backup file paths
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/postgres"
mkdir -p "${BACKUP_DIR}"

# 1. Custom binary archive format (compressed with metadata and checksums)
docker exec -t rr-postgres pg_dump \
    -U postgres \
    -d rr_jaggery_db \
    -Fc \
    --blobs \
    --verbose \
    -f /tmp/rr_jaggery_db_${TIMESTAMP}.dump

# Copy from container to host filesystem
docker cp rr-postgres:/tmp/rr_jaggery_db_${TIMESTAMP}.dump "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump"
docker exec rr-postgres rm -f /tmp/rr_jaggery_db_${TIMESTAMP}.dump

# 2. Plain SQL schema-only reference dump
docker exec -t rr-postgres pg_dump \
    -U postgres \
    -d rr_jaggery_db \
    --schema-only \
    -f /tmp/rr_jaggery_db_schema_${TIMESTAMP}.sql

docker cp rr-postgres:/tmp/rr_jaggery_db_schema_${TIMESTAMP}.sql "${BACKUP_DIR}/rr_jaggery_db_schema_${TIMESTAMP}.sql"
docker exec rr-postgres rm -f /tmp/rr_jaggery_db_schema_${TIMESTAMP}.sql
```

### 1.2 Archive Integrity Verification

Verify the integrity of the binary dump archive immediately after creation:

```bash
# 1. Verify archive header and table of contents
pg_restore -l "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump" > /dev/null
if [ $? -eq 0 ]; then
    echo "[OK] Backup archive TOC verified successfully."
else
    echo "[ERROR] Backup archive TOC corrupted!"
    exit 1
fi

# 2. Compute cryptographic SHA256 checksum
sha256sum "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump" > "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump.sha256"
cat "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump.sha256"
```

---

## 2. Restoration Test in a Disposable Database

Never assume a backup is valid until it has been successfully restored into an isolated, disposable container.

### 2.1 Disposable Restore Procedure

```bash
TEST_CONTAINER="rr-backup-restore-test"
RESTORE_DUMP="${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump"

# 1. Spin up isolated throwaway PostgreSQL 16 container
docker run -d \
    --name ${TEST_CONTAINER} \
    -e POSTGRES_DB=rr_restore_verify \
    -e POSTGRES_PASSWORD=disposable_verify_pwd \
    postgres:16-alpine

# Wait for startup
until docker exec ${TEST_CONTAINER} pg_isready -U postgres -d rr_restore_verify; do
    sleep 1
done

# 2. Copy backup into throwaway container
docker cp "${RESTORE_DUMP}" ${TEST_CONTAINER}:/tmp/restore.dump

# 3. Perform restoration
docker exec ${TEST_CONTAINER} pg_restore \
    -U postgres \
    -d rr_restore_verify \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    /tmp/restore.dump

# 4. Verify table count in restored database
RESTORED_TABLES=$(docker exec ${TEST_CONTAINER} psql -U postgres -d rr_restore_verify -t -c "
    SELECT count(*) FROM information_schema.tables
    WHERE table_schema IN ('auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
                           'notification_schema', 'inventory_schema', 'procurement_schema', 'production_schema')
       OR (table_schema = 'public' AND table_name LIKE 'form_%');
")
echo "Restored application tables: ${RESTORED_TABLES}"

# 5. Clean up throwaway container
docker rm -f ${TEST_CONTAINER}
```

---

## 3. Off-Host Backup Storage and Verification

Storing backups exclusively on the VPS host or inside Docker volumes creates a single point of failure (risk of disk corruption, hypervisor failure, or accidental deletion).

### 3.1 Off-Host Transfer Protocol

Use encrypted, authenticated transport (`scp` / `rsync` over SSH or S3-compatible cold storage like Cloudflare R2 / AWS S3 with client-side encryption):

```bash
# Transfer archive and checksum to secure off-host storage
SECURE_BACKUP_HOST="backup-storage.rrjaggery.internal"
SECURE_USER="backup-agent"

scp -i /root/.ssh/id_backup "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump" "${SECURE_USER}@${SECURE_BACKUP_HOST}:/secure_vault/postgres/"
scp -i /root/.ssh/id_backup "${BACKUP_DIR}/rr_jaggery_db_${TIMESTAMP}.dump.sha256" "${SECURE_USER}@${SECURE_BACKUP_HOST}:/secure_vault/postgres/"

# Verify SHA256 checksum remotely on the backup target
ssh -i /root/.ssh/id_backup "${SECURE_USER}@${SECURE_BACKUP_HOST}" \
    "cd /secure_vault/postgres && sha256sum -c rr_jaggery_db_${TIMESTAMP}.dump.sha256"
```

---

## 4. Rollback Protocols

In the event of an operational regression, migration failure, or emergency, follow these tiered rollback procedures.

> [!CAUTION]
> Automatic production database drop-and-restore rollbacks are strictly forbidden. Dropping live databases causes irreversible loss of transactions committed during the incident window.

### Tier 1: Reverting Application Credentials (Zero Downtime)

If a service encounters permission issues, you can immediately revert the service to connect using `DB_USER=postgres` without changing table structures:

1. Update `.env` on the VPS:
   ```env
   DB_USER=postgres
   DB_PASSWORD=<postgres_admin_password>
   ```
2. Restart application services:
   ```bash
   docker compose -f compose.production.yaml up -d --no-deps <service-name>
   ```
3. This restores superuser-level access instantly without exposing secrets or altering data.

### Tier 2: Restoring Schema & Table Ownership to `postgres`

If administrative maintenance or third-party tools require `postgres` ownership:

```sql
-- Connect as postgres superuser to rr_jaggery_db
DO $$
DECLARE
    r RECORD;
    s TEXT;
    schemas TEXT[] := ARRAY[
        'auth_schema', 'commerce_schema', 'customer_schema',
        'finance_schema', 'notification_schema', 'inventory_schema',
        'procurement_schema', 'production_schema'
    ];
BEGIN
    -- 1. Restore schema ownership to postgres
    FOREACH s IN ARRAY schemas LOOP
        EXECUTE format('ALTER SCHEMA %I OWNER TO postgres', s);
    END LOOP;

    -- 2. Restore table ownership to postgres
    FOR r IN (
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = ANY(schemas) OR (schemaname = 'public' AND tablename LIKE 'form_%')
    ) LOOP
        EXECUTE format('ALTER TABLE %I.%I OWNER TO postgres', r.schemaname, r.tablename);
    END LOOP;

    -- 3. Restore sequence ownership to postgres
    FOR r IN (
        SELECT n.nspname AS schemaname, c.relname AS seqname
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S' AND n.nspname = ANY(schemas)
    ) LOOP
        EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO postgres', r.schemaname, r.seqname);
    END LOOP;

    RAISE NOTICE 'All application schemas, tables, and sequences restored to postgres ownership.';
END $$;
```

### Tier 3: Reverting Least-Privilege Grants

To revoke least-privilege role access and clean up service roles:

```sql
-- Revoke grants from runtime roles
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth_schema FROM rr_auth;
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA commerce_schema FROM rr_commerce;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA commerce_schema FROM rr_commerce;
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA customer_schema, finance_schema, notification_schema FROM rr_custfin;
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA inventory_schema, procurement_schema FROM rr_invproc;
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA production_schema FROM rr_production;
REVOKE SELECT ON TABLE procurement_schema.purchase_order_lines FROM rr_production;

-- Revoke schema usage
REVOKE USAGE ON SCHEMA auth_schema FROM rr_auth;
REVOKE USAGE ON SCHEMA commerce_schema FROM rr_commerce;
REVOKE USAGE ON SCHEMA customer_schema, finance_schema, notification_schema FROM rr_custfin;
REVOKE USAGE ON SCHEMA inventory_schema, procurement_schema FROM rr_invproc;
REVOKE USAGE ON SCHEMA production_schema, procurement_schema FROM rr_production;
```

---

## 5. Operations That Are Not Automatically Reversible

Documented non-reversible operations:

| Operation | Risk / Irreversibility Nature | Mitigation / Recovery Procedure |
| :--- | :--- | :--- |
| **`DROP TABLE` / `DROP COLUMN`** | Irreversible physical data loss. *Note: Phase 2.2C strictly prohibits destructive DDL.* | Recover only via point-in-time recovery (PITR) or restoring from `.dump` backup. |
| **Identity Sequence Advancements** | Once `nextval()` is called on an identity sequence (e.g. `stock_outbox_id_seq`), gaps are permanent; sequence values cannot be safely rolled back in concurrent transactions. | Normal transactional behavior; no action required unless explicit reset via `setval()` is audited. |
| **Data Modifications during Incident** | New orders or transactions created while running under least privilege cannot be discarded during a schema rollback without losing customer data. | Use Tier 1 credential fallback instead of restoring old database dumps. |
| **Password Rotations** | If role passwords are regenerated without updating application configs, connections fail immediately. | Maintain version-controlled, securely encrypted secret rotation procedures (e.g. Bitwarden / Vault / AWS Secrets Manager). |
