$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  RR JAGGERY TRADERS — SPRINT 7 INTEGRATED VERIFICATION" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$passed = 0
$failed = 0

function Assert-Condition($name, $condition, $details="") {
    if ($condition) {
        Write-Host "  [PASS] $name" -ForegroundColor Green
        if ($details) { Write-Host "         $details" -ForegroundColor DarkGray }
        $script:passed++
    } else {
        Write-Host "  [FAIL] $name" -ForegroundColor Red
        if ($details) { Write-Host "         $details" -ForegroundColor Yellow }
        $script:failed++
    }
}

# 1. Login as ADMIN and MANAGER
Write-Host "`n1. Authentication & RBAC Verification" -ForegroundColor Yellow
$adminAuth = Invoke-RestMethod -Uri "http://localhost/api/v1/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"admin@rrjaggery.com","password":"Admin@123"}'
$adminToken = $adminAuth.data.token
Assert-Condition "ADMIN Login Success" ($adminToken -ne $null) "User: $($adminAuth.data.user.email), Roles: $($adminAuth.data.user.roles -join ', ')"

$managerAuth = Invoke-RestMethod -Uri "http://localhost/api/v1/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"manager@rrjaggery.com","password":"Manager@123"}'
$managerToken = $managerAuth.data.token
Assert-Condition "MANAGER Login Success" ($managerToken -ne $null) "User: $($managerAuth.data.user.email), Roles: $($managerAuth.data.user.roles -join ', ')"

# Register/Login CUSTOMER
$custEmail = "cust_sprint7_" + (Get-Random) + "@test.com"
$custRegisterPayload = @{
    email = $custEmail
    password = "Customer@123"
    fullName = "Sprint7 Test Customer"
    customerType = "RETAIL"
} | ConvertTo-Json

$custRegister = Invoke-RestMethod -Uri "http://localhost/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $custRegisterPayload
$customerToken = $custRegister.data.token
Assert-Condition "CUSTOMER Registration/Login Success" ($customerToken -ne $null) "User: $custEmail, Roles: $($custRegister.data.user.roles -join ', ')"

# Verify CUSTOMER 403 Forbidden on internal APIs
$customerDenied = $false
try {
    Invoke-RestMethod -Uri "http://localhost/api/v1/notifications" -Headers @{Authorization="Bearer $customerToken"} -Method Get
} catch {
    $code = [int]$_.Exception.Response.StatusCode
    if ($code -eq 403) { $customerDenied = $true }
}
Assert-Condition "CUSTOMER access to Notification Service returns HTTP 403" $customerDenied

$customerFinanceDenied = $false
try {
    Invoke-RestMethod -Uri "http://localhost/api/v1/finance/expenses" -Headers @{Authorization="Bearer $customerToken"} -Method Get
} catch {
    $code = [int]$_.Exception.Response.StatusCode
    if ($code -eq 403) { $customerFinanceDenied = $true }
}
Assert-Condition "CUSTOMER access to Finance Expenses returns HTTP 403" $customerFinanceDenied

$customerInventoryDenied = $false
try {
    Invoke-RestMethod -Uri "http://localhost/api/v1/inventory/items" -Headers @{Authorization="Bearer $customerToken"} -Method Get
} catch {
    $code = [int]$_.Exception.Response.StatusCode
    if ($code -eq 403) { $customerInventoryDenied = $true }
}
Assert-Condition "CUSTOMER access to Inventory Items returns HTTP 403" $customerInventoryDenied

# 2. Notifications & In-App Alerts Service
Write-Host "`n2. Notifications & Operational Alerts Service" -ForegroundColor Yellow
$adminHeaders = @{Authorization="Bearer $adminToken"}

# Sync Operational Alerts
$syncPayload = @(
    @{
        type = "LOW_STOCK"
        severity = "CRITICAL"
        title = "Low Stock Alert: Organic Sugarcane"
        message = "Current stock (80 KG) is below configured minimum stock level (100 KG)."
        referenceId = "INV-RAW-001"
        referenceType = "INVENTORY"
        role = "MANAGER"
    },
    @{
        type = "SUPPLIER_PAYABLE"
        severity = "WARNING"
        title = "Supplier Payable Outstanding: Ramesh Farms"
        message = "Pending payment of Rs 45,000 for raw sugarcane consignment."
        referenceId = "SUP-001"
        referenceType = "SUPPLIER"
        role = "MANAGER"
    },
    @{
        type = "CUSTOMER_RECEIVABLE"
        severity = "INFO"
        title = "Customer Receivable: Green Earth Wholesalers"
        message = "Outstanding payment of Rs 1,20,000 pending from wholesale account."
        referenceId = "CUST-001"
        referenceType = "CUSTOMER"
        role = "MANAGER"
    }
) | ConvertTo-Json

