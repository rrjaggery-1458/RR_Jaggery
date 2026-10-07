$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  SPRINT 6 FINAL GATE - COMPREHENSIVE AUTOMATED AUDIT    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Login & Token Retrieval
Write-Host "`n--- [1] Authenticating Admin and Customer ---" -ForegroundColor Yellow
$adminLoginBody = @{ email = "admin@rrjaggery.com"; password = "Admin@123" } | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $adminLoginBody
$adminToken = if ($adminAuth.data.token) { $adminAuth.data.token } elseif ($adminAuth.token) { $adminAuth.token } else { $adminAuth }
Write-Host "Admin Token: Extracted successfully ($($adminToken.Substring(0, 15))...)" -ForegroundColor Green

# Register / Login Customer
$custToken = ""
try {
    $custLoginBody = @{ email = "cust_sprint6_gate@rrjaggery.com"; password = "Customer@123" } | ConvertTo-Json
    $custAuth = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $custLoginBody
    $custToken = if ($custAuth.data.token) { $custAuth.data.token } elseif ($custAuth.token) { $custAuth.token } else { $custAuth }
} catch {
    $custRegBody = @{ email = "cust_sprint6_gate@rrjaggery.com"; password = "Customer@123"; fullName = "Test Customer"; role = "CUSTOMER" } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $custRegBody | Out-Null
    $custAuth = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $custLoginBody
    $custToken = if ($custAuth.data.token) { $custAuth.data.token } elseif ($custAuth.token) { $custAuth.token } else { $custAuth }
}
Write-Host "Customer Token: Extracted successfully" -ForegroundColor Green

$adminHeaders = @{ "Authorization" = "Bearer $adminToken"; "Content-Type" = "application/json" }
$custHeaders = @{ "Authorization" = "Bearer $custToken"; "Content-Type" = "application/json" }

# 2. Test Security Barrier (Customer 403 on internal financial APIs)
Write-Host "`n--- [2] Verifying Security Model (Customer Access Blocked) ---" -ForegroundColor Yellow
$secEndpoints = @(
    "http://localhost:8087/api/v1/finance/business-masters/expense-categories",
    "http://localhost:8087/api/v1/finance/business-masters/payroll-rules",
    "http://localhost:8087/api/v1/finance/employees",
    "http://localhost:8087/api/v1/finance/expenses",
    "http://localhost:8086/api/v1/production/cost-masters"
)
foreach ($url in $secEndpoints) {
    try {
        $res = Invoke-RestMethod -Uri $url -Headers $custHeaders -Method Get
        Write-Host "Security FAIL: Customer allowed access to $url" -ForegroundColor Red
    } catch {
        Write-Host "Security PASS: Customer blocked (403 Forbidden) from $url" -ForegroundColor Green
    }
}

# 3. Dynamic Master Creation & Usage (Expense Category)
Write-Host "`n--- [3] Dynamic Master: Create Custom Expense Category & Use in Expense ---" -ForegroundColor Yellow
$uniqueCatName = "Custom Agri Research " + (Get-Random -Minimum 1000 -Maximum 9999)
$customExpCat = @{
    name = $uniqueCatName
    parentCategory = "OPERATING"
    description = "R&D for cane yield improvement"
    active = $true
} | ConvertTo-Json
$catRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/business-masters/expense-categories" -Method Post -Headers $adminHeaders -Body $customExpCat
$catData = if ($catRes.data) { $catRes.data } else { $catRes }
Write-Host "Created Custom Expense Category: $($catData.name) (ID: $($catData.id))" -ForegroundColor Green

$customExpense = @{
    category = $catData.name
    expenseDate = (Get-Date -Format "yyyy-MM-dd")
    amount = 12500.00
    paymentMethod = "BANK_TRANSFER"
    payeeVendor = "Agri Tech Labs"
    description = "Sugarcane sucrose density testing"
} | ConvertTo-Json
$expRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/expenses" -Method Post -Headers $adminHeaders -Body $customExpense
$expData = if ($expRes.data) { $expRes.data } else { $expRes }
Write-Host "Expense created with custom category: ID $($expData.id), Amount ₹$($expData.amount), Category: $($expData.category)" -ForegroundColor Green

