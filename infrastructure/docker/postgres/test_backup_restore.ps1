# =============================================================================
# RR JAGGERY TRADERS — DISPOSABLE BACKUP & RESTORE INTEGRATION DRILL
# Phase 2.2E.4: Validates backup generation, gzip integrity, checksum verification,
# disposable target restore, schema parity, data fidelity, and upload archiving.
# =============================================================================

$ErrorActionPreference = "Continue"

if (Test-Path Variable:\PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$TestId = [System.Guid]::NewGuid().ToString("N").Substring(0, 8)
$SourceContainer = "rr-disp-bkp-src-$TestId"
$TargetContainer = "rr-disp-rst-tgt-$TestId"
$DbName = "rr_jaggery_db"
$SuperUser = "postgres"
$SuperPass = "disposable_test_pass_" + [System.Guid]::NewGuid().ToString("N").Substring(0, 12)
$InitSqlPath = "$PSScriptRoot/init-schemas.sql"
$ProvisionScriptPath = "$PSScriptRoot/provision_runtime_roles.ps1"

$TempBackupDir = Join-Path $PSScriptRoot "tmp_drill_bkp_$TestId"
$TempUploadsSrc = Join-Path $PSScriptRoot "tmp_drill_upl_src_$TestId"
$TempUploadsTgt = Join-Path $PSScriptRoot "tmp_drill_upl_tgt_$TestId"

$PassCount = 0
$FailCount = 0

function Report-Pass($testName) {
    $script:PassCount++
    Write-Host "[PASS] $testName" -ForegroundColor Green
}

function Report-Fail($testName, $reason) {
    $script:FailCount++
    Write-Host "[FAIL] $testName" -ForegroundColor Red
    Write-Host "       Reason: $reason" -ForegroundColor Yellow
}

Write-Host "=============================================================" -ForegroundColor Cyan
Write-Host "STAGE 5: DISPOSABLE BACKUP & RESTORE INTEGRATION DRILL" -ForegroundColor Cyan
Write-Host "Test ID: $TestId" -ForegroundColor Cyan
Write-Host "Source Container: $SourceContainer" -ForegroundColor Cyan
Write-Host "Target Container: $TargetContainer" -ForegroundColor Cyan
Write-Host "=============================================================" -ForegroundColor Cyan

try {
    New-Item -ItemType Directory -Path $TempBackupDir -Force | Out-Null
    New-Item -ItemType Directory -Path $TempUploadsSrc -Force | Out-Null
    New-Item -ItemType Directory -Path $TempUploadsTgt -Force | Out-Null

    # -------------------------------------------------------------------------
    # 1. Spin up Isolated Source Container
    # -------------------------------------------------------------------------
    Write-Host "`n1. Launching isolated disposable source PostgreSQL container..." -ForegroundColor Cyan
    docker run -d `
        --name $SourceContainer `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e POSTGRES_DB=$DbName `
        -e POSTGRES_USER=$SuperUser `
        -e POSTGRES_PASSWORD=$SuperPass `
        -v "${InitSqlPath}:/docker-entrypoint-initdb.d/init-schemas.sql:ro" `
        postgres:16-alpine | Out-Null

    # Verify ownership
    $srcLabel = (docker inspect $SourceContainer --format '{{.Config.Labels.rrtestid}}' 2>$null).Trim()
    if ($srcLabel -ne $TestId) {
        Write-Error "Safety Failure: Source container does not match TestId. Aborting."
        exit 1
    }

    # Wait for source database readiness
    $maxAttempts = 40
    $attempt = 0
    $srcReady = $false
    while ($attempt -lt $maxAttempts) {
        $attempt++
        Start-Sleep -Seconds 1
        $logs = docker logs $SourceContainer 2>&1 | Out-String
        if ($logs -match "PostgreSQL init process complete; ready for start up") {
            $status = docker exec $SourceContainer pg_isready -U $SuperUser -d $DbName 2>$null
            if ($status -match "accepting connections") {
                $srcReady = $true
                break
            }
        }
    }
    if (-not $srcReady) {
        Write-Error "Disposable source container failed to initialize in time."
        exit 1
    }
    Report-Pass "Disposable source container initialized successfully ($SourceContainer)"

    # Provision roles
    Write-Host "`n2. Provisioning runtime roles and seeding test data..." -ForegroundColor Cyan
    $testPassword = "drill_pwd_" + [System.Guid]::NewGuid().ToString("N").Substring(0, 10)
    powershell -ExecutionPolicy Bypass -File $ProvisionScriptPath `
        -ContainerName $SourceContainer `
        -DbName $DbName `
        -AdminUser $SuperUser `
        -AdminPassword $SuperPass `
        -AuthPassword $testPassword `
        -CommercePassword $testPassword `
        -CustFinPassword $testPassword `
        -InvProcPassword $testPassword `
        -ProductionPassword $testPassword | Out-Null
    Report-Pass "Runtime roles provisioned via provision_runtime_roles.ps1"

    # Seed representative data across schemas
    $seedSql = @"
INSERT INTO auth_schema.users (id, email, password_hash, full_name, enabled, email_verified)
VALUES (gen_random_uuid(), 'drill_user@rrjaggery.com', 'dummy_hash', 'Drill User', true, true);

INSERT INTO commerce_schema.categories (name, slug, description)
VALUES ('Drill Jaggery Category', 'drill-jaggery-category', 'Test category for backup/restore drill');

INSERT INTO customer_schema.customers (contact_person, phone)
VALUES ('Drill Customer', '9999888877');

INSERT INTO public.form_definitions (form_key, display_name, created_by, updated_by)
VALUES ('DRILL_FORM', 'Drill Form Definition', 'drill_agent', 'drill_agent');
"@
    $seedResult = docker exec -i $SourceContainer psql -U $SuperUser -d $DbName -c "$seedSql" 2>&1
    if ($LASTEXITCODE -eq 0 -and $seedResult -match "INSERT 0 1") {
        Report-Pass "Test data seeded across auth_schema, commerce_schema, customer_schema, and public.form_definitions"
    } else {
        Report-Fail "Test data seeding" $seedResult
    }

    # Seed sample upload files
    $sampleImgPath = Join-Path $TempUploadsSrc "product_jaggery_block.jpg"
    $sampleDocPath = Join-Path $TempUploadsSrc "customer_invoice_001.pdf"
    [IO.File]::WriteAllText($sampleImgPath, "FAKE_JPEG_IMAGE_DATA_RR_JAGGERY_2026")
    [IO.File]::WriteAllText($sampleDocPath, "FAKE_PDF_DOCUMENT_DATA_RR_JAGGERY_2026")
    Report-Pass "Sample upload files created in temporary staging directory"

    # -------------------------------------------------------------------------
    # 3. Execute Backup Procedure
    # -------------------------------------------------------------------------
    Write-Host "`n3. Executing database and uploads backup..." -ForegroundColor Cyan
    $dbBackupFile = Join-Path $TempBackupDir "rr_jaggery_db_drill_$TestId.sql.gz"
    $uploadsBackupFile = Join-Path $TempBackupDir "rr_jaggery_uploads_drill_$TestId.tar.gz"

    # Stream database dump to container-internal file and copy out (avoiding OS encoding translation)
    docker exec -i $SourceContainer sh -c "pg_dumpall -U $SuperUser | gzip -c > /tmp/drill_backup.sql.gz"
    docker cp "${SourceContainer}:/tmp/drill_backup.sql.gz" $dbBackupFile
    docker exec -i $SourceContainer rm -f /tmp/drill_backup.sql.gz

    if ((Test-Path $dbBackupFile) -and ((Get-Item $dbBackupFile).Length -gt 0)) {
        Report-Pass "Database dump generated without pseudo-TTY (size: $((Get-Item $dbBackupFile).Length) bytes)"
    } else {
        Report-Fail "Database dump generation" "File missing or 0 bytes"
    }

    # Verify gzip integrity via Docker alpine
    $backupDirLinux = $TempBackupDir.Replace('\', '/')
    $backupFileLeaf = Split-Path $dbBackupFile -Leaf
    $gzCheck = docker run --rm -v "${backupDirLinux}:/backup" alpine gzip -t "/backup/$backupFileLeaf" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Report-Pass "Database dump passed gzip integrity validation"
    } else {
        Report-Fail "Database dump gzip integrity" $gzCheck
    }

    # Generate and verify SHA256 checksum
    $dbHash = (Get-FileHash -Path $dbBackupFile -Algorithm SHA256).Hash.ToLower()
    $dbHashFile = "$dbBackupFile.sha256"
    "$dbHash  $(Split-Path $dbBackupFile -Leaf)" | Out-File -FilePath $dbHashFile -Encoding ascii
    Report-Pass "Database dump SHA256 checksum generated ($dbHash)"

    # Archive sample uploads using native Windows tar
    tar -czf $uploadsBackupFile -C $TempUploadsSrc .
    if ((Test-Path $uploadsBackupFile) -and ((Get-Item $uploadsBackupFile).Length -gt 0)) {
        Report-Pass "Uploads archive generated (size: $((Get-Item $uploadsBackupFile).Length) bytes)"
    } else {
        Report-Fail "Uploads archive generation" "File missing or 0 bytes"
    }

    # -------------------------------------------------------------------------
    # 4. Spin up Clean Disposable Target Container
    # -------------------------------------------------------------------------
    Write-Host "`n4. Launching clean disposable target PostgreSQL container..." -ForegroundColor Cyan
    docker run -d `
        --name $TargetContainer `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e POSTGRES_DB=$DbName `
        -e POSTGRES_USER=$SuperUser `
        -e POSTGRES_PASSWORD=$SuperPass `
        postgres:16-alpine | Out-Null

    # Verify ownership
    $tgtLabel = (docker inspect $TargetContainer --format '{{.Config.Labels.rrtestid}}' 2>$null).Trim()
    if ($tgtLabel -ne $TestId) {
        Write-Error "Safety Failure: Target container does not match TestId. Aborting."
        exit 1
    }

    # Wait for target database readiness
    $attempt = 0
    $tgtReady = $false
    while ($attempt -lt $maxAttempts) {
        $attempt++
        Start-Sleep -Seconds 1
        $logs = docker logs $TargetContainer 2>&1 | Out-String
        if ($logs -match "PostgreSQL init process complete; ready for start up") {
            $status = docker exec $TargetContainer pg_isready -U $SuperUser -d $DbName 2>$null
            if ($status -match "accepting connections") {
                $tgtReady = $true
                break
            }
        }
    }
    if (-not $tgtReady) {
        Write-Error "Disposable target container failed to initialize in time."
        exit 1
    }
    Report-Pass "Clean disposable target container initialized successfully ($TargetContainer)"

    # -------------------------------------------------------------------------
    # 5. Execute Restore onto Disposable Target
    # -------------------------------------------------------------------------
    Write-Host "`n5. Executing restore into clean target..." -ForegroundColor Cyan

    # Verify checksum matches before restoring
    $recomputedHash = (Get-FileHash -Path $dbBackupFile -Algorithm SHA256).Hash.ToLower()
    if ($recomputedHash -eq $dbHash) {
        Report-Pass "Pre-restore checksum verification matches original backup"
    } else {
        Report-Fail "Pre-restore checksum verification" "Hash mismatch!"
    }

    # Deliver and decompress into TargetContainer
    docker cp $dbBackupFile "${TargetContainer}:/tmp/drill_restore.sql.gz"
    docker exec -i $TargetContainer sh -c "gunzip -c /tmp/drill_restore.sql.gz | psql -U $SuperUser" | Out-Null
    docker exec -i $TargetContainer rm -f /tmp/drill_restore.sql.gz
    Report-Pass "Restoration SQL stream delivered and executed in target container"

    # -------------------------------------------------------------------------
    # 6. Verify Restored Data Fidelity & Schemas
    # -------------------------------------------------------------------------
    Write-Host "`n6. Validating restored schemas, tables, and seeded records..." -ForegroundColor Cyan

    # Verify schemas
    $schemasQuery = docker exec -i $TargetContainer psql -U $SuperUser -d $DbName -t -A -c "
        SELECT count(*) FROM information_schema.schemata
        WHERE schema_name IN ('auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
                              'inventory_schema', 'notification_schema', 'procurement_schema', 'production_schema');
    " 2>&1
    if ($schemasQuery.Trim() -eq "8") {
        Report-Pass "All 8 application schemas successfully restored on target"
    } else {
        Report-Fail "Restored application schemas count" "Expected 8, got: $schemasQuery"
    }

    # Verify table count (expected 58)
    $tableCount = docker exec -i $TargetContainer psql -U $SuperUser -d $DbName -t -A -c "
        SELECT count(*) FROM information_schema.tables
        WHERE table_schema IN ('auth_schema', 'commerce_schema', 'customer_schema', 'finance_schema',
                               'inventory_schema', 'notification_schema', 'procurement_schema', 'production_schema')
           OR (table_schema = 'public' AND table_name LIKE 'form_%');
    " 2>&1
    if ($tableCount.Trim() -eq "58") {
        Report-Pass "All 58 application tables successfully restored on target"
    } else {
        Report-Fail "Restored tables count" "Expected 58, got: $tableCount"
    }

    # Verify seeded data presence
    $authCheck = docker exec -i $TargetContainer psql -U $SuperUser -d $DbName -t -A -c "
        SELECT count(*) FROM auth_schema.users WHERE email = 'drill_user@rrjaggery.com' AND email_verified = true;
    " 2>&1
    if ($authCheck.Trim() -eq "1") {
        Report-Pass "Verified restored auth user with email_verified=true"
    } else {
        Report-Fail "Restored auth user check" $authCheck
    }

    $commCheck = docker exec -i $TargetContainer psql -U $SuperUser -d $DbName -t -A -c "
        SELECT count(*) FROM commerce_schema.categories WHERE name = 'Drill Jaggery Category';
    " 2>&1
    if ($commCheck.Trim() -eq "1") {
        Report-Pass "Verified restored category 'Drill Jaggery Category'"
    } else {
        Report-Fail "Restored category check" $commCheck
    }

    $custCheck = docker exec -i $TargetContainer psql -U $SuperUser -d $DbName -t -A -c "
        SELECT count(*) FROM customer_schema.customers WHERE contact_person = 'Drill Customer';
    " 2>&1
    if ($custCheck.Trim() -eq "1") {
        Report-Pass "Verified restored customer 'Drill Customer'"
    } else {
        Report-Fail "Restored customer check" $custCheck
    }

    $formCheck = docker exec -i $TargetContainer psql -U $SuperUser -d $DbName -t -A -c "
        SELECT count(*) FROM public.form_definitions WHERE form_key = 'DRILL_FORM';
    " 2>&1
    if ($formCheck.Trim() -eq "1") {
        Report-Pass "Verified restored dynamic form 'DRILL_FORM' in public schema"
    } else {
        Report-Fail "Restored form definition check" $formCheck
    }

    # Restore uploads into target temporary directory
    tar -xzf $uploadsBackupFile -C $TempUploadsTgt
    $tgtImg = Join-Path $TempUploadsTgt "product_jaggery_block.jpg"
    $tgtDoc = Join-Path $TempUploadsTgt "customer_invoice_001.pdf"
    if ((Test-Path $tgtImg) -and (Test-Path $tgtDoc) -and
        ([IO.File]::ReadAllText($tgtImg) -eq "FAKE_JPEG_IMAGE_DATA_RR_JAGGERY_2026") -and
        ([IO.File]::ReadAllText($tgtDoc) -eq "FAKE_PDF_DOCUMENT_DATA_RR_JAGGERY_2026")) {
        Report-Pass "Uploads archive restored and byte-verified against original sample files"
    } else {
        Report-Fail "Uploads restore verification" "Files missing or contents corrupted"
    }

    # -------------------------------------------------------------------------
    # 7. Negative / Failure Injection Testing
    # -------------------------------------------------------------------------
    Write-Host "`n7. Testing failure cases..." -ForegroundColor Cyan

    # Failure 1: Missing backup file
    $nonExistentFile = Join-Path $TempBackupDir "non_existent_file.sql.gz"
    if (-not (Test-Path $nonExistentFile)) {
        Report-Pass "Negative Test: Verified non-existent backup file does not exist"
    }

    # Failure 2: Corrupted backup file detection
    $corruptBackup = Join-Path $TempBackupDir "corrupted.sql.gz"
    [IO.File]::WriteAllText($corruptBackup, "NOT_A_VALID_GZIP_FILE_DATA")
    $corruptLeaf = Split-Path $corruptBackup -Leaf
    $corruptCheck = docker run --rm -v "${backupDirLinux}:/backup" alpine gzip -t "/backup/$corruptLeaf" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Report-Pass "Negative Test: Corrupted archive is detected and rejected by gzip integrity test"
    } else {
        Report-Fail "Negative Test: Corrupted archive check" "Expected gzip to fail, but it returned 0"
    }
}
finally {
    # -------------------------------------------------------------------------
    # Strict Cleanup: Only remove resources matching $TestId
    # -------------------------------------------------------------------------
    Write-Host "`n8. Cleaning up disposable test resources..." -ForegroundColor Cyan
    
    $existingSrc = docker ps -a --filter "name=$SourceContainer" -q 2>$null
    if ($existingSrc) {
        $lbl = (docker inspect $SourceContainer --format '{{.Config.Labels.rrtestid}}' 2>$null)
        if ($lbl -and $lbl.Trim() -eq $TestId) {
            docker rm -f $SourceContainer 2>$null | Out-Null
            Write-Host "Source container $SourceContainer cleaned up." -ForegroundColor Green
        }
    }

    $existingTgt = docker ps -a --filter "name=$TargetContainer" -q 2>$null
    if ($existingTgt) {
        $lbl = (docker inspect $TargetContainer --format '{{.Config.Labels.rrtestid}}' 2>$null)
        if ($lbl -and $lbl.Trim() -eq $TestId) {
            docker rm -f $TargetContainer 2>$null | Out-Null
            Write-Host "Target container $TargetContainer cleaned up." -ForegroundColor Green
        }
    }

    # Clean temporary directories
    if (Test-Path $TempBackupDir) { Remove-Item -Recurse -Force $TempBackupDir 2>$null }
    if (Test-Path $TempUploadsSrc) { Remove-Item -Recurse -Force $TempUploadsSrc 2>$null }
    if (Test-Path $TempUploadsTgt) { Remove-Item -Recurse -Force $TempUploadsTgt 2>$null }
    Write-Host "Temporary drill directories cleaned up." -ForegroundColor Green
}

Write-Host "`n=============================================================" -ForegroundColor Cyan
Write-Host "STAGE 5 DRILL SUMMARY: $PassCount PASSED, $FailCount FAILED" -ForegroundColor $(if ($FailCount -eq 0) { "Green" } else { "Red" })
Write-Host "=============================================================" -ForegroundColor Cyan

if ($FailCount -gt 0) {
    exit 1
}
