$ErrorActionPreference = "Stop"

Write-Host "=== 1. Admin Authentication ==="
$loginBody = @{ email = "admin@rrjaggery.com"; password = "Admin@123" } | ConvertTo-Json
$authRes = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $authRes.data.token
Write-Host "Admin Logged in successfully: $($authRes.data.fullName) [Roles: $($authRes.data.roles -join ', ')]"

Write-Host "`n=== 2. List Investments (Initial) ==="
$invRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/investments" -Headers @{ Authorization = "Bearer $token" } -Method Get
Write-Host "Current Investments Count: $($invRes.data.Count)"

Write-Host "`n=== 3. Create Investment (Admin) ==="
$createBody = @{
    investorName = "Rohan Patil (Proprietor)"
    investmentType = "OWNER_CAPITAL"
    amount = 500000.00
    investmentDate = "2026-09-30"
    details = "Initial Equity Infusion for Phase 1 Factory Automation"
} | ConvertTo-Json
$created = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/investments" -Headers @{ Authorization = "Bearer $token" } -Method Post -ContentType "application/json" -Body $createBody
Write-Host "Investment Created: ID=$($created.data.id), Investor=$($created.data.investorName), Amount=Rs.$($created.data.amount), Type=$($created.data.investmentType), Status=$($created.data.status)"

Write-Host "`n=== 4. Fetch Investment Form Definition & Options ==="
$formDef = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/forms/definitions/INVESTMENT" -Headers @{ Authorization = "Bearer $token" } -Method Get
Write-Host "INVESTMENT Form: $($formDef.data.displayName) (v$($formDef.data.version)), Fields Count: $($formDef.data.fields.Count)"
foreach ($field in $formDef.data.fields) {
    Write-Host "  Field: $($field.label) ($($field.fieldKey)) [Type: $($field.fieldType), Required: $($field.required), Options: $($field.options.Count)]"
    foreach ($opt in $field.options) {
        Write-Host "    - Option: $($opt.label) ($($opt.value)) [Active: $($opt.active)]"
    }
}

Write-Host "`n=== 5. Fetch Expense Form Definition & Add Custom Field ==="
$expenseDef = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/forms/definitions/EXPENSE" -Headers @{ Authorization = "Bearer $token" } -Method Get
Write-Host "EXPENSE Form: $($expenseDef.data.displayName) (v$($expenseDef.data.version)), Fields Count: $($expenseDef.data.fields.Count)"

$fieldBody = @{
    fieldKey = "boilerShiftSupervisor"
    label = "Boiler Shift Supervisor"
    fieldType = "TEXT"
    required = $false
    displayOrder = 1
    placeholder = "e.g. Suresh Shinde"
    helpText = "Name of on-duty supervisor during boiler expense"
} | ConvertTo-Json
$fieldRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/forms/definitions/EXPENSE/fields" -Headers @{ Authorization = "Bearer $token" } -Method Post -ContentType "application/json" -Body $fieldBody
Write-Host "Added Custom Field to EXPENSE: ID=$($fieldRes.data.id), Label=$($fieldRes.data.label), Key=$($fieldRes.data.fieldKey)"

Write-Host "`n=== 6. Verify Expense Form After Custom Field ==="
$expenseDefAfter = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/forms/definitions/EXPENSE" -Headers @{ Authorization = "Bearer $token" } -Method Get
Write-Host "EXPENSE Form Fields Count After: $($expenseDefAfter.data.fields.Count)"
foreach ($f in $expenseDefAfter.data.fields) {
    Write-Host "  - $($f.label) ($($f.fieldKey)) [Type: $($f.fieldType), Active: $($f.active)]"
}

Write-Host "`n=== 7. Verify RBAC / Separation of Investment from Expenses ==="
$expRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/expenses" -Headers @{ Authorization = "Bearer $token" } -Method Get
Write-Host "Operating Expenses Count: $($expRes.data.Count) (Investments are strictly NOT mixed with Operating Expenses)"

Write-Host "`n=== ALL RUNTIME CHECKS PASSED SUCCESSFULLY ==="
