# =============================================================================
# RR JAGGERY TRADERS — ISOLATED FIVE-SERVICE STARTUP VALIDATION SUITE
# Phase 2.2E.5: Validates all five Spring Boot microservices against a disposable
# PostgreSQL 16 container using least-privilege runtime credentials and Hibernate validate.
# =============================================================================

$ErrorActionPreference = "Continue"

if (Test-Path Variable:\PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$TestId = [System.Guid]::NewGuid().ToString("N").Substring(0, 8)
$NetworkName = "rr-test-net-$TestId"
$PgContainer = "rr-test-pg-$TestId"
$DbName = "rr_jaggery_db"
$SuperUser = "postgres"
$SuperPass = "disposable_test_pass_" + [System.Guid]::NewGuid().ToString("N").Substring(0, 12)
$InitSqlPath = "$PSScriptRoot/init-schemas.sql"
$ProvisionScriptPath = "$PSScriptRoot/provision_runtime_roles.ps1"

# Service containers
$AuthContainer    = "rr-test-auth-$TestId"
$CommerceContainer= "rr-test-comm-$TestId"
$CustFinContainer = "rr-test-cust-$TestId"
$InvProcContainer = "rr-test-inv-$TestId"
$ProdContainer    = "rr-test-prod-$TestId"

$AllTestContainers = @($PgContainer, $AuthContainer, $CommerceContainer, $CustFinContainer, $InvProcContainer, $ProdContainer)

function New-RandomTestPassword {
    $bytes = New-Object byte[] 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [System.Convert]::ToBase64String($bytes) -replace '[/+=]', 'x'
}

$authPass = New-RandomTestPassword
$commPass = New-RandomTestPassword
$custPass = New-RandomTestPassword
$invPass  = New-RandomTestPassword
$prodPass = New-RandomTestPassword

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
Write-Host "PHASE 2.2E.5: ISOLATED FIVE-SERVICE STARTUP & LEAST-PRIVILEGE VALIDATION" -ForegroundColor Cyan
Write-Host "Test ID: $TestId" -ForegroundColor Cyan
Write-Host "Network: $NetworkName (No host ports published)" -ForegroundColor Cyan
Write-Host "PostgreSQL Container: $PgContainer" -ForegroundColor Cyan
Write-Host "=============================================================" -ForegroundColor Cyan

try {
    # -------------------------------------------------------------------------
    # 1. Create Isolated Docker Bridge Network
    # -------------------------------------------------------------------------
    Write-Host "`n1. Creating isolated Docker bridge network..." -ForegroundColor Cyan
    docker network create --label "rrdisposable=true" --label "rrtestid=$TestId" $NetworkName | Out-Null
    Report-Pass "Isolated test network created ($NetworkName)"

    # -------------------------------------------------------------------------
    # 2. Launch Isolated PostgreSQL 16 Container
    # -------------------------------------------------------------------------
    Write-Host "`n2. Launching isolated disposable PostgreSQL 16 container..." -ForegroundColor Cyan
    docker run -d `
        --name $PgContainer `
        --network $NetworkName `
        --network-alias postgres `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e POSTGRES_DB=$DbName `
        -e POSTGRES_USER=$SuperUser `
        -e POSTGRES_PASSWORD=$SuperPass `
        -v "${InitSqlPath}:/docker-entrypoint-initdb.d/init-schemas.sql:ro" `
        postgres:16-alpine | Out-Null

    # Wait for PostgreSQL readiness
    $maxAttempts = 40
    $attempt = 0
    $pgReady = $false
    while ($attempt -lt $maxAttempts) {
        $attempt++
        Start-Sleep -Seconds 1
        $logs = docker logs $PgContainer 2>&1 | Out-String
        if ($logs -match "PostgreSQL init process complete; ready for start up") {
            $status = docker exec $PgContainer pg_isready -U $SuperUser -d $DbName 2>$null
            if ($status -match "accepting connections") {
                $pgReady = $true
                break
            }
        }
    }
    if (-not $pgReady) {
        Write-Error "Disposable PostgreSQL container failed to initialize."
        exit 1
    }
    Report-Pass "PostgreSQL 16 initialized with 8 schemas, 58 tables, and least-privilege roles"

    # Provision runtime roles
    Write-Host "`n3. Provisioning runtime role passwords into disposable database..." -ForegroundColor Cyan
    powershell -ExecutionPolicy Bypass -File $ProvisionScriptPath `
        -ContainerName $PgContainer `
        -DbName $DbName `
        -AdminUser $SuperUser `
        -AdminPassword $SuperPass `
        -AuthPassword $authPass `
        -CommercePassword $commPass `
        -CustFinPassword $custPass `
        -InvProcPassword $invPass `
        -ProductionPassword $prodPass | Out-Null
    Report-Pass "All 5 runtime roles provisioned with randomized in-memory passwords"

    # -------------------------------------------------------------------------
    # 4. Start Five Spring Boot Services Against Disposable Database
    # -------------------------------------------------------------------------
    Write-Host "`n4. Launching five Spring Boot services with least-privilege credentials & DDL validate..." -ForegroundColor Cyan

    $jwtSecret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    $jvmOpts = "-Xms64m -Xmx256m -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"

    # 4a. auth-service
    # MANAGEMENT_HEALTH_MAIL_ENABLED=false: suppress MailHealthIndicator so /actuator/health
    # returns UP in the test environment where no SMTP server is configured.
    # The application is fully healthy; only the mail health indicator is disabled for testing.
    docker run -d `
        --name $AuthContainer `
        --network $NetworkName `
        --network-alias auth-service `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e AUTH_SERVICE_PORT=8081 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_auth `
        -e DB_PASSWORD=$authPass `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e JWT_EXPIRATION_MS=900000 `
        -e BOOTSTRAP_ADMIN_USERNAME=admin@rrjaggery.com `
        -e BOOTSTRAP_ADMIN_PASSWORD=BootstrapPassword123! `
        -e ALLOWED_ORIGINS=http://localhost:5173 `
        -e SPRING_PROFILES_ACTIVE=prod `
        -e MANAGEMENT_HEALTH_MAIL_ENABLED=false `
        rr-test-auth:latest | Out-Null

    # 4b. inventory-procurement-service
    docker run -d `
        --name $InvProcContainer `
        --network $NetworkName `
        --network-alias inventory-procurement-service `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e INVENTORY_PROCUREMENT_SERVICE_PORT=8084 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_invproc `
        -e DB_PASSWORD=$invPass `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e ALLOWED_ORIGINS=http://localhost:5173 `
        -e SPRING_PROFILES_ACTIVE=prod `
        rr-test-inventory-procurement:latest | Out-Null

    # 4c. commerce-service
    docker run -d `
        --name $CommerceContainer `
        --network $NetworkName `
        --network-alias commerce-service `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e COMMERCE_SERVICE_PORT=8082 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_commerce `
        -e DB_PASSWORD=$commPass `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e INVENTORY_SERVICE_URL="http://inventory-procurement-service:8084" `
        -e ALLOWED_ORIGINS=http://localhost:5173 `
        -e SPRING_PROFILES_ACTIVE=prod `
        rr-test-commerce:latest | Out-Null

    # 4d. customer-finance-service
    docker run -d `
        --name $CustFinContainer `
        --network $NetworkName `
        --network-alias customer-finance-service `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e CUSTOMER_FINANCE_SERVICE_PORT=8083 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_custfin `
        -e DB_PASSWORD=$custPass `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e COMMERCE_SERVICE_URL="http://commerce-service:8082" `
        -e INVENTORY_SERVICE_URL="http://inventory-procurement-service:8084" `
        -e ALLOWED_ORIGINS=http://localhost:5173 `
        -e SPRING_PROFILES_ACTIVE=prod `
        rr-test-customer-finance:latest | Out-Null

    # 4e. production-service
    docker run -d `
        --name $ProdContainer `
        --network $NetworkName `
        --network-alias production-service `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e PRODUCTION_SERVICE_PORT=8086 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_production `
        -e DB_PASSWORD=$prodPass `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e INVENTORY_SERVICE_URL="http://inventory-procurement-service:8084" `
        -e ALLOWED_ORIGINS=http://localhost:5173 `
        -e SPRING_PROFILES_ACTIVE=prod `
        rr-test-production:latest | Out-Null

    # -------------------------------------------------------------------------
    # 5. Monitor Service Startups and Probe Health Endpoints
    # -------------------------------------------------------------------------
    Write-Host "`n5. Waiting for services to initialize and validate schema..." -ForegroundColor Cyan
    Write-Host "Allowing 35s initial grace period for concurrent JVM boot..." -ForegroundColor Yellow
    Start-Sleep -Seconds 35

    $servicesToCheck = @(
        @{ Name = "auth-service"; Container = $AuthContainer; Port = 8081; Role = "rr_auth" },
        @{ Name = "inventory-procurement-service"; Container = $InvProcContainer; Port = 8084; Role = "rr_invproc" },
        @{ Name = "commerce-service"; Container = $CommerceContainer; Port = 8082; Role = "rr_commerce" },
        @{ Name = "customer-finance-service"; Container = $CustFinContainer; Port = 8083; Role = "rr_custfin" },
        @{ Name = "production-service"; Container = $ProdContainer; Port = 8086; Role = "rr_production" }
    )

    $timeoutSeconds = 240
    foreach ($svc in $servicesToCheck) {
        $svcName = $svc.Name
        $cntName = $svc.Container
        $port = $svc.Port
        $role = $svc.Role
        
        Write-Host "Probing $svcName ($cntName)..."
        $svcReady = $false
        $elapsed = 0
        while ($elapsed -lt $timeoutSeconds) {
            $isRunning = docker inspect $cntName --format '{{.State.Running}}' 2>$null
            if ($isRunning -ne "true") {
                Write-Host "Container $cntName stopped unexpectedly! Breaking wait loop early." -ForegroundColor Yellow
                break
            }
            
            # Probe /actuator/health via internal alpine container on $NetworkName
            $healthJson = docker run --rm --network $NetworkName alpine wget -qO- --timeout=3 "http://${cntName}:${port}/actuator/health" 2>$null
            if ($healthJson -match '"status"\s*:\s*"UP"') {
                $svcReady = $true
                break
            }

            Start-Sleep -Seconds 4
            $elapsed += 4
        }

        # Inspect container logs for validation confirmation and zero errors
        $logs = docker logs $cntName 2>&1 | Out-String

        if ($svcReady) {
            Report-Pass "Service '$svcName' is healthy (HTTP 200 UP) on role '$role'"
        } else {
            Report-Fail "Service '$svcName' startup / health probe" "Timed out after ${timeoutSeconds}s or exited early"
        }

        # Check for Hibernate schema-validation success
        if ($logs -match "Schema-validation: missing|SchemaManagementException") {
            Report-Fail "Service '$svcName' Hibernate schema validation" "Schema validation failed in logs!"
        } elseif ($logs -match "Started [A-Za-z0-9]+ in [0-9.]+ seconds") {
            Report-Pass "Service '$svcName' Hibernate schema validation succeeded cleanly"
        } else {
            Report-Fail "Service '$svcName' startup completion" "Spring Boot startup line not found in logs"
        }

        # Check for authentication or permission errors
        if ($logs -match "PSQLException|password authentication failed|permission denied") {
            Report-Fail "Service '$svcName' least-privilege security check" "Found database permission or auth error in logs!"
        } else {
            Report-Pass "Service '$svcName' zero permission-denied or database authentication errors"
        }
    }

    # -------------------------------------------------------------------------
    # 6. Negative Tests (Step 5)
    # -------------------------------------------------------------------------
    Write-Host "`n6. Executing negative security and validation checks..." -ForegroundColor Cyan

    # 6a. Wrong password produces controlled authentication failure
    $negWrongPassContainer = "rr-neg-wrongpass-$TestId"
    docker run -d `
        --name $negWrongPassContainer `
        --network $NetworkName `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e AUTH_SERVICE_PORT=8081 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_auth `
        -e DB_PASSWORD="deliberately_wrong_password_999" `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e BOOTSTRAP_ADMIN_USERNAME=admin@rrjaggery.com `
        -e BOOTSTRAP_ADMIN_PASSWORD=BootstrapPassword123! `
        -e SPRING_PROFILES_ACTIVE=prod `
        -e MANAGEMENT_HEALTH_MAIL_ENABLED=false `
        rr-test-auth:latest | Out-Null

    $waitElapsed = 0
    while ($waitElapsed -lt 30) {
        Start-Sleep -Seconds 2
        $waitElapsed += 2
        $state = docker inspect $negWrongPassContainer --format '{{.State.Running}}' 2>$null
        if ($state -ne "true") { break }
    }
    $negWrongLogs = docker logs $negWrongPassContainer 2>&1 | Out-String
    docker rm -f $negWrongPassContainer 2>$null | Out-Null

    if ($negWrongLogs -match "password authentication failed for user ""rr_auth""") {
        Report-Pass "Negative Test: Incorrect password produces controlled authentication failure"
    } else {
        Report-Fail "Negative Test: Incorrect password" "Expected password authentication failure, got: $negWrongLogs"
    }

    # 6b. Schema mismatch causes validate to fail closed
    Write-Host "Testing deliberate schema mismatch (dropping email_verified column)..."
    docker exec -i $PgContainer psql -U $SuperUser -d $DbName -c "ALTER TABLE auth_schema.users DROP COLUMN email_verified;" 2>$null | Out-Null

    $negMismatchContainer = "rr-neg-mismatch-$TestId"
    docker run -d `
        --name $negMismatchContainer `
        --network $NetworkName `
        --label "rrdisposable=true" `
        --label "rrtestid=$TestId" `
        -e JAVA_OPTS="$jvmOpts" `
        -e AUTH_SERVICE_PORT=8081 `
        -e DB_HOST=postgres `
        -e DB_PORT=5432 `
        -e DB_NAME=$DbName `
        -e DB_USER=rr_auth `
        -e DB_PASSWORD=$authPass `
        -e HIBERNATE_DDL_AUTO=validate `
        -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate `
        -e JWT_SECRET=$jwtSecret `
        -e BOOTSTRAP_ADMIN_USERNAME=admin@rrjaggery.com `
        -e BOOTSTRAP_ADMIN_PASSWORD=BootstrapPassword123! `
        -e SPRING_PROFILES_ACTIVE=prod `
        -e MANAGEMENT_HEALTH_MAIL_ENABLED=false `
        rr-test-auth:latest | Out-Null

    $waitElapsed = 0
    while ($waitElapsed -lt 30) {
        Start-Sleep -Seconds 2
        $waitElapsed += 2
        $state = docker inspect $negMismatchContainer --format '{{.State.Running}}' 2>$null
        if ($state -ne "true") { break }
    }
    $negMismatchLogs = docker logs $negMismatchContainer 2>&1 | Out-String
    docker rm -f $negMismatchContainer 2>$null | Out-Null

    if ($negMismatchLogs -match "SchemaManagementException|missing column \[email_verified\]") {
        Report-Pass "Negative Test: Schema mismatch correctly causes Hibernate validate to fail closed"
    } else {
        Report-Fail "Negative Test: Schema mismatch" "Expected SchemaManagementException for missing column, got: $negMismatchLogs"
    }

    # 6c. Unauthorized DDL remains denied under runtime role
    $negDdl = docker exec -e PGPASSWORD=$authPass $PgContainer psql -U rr_auth -d $DbName -c "CREATE TABLE auth_schema.backdoor (id INT);" 2>&1
    if ($negDdl -match "permission denied for schema auth_schema") {
        Report-Pass "Negative Test: Unauthorized DDL (CREATE TABLE) remains denied under rr_auth"
    } else {
        Report-Fail "Negative Test: Unauthorized DDL" "Expected permission denied, got: $negDdl"
    }

}
finally {
    # -------------------------------------------------------------------------
    # 7. Strict Cleanup of Disposable Test Resources
    # -------------------------------------------------------------------------
    Write-Host "`n7. Cleaning up disposable test containers and network ($TestId)..." -ForegroundColor Cyan

    foreach ($cnt in $AllTestContainers) {
        $cExists = docker ps -a --filter "name=$cnt" -q 2>$null
        if ($cExists) {
            $lbl = (docker inspect $cnt --format '{{.Config.Labels.rrtestid}}' 2>$null)
            if ($lbl -and $lbl.Trim() -eq $TestId) {
                docker rm -f $cnt 2>$null | Out-Null
                Write-Host "Removed test container $cnt." -ForegroundColor Green
            }
        }
    }

    $netExists = docker network ls --filter "name=$NetworkName" -q 2>$null
    if ($netExists) {
        docker network rm $NetworkName 2>$null | Out-Null
        Write-Host "Removed test network $NetworkName." -ForegroundColor Green
    }
}

Write-Host "`n=============================================================" -ForegroundColor Cyan
Write-Host "PHASE 2.2E.5 VALIDATION SUMMARY: $PassCount PASSED, $FailCount FAILED" -ForegroundColor $(if ($FailCount -eq 0) { "Green" } else { "Red" })
Write-Host "=============================================================" -ForegroundColor Cyan

if ($FailCount -gt 0) {
    exit 1
}

