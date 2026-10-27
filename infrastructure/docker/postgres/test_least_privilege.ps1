# =============================================================================
# RR JAGGERY TRADERS — AUTOMATED LEAST-PRIVILEGE VERIFICATION SUITE
# Script: test_least_privilege.ps1
# Mode: Isolated Disposable PostgreSQL 16 Test Environment
# =============================================================================

$ErrorActionPreference = "Continue"
if (Test-Path Variable:\PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}


$ContainerName = "rr-disposable-pg-test"
$DbName = "rr_jaggery_db"
$SuperUser = "postgres"
$SuperPass = "disposable_test_pass"
$InitSqlPath = "$PSScriptRoot/init-schemas.sql"
$MigrationSqlPath = "$PSScriptRoot/migrations/V1_least_privilege_hardening.sql"
$ParitySqlPath = "$PSScriptRoot/verify_schema_parity.sql"

$PassCount = 0
$FailCount = 0

function Report-Pass($testName) {
    Write-Host "[PASS] $testName" -ForegroundColor Green
    $script:PassCount++
}

function Report-Fail($testName, $reason) {
    Write-Host "[FAIL] $testName : $reason" -ForegroundColor Red
    $script:FailCount++
}

Write-Host "=============================================================" -ForegroundColor Cyan
Write-Host "STAGE 4: BUILDING DISPOSABLE POSTGRESQL 16 TEST ENVIRONMENT" -ForegroundColor Cyan
Write-Host "=============================================================" -ForegroundColor Cyan

# 1. Clean up any existing disposable container from prior test runs
docker rm -f $ContainerName 2>$null | Out-Null

# 2. Launch isolated disposable container from fresh/empty data directory
Write-Host "Starting disposable PostgreSQL 16 container ($ContainerName)..."
docker run -d `
    --name $ContainerName `
    -e POSTGRES_DB=$DbName `
    -e POSTGRES_USER=$SuperUser `
    -e POSTGRES_PASSWORD=$SuperPass `
    -v "${InitSqlPath}:/docker-entrypoint-initdb.d/init-schemas.sql:ro" `
    postgres:16-alpine | Out-Null

# 3. Wait for database readiness (wait for entrypoint init script completion and restart)
Write-Host "Waiting for database initialization..."
$maxAttempts = 40
$attempt = 0
$ready = $false

while ($attempt -lt $maxAttempts) {
    $attempt++
    Start-Sleep -Seconds 1
    $logs = docker logs $ContainerName 2>&1 | Out-String
    if ($logs -match "PostgreSQL init process complete; ready for start up") {
        $status = docker exec $ContainerName pg_isready -U $SuperUser -d $DbName 2>$null
        if ($status -match "accepting connections") {
            $ready = $true
            break
        }
    }
}

if (-not $ready) {
    Write-Error "Container failed to complete initialization within timeout."
    exit 1
}

Write-Host "Disposable PostgreSQL container ready and initialized." -ForegroundColor Green


# Copy verification and migration scripts into container
docker cp $MigrationSqlPath "${ContainerName}:/tmp/V1_least_privilege_hardening.sql"
docker cp $ParitySqlPath "${ContainerName}:/tmp/verify_schema_parity.sql"

# =============================================================================
# TEST 1: SCHEMA PARITY CHECK
# =============================================================================
Write-Host "`n--- Running Test 1: Schema Parity Audit ---" -ForegroundColor Yellow
$parityOutput = docker exec $ContainerName psql -U $SuperUser -d $DbName -f /tmp/verify_schema_parity.sql 2>&1
if ($LASTEXITCODE -eq 0 -and $parityOutput -match "ALL CHECKS PASSED") {
    Report-Pass "Schema parity verification (58 tables, sequences, roles, ownership, grants)"
} else {
    Report-Fail "Schema parity verification" $parityOutput
}

# =============================================================================
# TEST 2: POSITIVE DML AND IDENTITY SEQUENCE TESTS
# =============================================================================
Write-Host "`n--- Running Test 2: Positive DML & Sequence Tests ---" -ForegroundColor Yellow

