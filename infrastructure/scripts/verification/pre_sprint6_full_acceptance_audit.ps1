$ErrorActionPreference = "Continue"
$BaseUrl = "http://localhost"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   PRE-SPRINT 6 FINAL ACCEPTANCE AUDIT & INTEGRITY VERIFICATION  " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# ─────────────────────────────────────────────────────────────────────────────
# 1. SECURITY & ROLE-BASED ACCESS CONTROL AUDIT
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "`n>>> [1. SECURITY & ACCESS CONTROL] <<<" -ForegroundColor Yellow

# A. Customer Token
Write-Host "  [1.1] Testing Customer Authentication (retail@example.com)..." -ForegroundColor White
$custLogin = @{ email = "retail@example.com"; password = "Retail@123" } | ConvertTo-Json
$custResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $custLogin -ContentType "application/json"
$custToken = $custResp.data.token
$custHeaders = @{ Authorization = "Bearer $custToken" }
Write-Host "    ✅ Customer logged in successfully (Role: $($custResp.data.roles -join ', '))" -ForegroundColor Green

# Verify Customer CANNOT access operational APIs
$forbiddenEndpoints = @(
    @{ Name = "Inventory Items"; Uri = "$BaseUrl/api/v1/inventory/items" },
    @{ Name = "Procurement Suppliers"; Uri = "$BaseUrl/api/v1/procurement/suppliers" },
    @{ Name = "Production Batches"; Uri = "$BaseUrl/api/v1/production/batches" },
    @{ Name = "Finance Expenses"; Uri = "$BaseUrl/api/v1/finance/expenses" },
    @{ Name = "Finance Employees"; Uri = "$BaseUrl/api/v1/finance/employees" },
    @{ Name = "Customer Directory"; Uri = "$BaseUrl/api/v1/customers" },
    @{ Name = "Admin User Management"; Uri = "$BaseUrl/api/v1/auth/admin/users" }
)

foreach ($ep in $forbiddenEndpoints) {
    try {
        $resp = Invoke-RestMethod -Uri $ep.Uri -Method Get -Headers $custHeaders -ErrorAction Stop
        Write-Host "    ❌ SECURITY BREACH: Customer accessed $($ep.Name)!" -ForegroundColor Red
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq 403 -or $code -eq 401) {
            Write-Host "    ✅ Customer blocked from $($ep.Name) (HTTP $code Forbidden/Unauthorized)" -ForegroundColor Green
        } else {
            Write-Host "    ⚠️ Customer blocked from $($ep.Name) with HTTP $code" -ForegroundColor Yellow
        }
    }
}

# Verify Customer CAN access Storefront products and customer orders
try {
    $prodResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/commerce/products" -Method Get -ErrorAction Stop
    Write-Host "    ✅ Customer can access Storefront catalog ($($prodResp.data.Count) products)" -ForegroundColor Green
} catch {
    Write-Host "    ❌ Failed to access products catalog" -ForegroundColor Red
}

# B. Manager Token & Access
Write-Host "`n  [1.2] Testing Manager Authentication (manager@rrjaggery.com)..." -ForegroundColor White
$mgrLogin = @{ email = "manager@rrjaggery.com"; password = "Manager@123" } | ConvertTo-Json
$mgrResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $mgrLogin -ContentType "application/json"
$mgrToken = $mgrResp.data.token
$mgrHeaders = @{ Authorization = "Bearer $mgrToken" }
Write-Host "    ✅ Manager logged in successfully (Roles: $($mgrResp.data.roles -join ', '))" -ForegroundColor Green

# Verify Manager CAN access operational APIs
$mgrEndpoints = @(
    @{ Name = "Inventory Items"; Uri = "$BaseUrl/api/v1/inventory/items" },
    @{ Name = "Procurement Suppliers"; Uri = "$BaseUrl/api/v1/procurement/suppliers" },
    @{ Name = "Production Batches"; Uri = "$BaseUrl/api/v1/production/batches" },
    @{ Name = "Finance Expenses"; Uri = "$BaseUrl/api/v1/finance/expenses" },
    @{ Name = "Finance Employees"; Uri = "$BaseUrl/api/v1/finance/employees" },
    @{ Name = "Customer Directory"; Uri = "$BaseUrl/api/v1/customers" },
    @{ Name = "Offline Wholesale Orders"; Uri = "$BaseUrl/api/v1/customers/offline-orders" }
)