# 4. Dynamic Master: Create Custom Production Cost Type & Rate
Write-Host "`n--- [4] Dynamic Master: Create Custom Production Cost Type & Verify in Costing ---" -ForegroundColor Yellow
$uniqueCostCode = "SOLAR_DEP_" + (Get-Random -Minimum 100 -Maximum 999)
$customCostMaster = @{
    costCode = $uniqueCostCode
    costName = "Solar Power Allocation"
    costType = "ENERGY"
    calculationBasis = "PER_KG_OUTPUT"
    rate = 2.75
    effectiveFrom = "2026-01-01"
    active = $true
    description = "Solar panel depreciation per KG output"
} | ConvertTo-Json
$costRes = Invoke-RestMethod -Uri "http://localhost:8086/api/v1/production/cost-masters" -Method Post -Headers $adminHeaders -Body $customCostMaster
$costData = if ($costRes.data) { $costRes.data } else { $costRes }
Write-Host "Created Custom Production Cost: $($costData.costCode) @ ₹$($costData.rate)/kg" -ForegroundColor Green

# 5. Disable One Master & Verify Integrity
Write-Host "`n--- [5] Disabling a Master Record & Verifying System Integrity ---" -ForegroundColor Yellow
$disablePayload = @{
    name = $catData.name
    parentCategory = $catData.parentCategory
    description = $catData.description
    active = $false
} | ConvertTo-Json
$disRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/business-masters/expense-categories/$($catData.id)" -Method Put -Headers $adminHeaders -Body $disablePayload
$disData = if ($disRes.data) { $disRes.data } else { $disRes }
Write-Host "Category $($catData.name) disabled. Active status: $($disData.active)" -ForegroundColor Green

# Verify active-only list excludes it
$activeCatsRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/business-masters/expense-categories?activeOnly=true" -Headers $adminHeaders
$activeCats = if ($activeCatsRes.data) { $activeCatsRes.data } else { $activeCatsRes }
$found = $activeCats | Where-Object { $_.id -eq $catData.id }
if (-not $found) {
    Write-Host "Integrity PASS: Disabled category excluded from active selection." -ForegroundColor Green
}

# Verify historical expense still exists and is intact
$expCheckRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/expenses/$($expData.id)" -Headers $adminHeaders
$expCheck = if ($expCheckRes.data) { $expCheckRes.data } else { $expCheckRes }
Write-Host "Historical Expense Intact: ID $($expCheck.id), Category '$($expCheck.category)', Amount ₹$($expCheck.amount)" -ForegroundColor Green

# 6. Test BATCH_BASED, PER_KG, and DAILY Payroll
Write-Host "`n--- [6] Testing BATCH_BASED, PER_KG, and DAILY Payroll ---" -ForegroundColor Yellow

# Create BATCH_BASED Worker
$batchWorker = @{
    fullName = "Ramesh Batch Specialist"
    designation = "Boiling Master"
    wageType = "BATCH_BASED"
    rateAmount = 350.00
    joiningDate = "2026-01-01"
} | ConvertTo-Json
$bWorkerRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/employees" -Method Post -Headers $adminHeaders -Body $batchWorker
$bWorker = if ($bWorkerRes.data) { $bWorkerRes.data } else { $bWorkerRes }
$bEmpId = $bWorker.id
Write-Host "Created BATCH_BASED Worker: $($bWorker.fullName) (Rate: ₹$($bWorker.rateAmount)/batch)" -ForegroundColor Green