# 2a. rr_auth can INSERT and SELECT users
$authDml = docker exec -e PGPASSWORD=rr_auth_dev_password $ContainerName psql -U rr_auth -d $DbName -c "
    INSERT INTO auth_schema.users (email, password_hash, full_name)
    VALUES ('test_admin@rrjaggery.com', 'hash123', 'Test Admin') RETURNING email;
" 2>&1
if ($LASTEXITCODE -eq 0 -and $authDml -match "test_admin@rrjaggery.com") {
    Report-Pass "rr_auth INSERT & SELECT in auth_schema.users"
} else {
    Report-Fail "rr_auth INSERT & SELECT in auth_schema.users" $authDml
}

# 2b. rr_commerce can insert into category, product, and stock_outbox (verifying identity sequence)
$commDml = docker exec -e PGPASSWORD=rr_commerce_dev_password $ContainerName psql -U rr_commerce -d $DbName -c "
    INSERT INTO commerce_schema.categories (name, slug) VALUES ('Jaggery Blocks', 'jaggery-blocks') RETURNING id;
    INSERT INTO commerce_schema.stock_outbox (command_key, operation, sku, quantity, reference_id)
    VALUES ('CMD-COMM-001', 'RESERVE', 'JAG-BLK-1KG', 5.000, 'ORD-001') RETURNING id;
" 2>&1
if ($LASTEXITCODE -eq 0 -and $commDml -match "1") {
    Report-Pass "rr_commerce DML and identity sequence stock_outbox_id_seq"
} else {
    Report-Fail "rr_commerce DML and identity sequence" $commDml
}

# 2c. rr_custfin can insert into customer and stock_outbox
$custDml = docker exec -e PGPASSWORD=rr_custfin_dev_password $ContainerName psql -U rr_custfin -d $DbName -c "
    INSERT INTO customer_schema.customers (contact_person, phone) VALUES ('Rohan Kumar', '9876543210') RETURNING id;
    INSERT INTO customer_schema.stock_outbox (command_key, operation, sku, quantity, reference_id)
    VALUES ('CMD-CUST-001', 'RELEASE', 'JAG-BLK-1KG', 2.000, 'OFF-001') RETURNING id;
" 2>&1
if ($LASTEXITCODE -eq 0 -and $custDml -match "1") {
    Report-Pass "rr_custfin DML and customer_schema.stock_outbox_id_seq"
} else {
    Report-Fail "rr_custfin DML and customer_schema.stock_outbox_id_seq" $custDml
}

# 2d. rr_custfin can manage dynamic forms in public schema
$formsDml = docker exec -e PGPASSWORD=rr_custfin_dev_password $ContainerName psql -U rr_custfin -d $DbName -c "
    INSERT INTO public.form_definitions (form_key, display_name, created_by, updated_by)
    VALUES ('WHOLESALE_ORDER', 'Wholesale Order Form', 'custfin_service', 'custfin_service') RETURNING form_key;
" 2>&1
if ($LASTEXITCODE -eq 0 -and $formsDml -match "WHOLESALE_ORDER") {
    Report-Pass "rr_custfin dynamic forms DML in public.form_definitions"
} else {
    Report-Fail "rr_custfin dynamic forms DML in public.form_definitions" $formsDml
}

# 2e. rr_invproc can insert into suppliers and purchase orders
$invDml = docker exec -e PGPASSWORD=rr_invproc_dev_password $ContainerName psql -U rr_invproc -d $DbName -c "
    WITH s AS (
        INSERT INTO procurement_schema.suppliers (supplier_code, supplier_name)
        VALUES ('SUP-001', 'Cane Farmer Coop') RETURNING id
    )
    INSERT INTO procurement_schema.purchase_orders (order_number, supplier_id)
    SELECT 'PO-2026-001', id FROM s RETURNING order_number;
" 2>&1
if ($LASTEXITCODE -eq 0 -and $invDml -match "PO-2026-001") {
    Report-Pass "rr_invproc DML in procurement_schema"
} else {
    Report-Fail "rr_invproc DML in procurement_schema" $invDml
}

# 2f. rr_production can insert into recipes and query procurement_schema.purchase_order_lines
$prodDml = docker exec -e PGPASSWORD=rr_production_dev_password $ContainerName psql -U rr_production -d $DbName -c "
    INSERT INTO production_schema.recipes (recipe_code, recipe_name, output_product_sku, output_product_name)
    VALUES ('REC-001', 'Pure Organic Jaggery Block', 'JAG-BLK-1KG', '1kg Jaggery Block') RETURNING recipe_code;
    SELECT COUNT(*) FROM procurement_schema.purchase_order_lines;