foreach ($ep in $mgrEndpoints) {
    try {
        $resp = Invoke-RestMethod -Uri $ep.Uri -Method Get -Headers $mgrHeaders -ErrorAction Stop
        Write-Host "    ✅ Manager operational access to $($ep.Name): OK (HTTP 200)" -ForegroundColor Green
    } catch {
        Write-Host "    ❌ Manager failed to access $($ep.Name): $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Verify Manager CANNOT access Admin-only endpoints
try {
    $adminUsersResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/admin/users" -Method Get -Headers $mgrHeaders -ErrorAction Stop
    Write-Host "    ❌ SECURITY BREACH: Manager accessed Admin user management!" -ForegroundColor Red
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 403) {
        Write-Host "    ✅ Manager blocked from Admin User Management (HTTP 403 Forbidden)" -ForegroundColor Green
    } else {
        Write-Host "    ⚠️ Manager blocked from Admin with HTTP $code" -ForegroundColor Yellow
    }
}

# C. Admin Token & Access
Write-Host "`n  [1.3] Testing Admin Authentication (admin@rrjaggery.com)..." -ForegroundColor White
$admLogin = @{ email = "admin@rrjaggery.com"; password = "Admin@123" } | ConvertTo-Json
$admResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $admLogin -ContentType "application/json"
$admToken = $admResp.data.token
$admHeaders = @{ Authorization = "Bearer $admToken" }
Write-Host "    ✅ Admin logged in successfully (Roles: $($admResp.data.roles -join ', '))" -ForegroundColor Green

# Admin User Management & Stats
try {
    $usersResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/admin/users" -Method Get -Headers $admHeaders -ErrorAction Stop
    $statsResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/admin/stats" -Method Get -Headers $admHeaders -ErrorAction Stop
    Write-Host "    ✅ Admin access to User Management: OK ($($usersResp.data.Count) accounts)" -ForegroundColor Green
    Write-Host "    ✅ Admin Stats: Total=$($statsResp.data.totalUsers), Admins=$($statsResp.data.adminCount), Managers=$($statsResp.data.managerCount), Customers=$($statsResp.data.retailCustomerCount)" -ForegroundColor Green
} catch {
    Write-Host "    ❌ Admin failed user management: $($_.Exception.Message)" -ForegroundColor Red
}

# D. Retired Roles Verification (Wholesale & Employee no login)
Write-Host "`n  [1.4] Testing Retired Role Logins (Must FAIL)..." -ForegroundColor White
$wsLogin = @{ email = "wholesale@example.com"; password = "Wholesale@123" } | ConvertTo-Json
try {
    $wsResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $wsLogin -ContentType "application/json" -ErrorAction Stop
    Write-Host "    ❌ SECURITY ISSUE: Wholesale customer account logged in!" -ForegroundColor Red
} catch {
    Write-Host "    ✅ Wholesale customer login rejected (Disabled/No login permitted)" -ForegroundColor Green
}

# ─────────────────────────────────────────────────────────────────────────────
# 2. DATA RECONCILIATION & POSTGRESQL INTEGRITY
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "`n>>> [2. DATA INTEGRITY CROSS-CHECK] <<<" -ForegroundColor Yellow

# A. Stock: Finished Goods = 1185 KG
$invResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/inventory/items" -Method Get -Headers $mgrHeaders
$fgItems = $invResp.data | Where-Object { $_.itemType -eq "FINISHED_GOOD" -or $_.itemType -eq "FINISHED_GOODS" }
$totalFgStock = ($fgItems | Measure-Object -Property currentQuantity -Sum).Sum
Write-Host "  Finished Goods Items ($($fgItems.Count) items):" -ForegroundColor Gray
foreach ($item in $fgItems) {
    Write-Host "    - $($item.sku) | $($item.itemName): $($item.currentQuantity) $($item.unitOfMeasure)" -ForegroundColor Gray
}
Write-Host "  Total Finished Goods Stock: $totalFgStock KG" -ForegroundColor Gray
if ($totalFgStock -eq 1185) {
    Write-Host "  ✅ Physical stock matches exactly: 1,185.000 KG" -ForegroundColor Green
} else {
    Write-Host "  ❌ Physical stock mismatch: Found $totalFgStock KG (Expected 1185 KG)" -ForegroundColor Red
}

# B. Supplier Payables: ₹207,000 across 5 suppliers
$supResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/procurement/suppliers" -Method Get -Headers $mgrHeaders
$suppliersWithBal = $supResp.data | Where-Object { $_.currentOutstanding -gt 0 }
$totalPayable = ($suppliersWithBal | Measure-Object -Property currentOutstanding -Sum).Sum
Write-Host "  Suppliers with Outstanding Balance ($($suppliersWithBal.Count) suppliers):" -ForegroundColor Gray
foreach ($s in $suppliersWithBal) {
    Write-Host "    - $($s.supplierCode) | $($s.supplierName): ₹$($s.currentOutstanding)" -ForegroundColor Gray
}
Write-Host "  Total Supplier Payables: ₹$totalPayable" -ForegroundColor Gray
if ($totalPayable -eq 207000 -and $suppliersWithBal.Count -eq 5) {
    Write-Host "  ✅ Supplier Payables match exactly: ₹207,000.00 across 5 suppliers" -ForegroundColor Green
} else {
    Write-Host "  ❌ Supplier Payables mismatch: Found ₹$totalPayable" -ForegroundColor Red
}

# C. Production Batches: 3 completed = 285 KG
$batchResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/production/batches" -Method Get -Headers $mgrHeaders
$completedBatches = $batchResp.data | Where-Object { $_.status -eq "COMPLETED" }
$totalProdQty = 0
foreach ($b in $completedBatches) {
    $qty = if ($b.actualQuantity) { $b.actualQuantity } elseif ($b.actualQuantityKg) { $b.actualQuantityKg } else { $b.plannedQuantityKg }
    $totalProdQty += $qty
}
Write-Host "  Completed Batches ($($completedBatches.Count) batches, Total: $totalProdQty KG)" -ForegroundColor Gray
if ($completedBatches.Count -eq 3 -and $totalProdQty -eq 285) {
    Write-Host "  ✅ Production Batches match exactly: 3 completed batches = 285 KG" -ForegroundColor Green
} else {
    Write-Host "  ❌ Production Batches mismatch" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────────────────────────
# 3. WHOLESALE & OFFLINE ORDER WORKFLOW AUDIT
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "`n>>> [3. WHOLESALE & B2B WORKFLOW] <<<" -ForegroundColor Yellow

$wsCustResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers" -Method Get -Headers $mgrHeaders
Write-Host "  Wholesale Customer records ($($wsCustResp.data.Count) wholesalers):" -ForegroundColor Gray
foreach ($wc in $wsCustResp.data) {
    Write-Host "    - $($wc.businessName) | Contact: $($wc.contactPerson) | Outstanding: ₹$($wc.currentOutstanding)" -ForegroundColor Gray
}

$wsOrdersResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/customers/offline-orders" -Method Get -Headers $mgrHeaders
Write-Host "  Wholesale Order records ($($wsOrdersResp.data.Count) orders):" -ForegroundColor Gray
foreach ($wo in $wsOrdersResp.data) {
    Write-Host "    - Order #$($wo.orderNumber) | Invoice: $($wo.invoiceNumber) | Total: ₹$($wo.totalAmount) | Status: $($wo.paymentStatus)" -ForegroundColor Gray
}

# ─────────────────────────────────────────────────────────────────────────────
# 4. STOREFRONT & IMAGE ROUTING AUDIT
# ─────────────────────────────────────────────────────────────────────────────
Write-Host "`n>>> [4. STOREFRONT & PRODUCT IMAGE PIPELINE] <<<" -ForegroundColor Yellow

$prods = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/commerce/products" -Method Get).data
Write-Host "  Storefront Products ($($prods.Count) products):" -ForegroundColor Gray
foreach ($p in $prods) {
    Write-Host "    - $($p.name) (Price: ₹$($p.retailPrice), Image: $($p.imagePath))" -ForegroundColor Gray
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "             ACCEPTANCE AUDIT SCRIPT COMPLETED                   " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
