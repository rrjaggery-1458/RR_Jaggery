$adminAuth = @{
    email = "admin@rrjaggery.com"
    password = "Admin@123"
} | ConvertTo-Json

$authRes = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -Body $adminAuth -ContentType "application/json"
$token = $authRes.data.token
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}
Write-Host "Admin Login Token obtained."

# 1. Test Wholesaler Creation
Write-Host "`n--- 1. Testing Wholesaler Creation ---"
$wholesalerPayload = @{
    customerType = "OFFLINE_WHOLESALE"
    businessName = "Test Organic Traders " + (Get-Random -Minimum 1000 -Maximum 9999)
    contactPerson = "Ramesh Kumar"
    phone = "98450" + (Get-Random -Minimum 10000 -Maximum 99999)
    gstin = "29ABCDE" + (Get-Random -Minimum 1000 -Maximum 9999) + "F1Z5"
    paymentTerms = "NET_30"
    creditLimit = 150000
    creditDays = 30
    billingAddress = @{
        street = "12 APMC Market Yard"
        city = "Mandya"
        state = "Karnataka"
        postalCode = "571401"
    }
    shippingAddress = @{
        street = "12 APMC Market Yard"
        city = "Mandya"
        state = "Karnataka"
        postalCode = "571401"
    }
} | ConvertTo-Json

try {
    $custRes = Invoke-RestMethod -Uri "http://localhost:8083/api/v1/customers" -Method Post -Body $wholesalerPayload -Headers $headers
    Write-Host "Wholesaler Created Successfully! ID: $($custRes.data.id), Name: $($custRes.data.businessName)"
} catch {
    Write-Host "Wholesaler creation failed: $($_.Exception.Message)"
}

# 2. Test Production Batch Creation
Write-Host "`n--- 2. Testing Production Batch Creation ---"
$batchPayload = @{
    recipeCode = "REC-CUSTOM"
    targetProductName = "Traditional Organic Jaggery Block 1KG"
    targetProductSku = "JAG-TRAD-1KG"
    plannedQuantity = 250.0
    actualQuantity = 250.0
    wastageQuantity = 5.0
    unitOfMeasure = "KG"
    scheduledDate = "2026-09-28"
    supervisor = "ADMIN"
    status = "COMPLETED"
    notes = "Pre-sprint 8 stabilization test batch"
} | ConvertTo-Json

try {
    $prodRes = Invoke-RestMethod -Uri "http://localhost:8086/api/v1/production/batches" -Method Post -Body $batchPayload -Headers $headers
    Write-Host "Production Batch Created! Batch Number: $($prodRes.data.batchNumber), Status: $($prodRes.data.status), ActualQty: $($prodRes.data.actualQuantity)"
} catch {
    Write-Host "Production Batch creation failed: $($_.Exception.Message)"
}

# 3. Test Business Master: Payment Method Create & Delete/Disable
Write-Host "`n--- 3. Testing Business Master Payment Method ---"
$pmPayload = @{
    code = "TEST_UPI_" + (Get-Random -Minimum 100 -Maximum 999)
    displayName = "Test Temp Payment Method"
    active = $true
    notes = "Temporary test payment method"
} | ConvertTo-Json

try {
    $pmRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/business-masters/payment-methods" -Method Post -Body $pmPayload -Headers $headers
    $pmId = $pmRes.data.id
    Write-Host "Payment Method Created! ID: $pmId, Code: $($pmRes.data.methodCode)"

    # Delete / Toggle Payment Method
    $delRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/business-masters/payment-methods/$pmId" -Method Delete -Headers $headers
    Write-Host "Payment Method Soft-Deleted/Toggled: $($delRes.message)"
} catch {
    Write-Host "Payment method test failed: $($_.Exception.Message)"
}

# 4. Test Attendance Recording
Write-Host "`n--- 4. Testing Attendance Recording ---"
try {
    $empRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/employees" -Method Get -Headers $headers
    if ($empRes.data.Count -gt 0) {
        $firstEmp = $empRes.data[0]
        $attPayload = @{
            employeeId = $firstEmp.id
            attendanceDate = "2026-09-28"
            status = "PRESENT"
            overtimeHours = 2.0
            notes = "Stabilization pass attendance test"
        } | ConvertTo-Json

        $attRes = Invoke-RestMethod -Uri "http://localhost:8087/api/v1/finance/attendance" -Method Post -Body $attPayload -Headers $headers
        Write-Host "Attendance Recorded! Status: $($attRes.data.status), OT: $($attRes.data.overtimeHours) hrs for Employee: $($firstEmp.fullName)"
    } else {
        Write-Host "No employees found to record attendance."
    }
} catch {
    Write-Host "Attendance recording failed: $($_.Exception.Message)"
}

# 5. Test Admin User Access
Write-Host "`n--- 5. Testing Admin User Access ---"
try {
    $usersRes = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/admin/users" -Method Get -Headers $headers
    Write-Host "Users retrieved: $($usersRes.data.Count) users found."
    
    # Create test manager
    $mgrPayload = @{
        email = "manager.test." + (Get-Random -Minimum 1000 -Maximum 9999) + "@rrjaggery.com"
        password = "ManagerPassword123!"
        fullName = "Test Operations Manager"
        phone = "9876543210"
    } | ConvertTo-Json
    $newMgrRes = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/admin/managers" -Method Post -Body $mgrPayload -Headers $headers
    Write-Host "Manager Created! ID: $($newMgrRes.data.id), Email: $($newMgrRes.data.email)"

    # Toggle status
    $toggleRes = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/admin/users/$($newMgrRes.data.id)/toggle-status" -Method Post -Headers $headers
    Write-Host "Manager Status Toggled: Enabled = $($toggleRes.data.enabled)"
} catch {
    Write-Host "User access test failed: $($_.Exception.Message)"
}