" 2>&1
if ($LASTEXITCODE -eq 0 -and $prodDml -match "REC-001") {
    Report-Pass "rr_production DML in production_schema and read-only query on procurement_schema.purchase_order_lines"
} else {
    Report-Fail "rr_production DML / cross-schema query" $prodDml
}

# 2g. rr_auth can query system health table stats (pg_read_all_stats)
$healthDml = docker exec -e PGPASSWORD=rr_auth_dev_password $ContainerName psql -U rr_auth -d $DbName -c "
    SELECT schemaname, relname, n_live_tup FROM pg_stat_user_tables WHERE schemaname = 'auth_schema';
" 2>&1
if ($LASTEXITCODE -eq 0 -and $healthDml -match "users") {
    Report-Pass "rr_auth SystemHealth stats query via pg_read_all_stats"
} else {
    Report-Fail "rr_auth SystemHealth stats query" $healthDml
}

# =============================================================================
# TEST 3: NEGATIVE SECURITY & ISOLATION TESTS (PERMISSION DENIED EXPECTED)
# =============================================================================
Write-Host "`n--- Running Test 3: Negative Security & Isolation Tests ---" -ForegroundColor Yellow

# 3a. rr_commerce CANNOT access auth_schema.users
$neg1 = docker exec -e PGPASSWORD=rr_commerce_dev_password $ContainerName psql -U rr_commerce -d $DbName -c "
    SELECT * FROM auth_schema.users;
" 2>&1
if ($neg1 -match "permission denied for schema auth_schema") {
    Report-Pass "Negative Test: rr_commerce is denied SELECT on auth_schema.users"
} else {
    Report-Fail "Negative Test: rr_commerce on auth_schema" "Expected permission denied, got: $neg1"
}

# 3b. rr_commerce CANNOT access finance_schema.operating_expenses
$neg2 = docker exec -e PGPASSWORD=rr_commerce_dev_password $ContainerName psql -U rr_commerce -d $DbName -c "
    SELECT * FROM finance_schema.operating_expenses;
" 2>&1
if ($neg2 -match "permission denied for schema finance_schema") {
    Report-Pass "Negative Test: rr_commerce is denied SELECT on finance_schema"
} else {
    Report-Fail "Negative Test: rr_commerce on finance_schema" "Expected permission denied, got: $neg2"
}

# 3c. rr_production CANNOT INSERT into procurement_schema.purchase_order_lines
$neg3 = docker exec -e PGPASSWORD=rr_production_dev_password $ContainerName psql -U rr_production -d $DbName -c "
    INSERT INTO procurement_schema.purchase_order_lines (purchase_order_id, sku, item_name, ordered_quantity, unit_cost, line_total)
    VALUES (gen_random_uuid(), 'CANE-01', 'Sugarcane', 10.000, 50.00, 500.00);
" 2>&1
if ($neg3 -match "permission denied") {
    Report-Pass "Negative Test: rr_production is denied INSERT on procurement_schema.purchase_order_lines"
} else {
    Report-Fail "Negative Test: rr_production INSERT on purchase_order_lines" "Expected permission denied, got: $neg3"
}

# 3d. rr_production CANNOT DELETE from procurement_schema.purchase_order_lines
$neg4 = docker exec -e PGPASSWORD=rr_production_dev_password $ContainerName psql -U rr_production -d $DbName -c "
    DELETE FROM procurement_schema.purchase_order_lines;
" 2>&1
if ($neg4 -match "permission denied") {
    Report-Pass "Negative Test: rr_production is denied DELETE on procurement_schema.purchase_order_lines"
} else {
    Report-Fail "Negative Test: rr_production DELETE on purchase_order_lines" "Expected permission denied, got: $neg4"
}

# 3e. Runtime role CANNOT CREATE tables in application schemas
$neg5 = docker exec -e PGPASSWORD=rr_commerce_dev_password $ContainerName psql -U rr_commerce -d $DbName -c "
    CREATE TABLE commerce_schema.hacked_table (id INT);
