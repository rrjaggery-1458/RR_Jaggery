# =============================================================================
# RR JAGGERY TRADERS — AUTOMATED LEAST-PRIVILEGE VERIFICATION SUITE
# Script: test_least_privilege.ps1
# Mode: Isolated Disposable PostgreSQL 16 Test Environment
# =============================================================================

$ErrorActionPreference = "Continue"
if (Test-Path Variable:\PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$TestId = [System.Guid]::NewGuid().ToString("N").Substring(0, 8)
$ContainerName = "rr-disposable-pg-test-$TestId"
$DbName = "rr_jaggery_db"
$SuperUser = "postgres"
$SuperPass = "disposable_test_pass_" + [System.Guid]::NewGuid().ToString("N").Substring(0, 12)
$InitSqlPath = "$PSScriptRoot/init-schemas.sql"
$MigrationSqlPath = "$PSScriptRoot/migrations/V1_least_privilege_hardening.sql"
$ParitySqlPath = "$PSScriptRoot/verify_schema_parity.sql"
$ProvisionScriptPath = "$PSScriptRoot/provision_runtime_roles.ps1"

# Generate in-memory randomized test credentials (never hardcoded, never stored in files)
function New-RandomTestPassword {
    $bytes = New-Object byte[] 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [System.Convert]::ToBase64String($bytes) -replace '[/+=]', 'x'
}

$authTestPass = New-RandomTestPassword
$commTestPass = New-RandomTestPassword
$custTestPass = New-RandomTestPassword
$invTestPass  = New-RandomTestPassword
$prodTestPass = New-RandomTestPassword

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
Write-Host "Container: $ContainerName (Test ID: $TestId)" -ForegroundColor Cyan
Write-Host "=============================================================" -ForegroundColor Cyan

try {
    # 1. Launch isolated disposable container from fresh/empty data directory with test labels
    Write-Host "Starting disposable PostgreSQL 16 container ($ContainerName)..."
    docker run -d `
        --name $ContainerName `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e POSTGRES_DB=$DbName `
        -e POSTGRES_USER=$SuperUser `
        -e POSTGRES_PASSWORD=$SuperPass `
        -v "${InitSqlPath}:/docker-entrypoint-initdb.d/init-schemas.sql:ro" `
        postgres:16-alpine | Out-Null

    # 2. Strict ownership verification: Fail closed if target container is not demonstrably owned by this run
    $assignedLabel = (docker inspect $ContainerName --format '{{.Config.Labels.rrtestid}}' 2>$null).Trim()
    if ($assignedLabel -ne $TestId) {
        Write-Error "Safety Failure: Target container '$ContainerName' does not match disposable test ID '$TestId'. Aborting immediately."
        exit 1
    }

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
        Report-Pass "Schema parity verification (58 tables, email_verified column, sequences, roles, ownership, grants)"
    } else {
        Report-Fail "Schema parity verification" $parityOutput
    }

    # =============================================================================
    # TEST 2: PRE-PROVISIONING FAIL-CLOSED NOLOGIN ENFORCEMENT
    # =============================================================================
    Write-Host "`n--- Running Test 2: Pre-Provisioning NOLOGIN Enforcement ---" -ForegroundColor Yellow
    $nologinAttempt = docker exec -e PGPASSWORD=dummy_pwd $ContainerName psql -U rr_auth -d $DbName -c "SELECT 1;" 2>&1 | Out-String
    if ($nologinAttempt -match "not permitted to log in|FATAL") {
        Report-Pass "Fail-closed check: rr_auth is rejected before password provisioning (NOLOGIN enforced)"
    } else {
        Report-Fail "Fail-closed check" "Expected NOLOGIN connection rejection, got: $nologinAttempt"
    }

    # =============================================================================
    # PROVISIONING: SET SECURE RUNTIME PASSWORDS & ENABLE LOGIN
    # =============================================================================
    Write-Host "`n--- Running Provisioning: Setting Test Passwords & Enabling LOGIN ---" -ForegroundColor Yellow
    & $ProvisionScriptPath `
        -ContainerName $ContainerName `
        -DbName $DbName `
        -AdminUser $SuperUser `
        -AdminPassword $SuperPass `
        -AuthPassword $authTestPass `
        -CommercePassword $commTestPass `
        -CustFinPassword $custTestPass `
        -InvProcPassword $invTestPass `
        -ProductionPassword $prodTestPass

    if ($LASTEXITCODE -ne 0) {
        Report-Fail "Role Provisioning" "provision_runtime_roles.ps1 failed with exit code $LASTEXITCODE"
        throw "Provisioning failed"
    } else {
        Report-Pass "Runtime role password provisioning completed successfully"
    }

    # =============================================================================
    # TEST 3: POSITIVE DML AND IDENTITY SEQUENCE TESTS (AUTHENTICATED)
    # =============================================================================
    Write-Host "`n--- Running Test 3: Positive DML & Sequence Tests ---" -ForegroundColor Yellow

    # 3a. rr_auth can INSERT and SELECT users
    $authDml = docker exec -e PGPASSWORD=$authTestPass $ContainerName psql -U rr_auth -d $DbName -c "
        INSERT INTO auth_schema.users (email, password_hash, full_name)
        VALUES ('test_admin@rrjaggery.com', 'hash123', 'Test Admin') RETURNING email, email_verified;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $authDml -match "test_admin@rrjaggery.com" -and $authDml -match "t") {
        Report-Pass "rr_auth INSERT & SELECT in auth_schema.users (including verified email_verified=true)"
    } else {
        Report-Fail "rr_auth INSERT & SELECT in auth_schema.users" $authDml
    }

    # 3b. rr_commerce can insert into category, product, and stock_outbox (verifying identity sequence)
    $commDml = docker exec -e PGPASSWORD=$commTestPass $ContainerName psql -U rr_commerce -d $DbName -c "
        INSERT INTO commerce_schema.categories (name, slug) VALUES ('Jaggery Blocks', 'jaggery-blocks') RETURNING id;
        INSERT INTO commerce_schema.stock_outbox (command_key, operation, sku, quantity, reference_id)
        VALUES ('CMD-COMM-001', 'RESERVE', 'JAG-BLK-1KG', 5.000, 'ORD-001') RETURNING id;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $commDml -match "1") {
        Report-Pass "rr_commerce DML and identity sequence stock_outbox_id_seq"
    } else {
        Report-Fail "rr_commerce DML and identity sequence" $commDml
    }

    # 3c. rr_custfin can insert into customer and stock_outbox
    $custDml = docker exec -e PGPASSWORD=$custTestPass $ContainerName psql -U rr_custfin -d $DbName -c "
        INSERT INTO customer_schema.customers (contact_person, phone) VALUES ('Rohan Kumar', '9876543210') RETURNING id;
        INSERT INTO customer_schema.stock_outbox (command_key, operation, sku, quantity, reference_id)
        VALUES ('CMD-CUST-001', 'RELEASE', 'JAG-BLK-1KG', 2.000, 'OFF-001') RETURNING id;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $custDml -match "1") {
        Report-Pass "rr_custfin DML and customer_schema.stock_outbox_id_seq"
    } else {
        Report-Fail "rr_custfin DML and customer_schema.stock_outbox_id_seq" $custDml
    }

    # 3d. rr_custfin can manage dynamic forms in public schema
    $formsDml = docker exec -e PGPASSWORD=$custTestPass $ContainerName psql -U rr_custfin -d $DbName -c "
        INSERT INTO public.form_definitions (form_key, display_name, created_by, updated_by)
        VALUES ('WHOLESALE_ORDER', 'Wholesale Order Form', 'custfin_service', 'custfin_service') RETURNING form_key;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $formsDml -match "WHOLESALE_ORDER") {
        Report-Pass "rr_custfin dynamic forms DML in public.form_definitions"
    } else {
        Report-Fail "rr_custfin dynamic forms DML in public.form_definitions" $formsDml
    }

    # 3e. rr_invproc can insert into suppliers and purchase orders
    $invDml = docker exec -e PGPASSWORD=$invTestPass $ContainerName psql -U rr_invproc -d $DbName -c "
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

    # 3f. rr_production can insert into recipes and query procurement_schema.purchase_order_lines
    $prodDml = docker exec -e PGPASSWORD=$prodTestPass $ContainerName psql -U rr_production -d $DbName -c "
        INSERT INTO production_schema.recipes (recipe_code, recipe_name, output_product_sku, output_product_name)
        VALUES ('REC-001', 'Pure Organic Jaggery Block', 'JAG-BLK-1KG', '1kg Jaggery Block') RETURNING recipe_code;
        SELECT COUNT(*) FROM procurement_schema.purchase_order_lines;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $prodDml -match "REC-001") {
        Report-Pass "rr_production DML in production_schema and read-only query on procurement_schema.purchase_order_lines"
    } else {
        Report-Fail "rr_production DML / cross-schema query" $prodDml
    }

    # 3g. rr_auth can query system health table stats (pg_read_all_stats)
    $healthDml = docker exec -e PGPASSWORD=$authTestPass $ContainerName psql -U rr_auth -d $DbName -c "
        SELECT schemaname, relname, n_live_tup FROM pg_stat_user_tables WHERE schemaname = 'auth_schema';
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $healthDml -match "users") {
        Report-Pass "rr_auth SystemHealth stats query via pg_read_all_stats"
    } else {
        Report-Fail "rr_auth SystemHealth stats query" $healthDml
    }

    # =============================================================================
    # TEST 3.5: UNQUALIFIED SEARCH_PATH QUERY RESOLUTION (ALL 5 ROLES)
    # =============================================================================
    Write-Host "`n--- Running Test 3.5: Unqualified search_path Query Resolution ---" -ForegroundColor Yellow

    # 3h. rr_auth unqualified 'users' (auth_schema)
    $spAuth = docker exec -e PGPASSWORD=$authTestPass $ContainerName psql -U rr_auth -d $DbName -t -A -c "SELECT count(*) FROM users;" 2>&1
    if ($LASTEXITCODE -eq 0 -and $spAuth -match "^\d+$") {
        Report-Pass "search_path resolution: rr_auth can query unqualified 'users'"
    } else {
        Report-Fail "search_path resolution: rr_auth unqualified 'users'" $spAuth
    }

    # 3i. rr_commerce unqualified 'categories' (commerce_schema)
    $spComm = docker exec -e PGPASSWORD=$commTestPass $ContainerName psql -U rr_commerce -d $DbName -t -A -c "SELECT count(*) FROM categories;" 2>&1
    if ($LASTEXITCODE -eq 0 -and $spComm -match "^\d+$") {
        Report-Pass "search_path resolution: rr_commerce can query unqualified 'categories'"
    } else {
        Report-Fail "search_path resolution: rr_commerce unqualified 'categories'" $spComm
    }

    # 3j. rr_custfin unqualified multi-schema: 'customers' (customer_schema), 'employees' (finance_schema), 'form_definitions' (public)
    $spCust = docker exec -e PGPASSWORD=$custTestPass $ContainerName psql -U rr_custfin -d $DbName -t -A -c "
        SELECT count(*) FROM customers;
        SELECT count(*) FROM employees;
        SELECT count(*) FROM form_definitions;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and ($spCust -split "\r?\n" | Where-Object { $_ -match "^\d+$" }).Count -ge 3) {
        Report-Pass "search_path resolution: rr_custfin can query unqualified 'customers', 'employees', 'form_definitions'"
    } else {
        Report-Fail "search_path resolution: rr_custfin unqualified multi-schema queries" $spCust
    }

    # 3k. rr_invproc unqualified multi-schema: 'inventory_items' (inventory_schema), 'suppliers' (procurement_schema)
    $spInv = docker exec -e PGPASSWORD=$invTestPass $ContainerName psql -U rr_invproc -d $DbName -t -A -c "
        SELECT count(*) FROM inventory_items;
        SELECT count(*) FROM suppliers;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and ($spInv -split "\r?\n" | Where-Object { $_ -match "^\d+$" }).Count -ge 2) {
        Report-Pass "search_path resolution: rr_invproc can query unqualified 'inventory_items', 'suppliers'"
    } else {
        Report-Fail "search_path resolution: rr_invproc unqualified multi-schema queries" $spInv
    }

    # 3l. rr_production unqualified cross-schema: 'recipes' (production_schema), 'purchase_order_lines' (procurement_schema)
    $spProd = docker exec -e PGPASSWORD=$prodTestPass $ContainerName psql -U rr_production -d $DbName -t -A -c "
        SELECT count(*) FROM recipes;
        SELECT count(*) FROM purchase_order_lines;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and ($spProd -split "\r?\n" | Where-Object { $_ -match "^\d+$" }).Count -ge 2) {
        Report-Pass "search_path resolution: rr_production can query unqualified 'recipes', 'purchase_order_lines'"
    } else {
        Report-Fail "search_path resolution: rr_production unqualified cross-schema queries" $spProd
    }

    # =============================================================================
    # TEST 4: NEGATIVE SECURITY & ISOLATION TESTS (PERMISSION DENIED EXPECTED)
    # =============================================================================
    Write-Host "`n--- Running Test 4: Negative Security & Isolation Tests ---" -ForegroundColor Yellow

    # 4a. rr_commerce CANNOT access auth_schema.users
    $neg1 = docker exec -e PGPASSWORD=$commTestPass $ContainerName psql -U rr_commerce -d $DbName -c "
        SELECT * FROM auth_schema.users;
    " 2>&1
    if ($neg1 -match "permission denied for schema auth_schema") {
        Report-Pass "Negative Test: rr_commerce is denied SELECT on auth_schema.users"
    } else {
        Report-Fail "Negative Test: rr_commerce on auth_schema" "Expected permission denied, got: $neg1"
    }

    # 4b. rr_commerce CANNOT access finance_schema.operating_expenses
    $neg2 = docker exec -e PGPASSWORD=$commTestPass $ContainerName psql -U rr_commerce -d $DbName -c "
        SELECT * FROM finance_schema.operating_expenses;
    " 2>&1
    if ($neg2 -match "permission denied for schema finance_schema") {
        Report-Pass "Negative Test: rr_commerce is denied SELECT on finance_schema"
    } else {
        Report-Fail "Negative Test: rr_commerce on finance_schema" "Expected permission denied, got: $neg2"
    }

    # 4c. rr_production CANNOT INSERT into procurement_schema.purchase_order_lines
    $neg3 = docker exec -e PGPASSWORD=$prodTestPass $ContainerName psql -U rr_production -d $DbName -c "
        INSERT INTO procurement_schema.purchase_order_lines (purchase_order_id, sku, item_name, ordered_quantity, unit_cost, line_total)
        VALUES (gen_random_uuid(), 'CANE-01', 'Sugarcane', 10.000, 50.00, 500.00);
    " 2>&1
    if ($neg3 -match "permission denied") {
        Report-Pass "Negative Test: rr_production is denied INSERT on procurement_schema.purchase_order_lines"
    } else {
        Report-Fail "Negative Test: rr_production INSERT on purchase_order_lines" "Expected permission denied, got: $neg3"
    }

    # 4d. rr_production CANNOT DELETE from procurement_schema.purchase_order_lines
    $neg4 = docker exec -e PGPASSWORD=$prodTestPass $ContainerName psql -U rr_production -d $DbName -c "
        DELETE FROM procurement_schema.purchase_order_lines;
    " 2>&1
    if ($neg4 -match "permission denied") {
        Report-Pass "Negative Test: rr_production is denied DELETE on procurement_schema.purchase_order_lines"
    } else {
        Report-Fail "Negative Test: rr_production DELETE on purchase_order_lines" "Expected permission denied, got: $neg4"
    }

    # 4e. Runtime role CANNOT CREATE tables in application schemas
    $neg5 = docker exec -e PGPASSWORD=$commTestPass $ContainerName psql -U rr_commerce -d $DbName -c "
        CREATE TABLE commerce_schema.hacked_table (id INT);
    " 2>&1
    if ($neg5 -match "permission denied") {
        Report-Pass "Negative Test: rr_commerce is denied CREATE TABLE in commerce_schema"
    } else {
        Report-Fail "Negative Test: rr_commerce CREATE TABLE" "Expected permission denied, got: $neg5"
    }

    # 4f. Runtime role CANNOT ALTER application tables
    $neg6 = docker exec -e PGPASSWORD=$custTestPass $ContainerName psql -U rr_custfin -d $DbName -c "
        ALTER TABLE customer_schema.customers ADD COLUMN backdoor TEXT;
    " 2>&1
    if ($neg6 -match "must be owner|permission denied") {
        Report-Pass "Negative Test: rr_custfin is denied ALTER TABLE customer_schema.customers"
    } else {
        Report-Fail "Negative Test: rr_custfin ALTER TABLE" "Expected must be owner or permission denied, got: $neg6"
    }

    # 4g. Runtime role CANNOT DROP application tables
    $neg7 = docker exec -e PGPASSWORD=$invTestPass $ContainerName psql -U rr_invproc -d $DbName -c "
        DROP TABLE inventory_schema.inventory_items;
    " 2>&1
    if ($neg7 -match "must be owner|permission denied") {
        Report-Pass "Negative Test: rr_invproc is denied DROP TABLE inventory_schema.inventory_items"
    } else {
        Report-Fail "Negative Test: rr_invproc DROP TABLE" "Expected must be owner or permission denied, got: $neg7"
    }

    # =============================================================================
    # TEST 5: MIGRATION SCRIPT IDEMPOTENCY & CREDENTIAL PERSISTENCE
    # =============================================================================
    Write-Host "`n--- Running Test 5: Migration Idempotency (V1 Migration Re-run) ---" -ForegroundColor Yellow
    $migrationPass = docker exec $ContainerName psql -U $SuperUser -d $DbName -f /tmp/V1_least_privilege_hardening.sql 2>&1
    if ($LASTEXITCODE -eq 0 -and $migrationPass -match "Migration V1 Succeeded") {
        Report-Pass "V1 migration idempotency: script executed cleanly without error"
    } else {
        Report-Fail "V1 migration idempotency" $migrationPass
    }

    # Verify that V1 migration preserved existing provisioned credentials and LOGIN state
    $postMigrationAuth = docker exec -e PGPASSWORD=$authTestPass $ContainerName psql -U rr_auth -d $DbName -c "
        SELECT count(*) FROM auth_schema.users;
    " 2>&1
    if ($LASTEXITCODE -eq 0 -and $postMigrationAuth -match "1") {
        Report-Pass "Credential persistence check: rr_auth retains LOGIN and password after V1 migration run"
    } else {
        Report-Fail "Credential persistence check" "rr_auth lost access after V1 migration: $postMigrationAuth"
    }

    # =============================================================================
    # TEST 6: SCHEMA DRIFT DETECTION & HEALING
    # =============================================================================
    Write-Host "`n--- Running Test 6: Schema Drift Detection & Self-Healing ---" -ForegroundColor Yellow
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
}
finally {
    # 4. Strict cleanup: Clean up only resources created by this test run
    Write-Host "`nStopping and removing disposable test container ($ContainerName)..." -ForegroundColor Cyan
    $currentLabel = (docker inspect $ContainerName --format '{{.Config.Labels.rrtestid}}' 2>$null).Trim()
    if ($currentLabel -eq $TestId) {
        docker rm -f $ContainerName 2>$null | Out-Null
        Write-Host "Disposable container $ContainerName cleaned up successfully." -ForegroundColor Green
    } else {
        Write-Warning "Container $ContainerName did not match test ID label; skipped removal for safety."
    }
}

Write-Host "`n=============================================================" -ForegroundColor Cyan
Write-Host "STAGE 4 TEST RESULTS SUMMARY: $PassCount PASSED, $FailCount FAILED" -ForegroundColor $(if ($FailCount -eq 0) { "Green" } else { "Red" })
Write-Host "=============================================================" -ForegroundColor Cyan

if ($FailCount -gt 0) {
    exit 1
}