$syncRes = Invoke-RestMethod -Uri "http://localhost/api/v1/notifications/sync-operational-alerts" -Headers $adminHeaders -Method Post -ContentType "application/json" -Body $syncPayload
Assert-Condition "Sync Operational Alerts API" ($syncRes.success -eq $true) "Synced count: $($syncRes.data.Count)"

$notifs = Invoke-RestMethod -Uri "http://localhost/api/v1/notifications" -Headers $adminHeaders -Method Get
Assert-Condition "List Notifications API" ($notifs.data.Count -gt 0) "Total notifications: $($notifs.data.Count)"

$unreadCount = Invoke-RestMethod -Uri "http://localhost/api/v1/notifications/unread-count" -Headers $adminHeaders -Method Get
Assert-Condition "Unread Notifications Count API" ($unreadCount.data.unreadCount -ge 0) "Unread: $($unreadCount.data.unreadCount)"

# Mark notification read
if ($notifs.data.Count -gt 0) {
    $firstId = $notifs.data[0].id
    $readRes = Invoke-RestMethod -Uri "http://localhost/api/v1/notifications/$firstId/read" -Headers $adminHeaders -Method Put
    $isReadSuccess = ($readRes.data.read -eq $true -or $readRes.data.isRead -eq $true)
    Assert-Condition "Mark Notification As Read API" $isReadSuccess "Notification ID: $firstId marked read"
}

# 3. Domain Reports Data Verification
Write-Host "`n3. Domain Reports Data Verification" -ForegroundColor Yellow

# A. Inventory Report
$inv = Invoke-RestMethod -Uri "http://localhost/api/v1/inventory/items" -Headers $adminHeaders -Method Get
Assert-Condition "Inventory Items Authoritative Source" ($inv.data.Count -gt 0) "Total Tracked Items: $($inv.data.Count)"

# B. Production Report
$prod = Invoke-RestMethod -Uri "http://localhost/api/v1/production/batches" -Headers $adminHeaders -Method Get
Assert-Condition "Production Batches Authoritative Source" ($prod.data.Count -gt 0) "Total Batches: $($prod.data.Count)"

# C. Suppliers Report
$supp = Invoke-RestMethod -Uri "http://localhost/api/v1/procurement/suppliers" -Headers $adminHeaders -Method Get
Assert-Condition "Suppliers Authoritative Source" ($supp.data.Count -gt 0) "Total Suppliers: $($supp.data.Count)"

# D. Operating Expenses Report
$exp = Invoke-RestMethod -Uri "http://localhost/api/v1/finance/expenses?year=2026&month=9" -Headers $adminHeaders -Method Get
Assert-Condition "Operating Expenses Period Filter (Sep 2026)" ($exp.data -ne $null) "Expenses in Period: $($exp.data.Count)"

# E. Empty Period Test (Year 2027 Month 1 - Known Zero Period)
Write-Host "`n4. Mandatory Empty Period Test" -ForegroundColor Yellow
$emptyExp = Invoke-RestMethod -Uri "http://localhost/api/v1/finance/expenses?year=2027&month=1" -Headers $adminHeaders -Method Get
Assert-Condition "Empty Period Expenses returns 0 items" ($emptyExp.data.Count -eq 0) "Returned count: $($emptyExp.data.Count) (No silent fallback)"

# F. Dynamic Financial Masters (Sprint 6 & 7)
Write-Host "`n5. Dynamic Masters Verification (No hardcoding)" -ForegroundColor Yellow
$expCatMasters = Invoke-RestMethod -Uri "http://localhost/api/v1/finance/business-masters/expense-categories" -Headers $adminHeaders -Method Get
Assert-Condition "Dynamic Expense Categories from Master" ($expCatMasters.data.Count -gt 0) "Categories: $($expCatMasters.data.name -join ', ')"

$payMethodMasters = Invoke-RestMethod -Uri "http://localhost/api/v1/finance/business-masters/payment-methods" -Headers $adminHeaders -Method Get
Assert-Condition "Dynamic Payment Methods from Master" ($payMethodMasters.data.Count -gt 0) "Methods: $($payMethodMasters.data.name -join ', ')"

$prodCostMasters = Invoke-RestMethod -Uri "http://localhost/api/v1/production/cost-masters" -Headers $adminHeaders -Method Get
Assert-Condition "Dynamic Production Cost Masters" ($prodCostMasters.data.Count -gt 0) "Cost Masters: $($prodCostMasters.data.costName -join ', ')"

# Summary
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  VERIFICATION SUMMARY: $passed Passed, $failed Failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "============================================================" -ForegroundColor Cyan