" 2>&1
if ($neg5 -match "permission denied") {
    Report-Pass "Negative Test: rr_commerce is denied CREATE TABLE in commerce_schema"
} else {
    Report-Fail "Negative Test: rr_commerce CREATE TABLE" "Expected permission denied, got: $neg5"
}

# 3f. Runtime role CANNOT ALTER application tables
$neg6 = docker exec -e PGPASSWORD=rr_custfin_dev_password $ContainerName psql -U rr_custfin -d $DbName -c "
    ALTER TABLE customer_schema.customers ADD COLUMN backdoor TEXT;
" 2>&1
if ($neg6 -match "must be owner|permission denied") {
    Report-Pass "Negative Test: rr_custfin is denied ALTER TABLE customer_schema.customers"
} else {
    Report-Fail "Negative Test: rr_custfin ALTER TABLE" "Expected must be owner or permission denied, got: $neg6"
}

# 3g. Runtime role CANNOT DROP application tables
$neg7 = docker exec -e PGPASSWORD=rr_invproc_dev_password $ContainerName psql -U rr_invproc -d $DbName -c "
    DROP TABLE inventory_schema.inventory_items;
" 2>&1
if ($neg7 -match "must be owner|permission denied") {
    Report-Pass "Negative Test: rr_invproc is denied DROP TABLE inventory_schema.inventory_items"
} else {
    Report-Fail "Negative Test: rr_invproc DROP TABLE" "Expected must be owner or permission denied, got: $neg7"
}


# =============================================================================
# TEST 4: MIGRATION SCRIPT IDEMPOTENCY (RUNNING V1 SCRIPT A SECOND TIME)
# =============================================================================
Write-Host "`n--- Running Test 4: Migration Idempotency (V1 Second Pass) ---" -ForegroundColor Yellow
$migrationPass2 = docker exec $ContainerName psql -U $SuperUser -d $DbName -f /tmp/V1_least_privilege_hardening.sql 2>&1
if ($LASTEXITCODE -eq 0 -and $migrationPass2 -match "Migration V1 Succeeded") {
    Report-Pass "V1 migration idempotency: second run succeeded without error or schema drift"
} else {
    Report-Fail "V1 migration idempotency" $migrationPass2
}

# =============================================================================
# TEST 5: SCHEMA DRIFT DETECTION
# =============================================================================
Write-Host "`n--- Running Test 5: Schema Drift Detection ---" -ForegroundColor Yellow
# Introduce deliberate drift: change table ownership away from rr_migrator to postgres on one table
docker exec $ContainerName psql -U $SuperUser -d $DbName -c "ALTER TABLE auth_schema.users OWNER TO postgres;" 2>$null | Out-Null
$driftOutput = docker exec $ContainerName psql -U $SuperUser -d $DbName -f /tmp/verify_schema_parity.sql 2>&1
if ($driftOutput -match "Parity Failure: 1 tables are not owned by rr_migrator") {
    Report-Pass "Deliberately introduced schema drift is correctly detected and rejected"
} else {
    Report-Fail "Schema drift detection" "Expected rejection of unowned table, got: $driftOutput"
}

# Revert drift using V1 migration (proving self-healing / idempotency)
$healOutput = docker exec $ContainerName psql -U $SuperUser -d $DbName -f /tmp/V1_least_privilege_hardening.sql 2>&1
if ($LASTEXITCODE -eq 0 -and $healOutput -match "Migration V1 Succeeded") {
    Report-Pass "V1 migration successfully heals schema drift and restores rr_migrator ownership"
} else {
    Report-Fail "V1 migration healing" $healOutput
}

# Clean up disposable container
Write-Host "`nStopping and removing disposable test container..." -ForegroundColor Cyan
docker rm -f $ContainerName 2>$null | Out-Null

Write-Host "`n=============================================================" -ForegroundColor Cyan
Write-Host "STAGE 4 TEST RESULTS SUMMARY: $PassCount PASSED, $FailCount FAILED" -ForegroundColor $(if ($FailCount -eq 0) { "Green" } else { "Red" })
Write-Host "=============================================================" -ForegroundColor Cyan

if ($FailCount -gt 0) {
    exit 1
}
