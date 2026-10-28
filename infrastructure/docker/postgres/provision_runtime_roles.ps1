# =============================================================================
# RR JAGGERY TRADERS — RUNTIME ROLE PASSWORD PROVISIONING SCRIPT
# Script: provision_runtime_roles.ps1
# Target: PostgreSQL 16 Runtime Service Roles
# =============================================================================

[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$ContainerName = "rr-postgres",

    [Parameter(Mandatory = $false)]
    [string]$DbName = "rr_jaggery_db",

    [Parameter(Mandatory = $false)]
    [string]$AdminUser = "postgres",

    [Parameter(Mandatory = $false)]
    [string]$AdminPassword = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } elseif ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "" }),

    [Parameter(Mandatory = $false)]
    [string]$AuthPassword = $env:RR_AUTH_DB_PASSWORD,

    [Parameter(Mandatory = $false)]
    [string]$CommercePassword = $env:RR_COMMERCE_DB_PASSWORD,

    [Parameter(Mandatory = $false)]
    [string]$CustFinPassword = $env:RR_CUSTFIN_DB_PASSWORD,

    [Parameter(Mandatory = $false)]
    [string]$InvProcPassword = $env:RR_INVPROC_DB_PASSWORD,

    [Parameter(Mandatory = $false)]
    [string]$ProductionPassword = $env:RR_PRODUCTION_DB_PASSWORD
)

$ErrorActionPreference = "Stop"

function Format-SqlPasswordLiteral([string]$val, [string]$roleName) {
    if ([string]::IsNullOrWhiteSpace($val)) {
        Write-Error "Password for role '$roleName' is missing or empty. All 5 runtime passwords are required."
        exit 1
    }
    # Reject control characters (CR, LF, NUL) to prevent line injection
    if ($val -match "[\r\n\0]") {
        Write-Error "Password for role '$roleName' contains invalid control characters."
        exit 1
    }
    # Standard SQL string literal: escape single quotes by doubling them
    $escaped = $val.Replace("'", "''")
    return "'$escaped'"
}

# 1. Validate all required passwords before attempting connection
$authLit    = Format-SqlPasswordLiteral $AuthPassword "rr_auth"
$commLit    = Format-SqlPasswordLiteral $CommercePassword "rr_commerce"
$custLit    = Format-SqlPasswordLiteral $CustFinPassword "rr_custfin"
$invLit     = Format-SqlPasswordLiteral $InvProcPassword "rr_invproc"
$prodLit    = Format-SqlPasswordLiteral $ProductionPassword "rr_production"

# 2. Construct safe SQL script for ALTER ROLE
$sqlStream = @"
\set ON_ERROR_STOP on
SET standard_conforming_strings = on;
ALTER ROLE rr_auth WITH LOGIN PASSWORD $authLit;
ALTER ROLE rr_commerce WITH LOGIN PASSWORD $commLit;
ALTER ROLE rr_custfin WITH LOGIN PASSWORD $custLit;
ALTER ROLE rr_invproc WITH LOGIN PASSWORD $invLit;
ALTER ROLE rr_production WITH LOGIN PASSWORD $prodLit;
"@

# 3. Assemble Docker execution parameters
$dockerArgs = @("exec", "-i")
if (-not [string]::IsNullOrEmpty($AdminPassword)) {
    $dockerArgs += @("-e", "PGPASSWORD=$AdminPassword")
}
$dockerArgs += @($ContainerName, "psql", "-U", $AdminUser, "-d", $DbName, "-v", "ON_ERROR_STOP=1", "-q")

# 4. Stream SQL commands into psql via stdin (never exposed in CLI args or process listings)
try {
    $output = $sqlStream | & docker @dockerArgs 2>&1
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0) {
        Write-Error "Failed to provision runtime roles in container '$ContainerName' (Exit code: $exitCode)."
        exit 1
    }

    Write-Host "[SUCCESS] All 5 PostgreSQL runtime roles provisioned with LOGIN and secure passwords." -ForegroundColor Green
    exit 0
}
catch {
    Write-Error "Exception during role password provisioning: $_"
    exit 1
}