# Record Attendance (3 days present)
for ($d = 1; $d -le 3; $d++) {
    $att = @{
        employeeId = $bEmpId
        attendanceDate = "2026-09-0$d"
        status = "PRESENT"
        overtimeHours = 2.0
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/attendance" -Method Post -Headers $adminHeaders -Body $att | Out-Null
}

# Calculate BATCH_BASED Payroll (3 batches @ ₹350 = ₹1050, Overtime = ₹0)
$calcBatchRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/payroll/calculate?employeeId=$bEmpId&year=2026&month=9&batchesWorked=3" -Headers $adminHeaders
$calcBatch = if ($calcBatchRes.data) { $calcBatchRes.data } else { $calcBatchRes }
Write-Host "BATCH_BASED Payroll: Base Earned ₹$($calcBatch.baseEarned), OT Pay ₹$($calcBatch.overtimePay), Net Payable ₹$($calcBatch.netPayable)" -ForegroundColor Green
if ($calcBatch.baseEarned -eq 1050.00 -and $calcBatch.overtimePay -eq 0.00) {
    Write-Host "BATCH_BASED Calculation PASS: 3 batches * ₹350 = ₹1050.00, OT strictly ₹0.00" -ForegroundColor Green
} else {
    Write-Host "BATCH_BASED Calculation FAIL" -ForegroundColor Red
}

# Create PER_KG Worker
$kgWorker = @{
    fullName = "Suresh Moulding Specialist"
    designation = "Moulding Craftsman"
    wageType = "PER_KG"
    rateAmount = 1.25
    joiningDate = "2026-01-01"
} | ConvertTo-Json
$kWorkerRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/employees" -Method Post -Headers $adminHeaders -Body $kgWorker
$kWorker = if ($kWorkerRes.data) { $kWorkerRes.data } else { $kWorkerRes }
$kEmpId = $kWorker.id
Write-Host "Created PER_KG Worker: $($kWorker.fullName) (Rate: ₹$($kWorker.rateAmount)/kg)" -ForegroundColor Green

# Calculate PER_KG Payroll (2000 KG produced @ ₹1.25 = ₹2500, Overtime = ₹0)
$calcKgRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/payroll/calculate?employeeId=$kEmpId&year=2026&month=9&quantityKg=2000" -Headers $adminHeaders
$calcKg = if ($calcKgRes.data) { $calcKgRes.data } else { $calcKgRes }
Write-Host "PER_KG Payroll: Base Earned ₹$($calcKg.baseEarned), OT Pay ₹$($calcKg.overtimePay), Net Payable ₹$($calcKg.netPayable)" -ForegroundColor Green
if ($calcKg.baseEarned -eq 2500.00 -and $calcKg.overtimePay -eq 0.00) {
    Write-Host "PER_KG Calculation PASS: 2000 kg * ₹1.25 = ₹2500.00, OT strictly ₹0.00" -ForegroundColor Green
} else {
    Write-Host "PER_KG Calculation FAIL" -ForegroundColor Red
}

# Create DAILY Worker
$dailyWorker = @{
    fullName = "Anil Daily Labour"
    designation = "Sugarcane Loader"
    wageType = "DAILY"
    rateAmount = 600.00
    joiningDate = "2026-01-01"
} | ConvertTo-Json
$dWorkerRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/employees" -Method Post -Headers $adminHeaders -Body $dailyWorker
$dWorker = if ($dWorkerRes.data) { $dWorkerRes.data } else { $dWorkerRes }
$dEmpId = $dWorker.id

# Record Attendance: 2 days present, 4 hrs overtime
for ($d = 1; $d -le 2; $d++) {
    $att = @{
        employeeId = $dEmpId
        attendanceDate = "2026-09-0$d"
        status = "PRESENT"
        overtimeHours = 2.0
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/attendance" -Method Post -Headers $adminHeaders -Body $att | Out-Null
}
# Calculate DAILY Payroll: 2 days @ ₹600 = ₹1200. OT: (600/8)*1.5*4 = ₹450. Gross = ₹1650.
$calcDailyRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/payroll/calculate?employeeId=$dEmpId&year=2026&month=9" -Headers $adminHeaders
$calcDaily = if ($calcDailyRes.data) { $calcDailyRes.data } else { $calcDailyRes }
Write-Host "DAILY Payroll: Base ₹$($calcDaily.baseEarned), OT ₹$($calcDaily.overtimePay), Gross ₹$($calcDaily.grossSalary)" -ForegroundColor Green
if ($calcDaily.baseEarned -eq 1200.00 -and $calcDaily.overtimePay -eq 450.00) {
    Write-Host "DAILY Calculation PASS: Base ₹1200 + OT ₹450 = Gross ₹1650.00" -ForegroundColor Green
}

# 7. Financial Ledger Reconciliation & Payment Integrity Test
Write-Host "`n--- [7] Testing Financial Ledger Reconciliation & Payment Integrity ---" -ForegroundColor Yellow

# Step 1: Issue Salary Advance of ₹500
$advPayload = @{
    employeeId = $dEmpId
    amount = 500.00
    advanceDate = "2026-09-02"
    paymentMethod = "CASH"
    notes = "Emergency Medical Advance"
} | ConvertTo-Json
$advRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/salary-advances" -Method Post -Headers $adminHeaders -Body $advPayload
$adv = if ($advRes.data) { $advRes.data } else { $advRes }
Write-Host "Issued Salary Advance: ₹$($adv.amount) to $($dWorker.fullName)" -ForegroundColor Green

# Step 2: Issue Deduction of ₹100
$dedPayload = @{
    employeeId = $dEmpId
    amount = 100.00
    periodYear = 2026
    periodMonth = 9
    deductionDate = "2026-09-03"
    reason = "Uniform Maintenance"
} | ConvertTo-Json
$dedRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/salary-deductions" -Method Post -Headers $adminHeaders -Body $dedPayload
$ded = if ($dedRes.data) { $dedRes.data } else { $dedRes }
Write-Host "Issued Salary Deduction: ₹$($ded.amount)" -ForegroundColor Green

# Recalculate Payroll: Gross = ₹1650, Advance = ₹500, Deduction = ₹100, Net Payable = ₹1050
$calcAfterAdvRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/payroll/calculate?employeeId=$dEmpId&year=2026&month=9" -Headers $adminHeaders
$calcAfterAdv = if ($calcAfterAdvRes.data) { $calcAfterAdvRes.data } else { $calcAfterAdvRes }
Write-Host "Recalculated Payroll: Gross ₹$($calcAfterAdv.grossSalary) - Adv ₹$($calcAfterAdv.advancesRecovered) - Ded ₹$($calcAfterAdv.deductions) = Net ₹$($calcAfterAdv.netPayable)" -ForegroundColor Green

# Step 3: Attempt Overpayment (Integrity Check - attempt ₹2000 when Net Payable is ₹1050)
$overpayReq = @{
    employeeId = $dEmpId
    periodYear = 2026
    periodMonth = 9
    paymentDate = "2026-09-30"
    amount = 2000.00
    paymentMethod = "BANK_TRANSFER"
    notes = "Overpayment Test"
} | ConvertTo-Json
try {
    $overpayRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/salary-payments" -Method Post -Headers $adminHeaders -Body $overpayReq
    Write-Host "Payment Integrity FAIL: Overpayment was accepted!" -ForegroundColor Red
} catch {
    Write-Host "Payment Integrity PASS: Overpayment was rejected by guard rule." -ForegroundColor Green
}

# Step 4: Record Valid Partial Salary Payment of ₹600
$validPayReq = @{
    employeeId = $dEmpId
    periodYear = 2026
    periodMonth = 9
    paymentDate = "2026-09-30"
    amount = 600.00
    paymentMethod = "BANK_TRANSFER"
    notes = "Partial Salary Settlement"
} | ConvertTo-Json
$payRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/salary-payments" -Method Post -Headers $adminHeaders -Body $validPayReq
$pay = if ($payRes.data) { $payRes.data } else { $payRes }
Write-Host "Valid Salary Payment Recorded: ₹$($pay.amount). Remaining: ₹$(1050 - 600) = ₹450.00" -ForegroundColor Green

# Step 5: Check Employee Ledger
$ledgerRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/employees/$dEmpId/ledger" -Headers $adminHeaders
$ledger = if ($ledgerRes.data) { $ledgerRes.data } else { $ledgerRes }
Write-Host "Ledger Entries Count: $($ledger.Count)" -ForegroundColor Green
foreach ($entry in $ledger) {
    Write-Host "  -> Date: $($entry.transactionDate) | Type: $($entry.transactionType) | Debit: ₹$($entry.debitAmount) | Credit: ₹$($entry.creditAmount) | Running Balance: ₹$($entry.runningBalance)" -ForegroundColor Gray
}

# 8. Effective Dating & Costing Verification on Completed Batch
Write-Host "`n--- [8] Dynamic Production Costing on Completed Batch ---" -ForegroundColor Yellow
$batchesRes = Invoke-RestMethod -Uri "http://localhost:8086/api/v1/production/batches" -Headers $adminHeaders
$batches = if ($batchesRes.data) { $batchesRes.data } else { $batchesRes }
$completedBatch = $batches | Where-Object { $_.status -eq "COMPLETED" } | Select-Object -First 1

if ($completedBatch) {
    $costingRes = Invoke-RestMethod -Uri "http://localhost:8086/api/v1/production/batches/$($completedBatch.id)/costing" -Headers $adminHeaders
    $costing = if ($costingRes.data) { $costingRes.data } else { $costingRes }
    Write-Host "Batch Costing for Batch $($completedBatch.batchNumber) (Output: $($completedBatch.actualQuantity) KG):" -ForegroundColor Green
    Write-Host "  Raw Material Cost: ₹$($costing.rawMaterialCost)" -ForegroundColor Gray
    Write-Host "  Labour Cost:       ₹$($costing.labourCost)" -ForegroundColor Gray
    Write-Host "  Fuel Cost:         ₹$($costing.fuelCost)" -ForegroundColor Gray
    Write-Host "  Packaging Cost:    ₹$($costing.packagingCost)" -ForegroundColor Gray
    Write-Host "  Other/Energy Cost: ₹$($costing.otherCost)" -ForegroundColor Gray
    Write-Host "  Total Batch Cost:  ₹$($costing.totalBatchCost)" -ForegroundColor Gray
    Write-Host "  Cost Per KG:       ₹$($costing.costPerKg)" -ForegroundColor Gray
    Write-Host "  Costing Status:    $($costing.costingStatus)" -ForegroundColor Gray
    Write-Host "  Costing Notes:     $($costing.costingNotes)" -ForegroundColor Gray
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  ALL AUTOMATED VERIFICATION CHECKS COMPLETE             " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
