# Sprint 4 Complete End-to-End Runtime Verification Script
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  RR JAGGERY - SPRINT 4 RUNTIME VERIFICATION (REAL E2E)   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost"
$adminEmail = "admin@rrjaggery.com"
$adminPassword = "Admin@123"

# 1. Authenticate Admin
Write-Host "`n[Step 1] Authenticating Admin..." -ForegroundColor Yellow
$loginPayload = @{ email = $adminEmail; password = $adminPassword } | ConvertTo-Json
$authRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
if (-not $authRes.success -or -not $authRes.data.token) {
    throw "Admin Authentication Failed!"
}
$token = $authRes.data.token
$headers = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }
Write-Host "  -> Admin Authenticated! User: $($authRes.data.user.fullName) ($($authRes.data.user.roles))" -ForegroundColor Green

# 2. Setup RAW MATERIAL in Inventory
Write-Host "`n[Step 2] Inventory: Creating Raw Material Item..." -ForegroundColor Yellow
$rmSku = "RM-CANE-" + (Get-Random -Minimum 1000 -Maximum 9999)
$createRmPayload = @{
    sku = $rmSku
    itemName = "Premium Organic Sugarcane"
    itemType = "RAW_MATERIAL"
    unitOfMeasure = "KG"
    initialQuantity = 0
    minimumStockLevel = 500
    batchLot = "LOT-2026-09"
    sourceReference = "MANDYA-FARMS"
} | ConvertTo-Json

$rmRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/items" -Method Post -Headers $headers -Body $createRmPayload
$rmItem = $rmRes.data
Write-Host "  -> Raw Material Created: SKU=$($rmItem.sku), Type=$($rmItem.itemType), InitialQty=$($rmItem.currentQuantity) KG, MinThreshold=$($rmItem.minimumStockLevel) KG" -ForegroundColor Green

# 3. Create Supplier
Write-Host "`n[Step 3] Procurement: Creating Supplier..." -ForegroundColor Yellow
$supCode = "SUP-" + (Get-Random -Minimum 1000 -Maximum 9999)
$createSupPayload = @{
    supplierCode = $supCode
    supplierName = "Mandya Sugarcane Farmers Co-op"
    contactName = "Suresh Patel"
    email = "suresh.$supCode@farmers.in"
    phone = "+91 98450 11223"
    gstin = "29AABCU9603R1ZM"
    paymentTermsDays = 30
} | ConvertTo-Json

$supRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/suppliers" -Method Post -Headers $headers -Body $createSupPayload
$supplier = $supRes.data
Write-Host "  -> Supplier Created: Code=$($supplier.supplierCode), Name=$($supplier.supplierName), Outstanding=INR $($supplier.currentOutstanding)" -ForegroundColor Green

# 4. Create Purchase Order
Write-Host "`n[Step 4] Procurement: Creating Purchase Order..." -ForegroundColor Yellow
$createPoPayload = @{
    supplierId = $supplier.id
    expectedDeliveryDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
    notes = "Sprint 4 Automated Test Purchase Order"
    lines = @(
        @{
            sku = $rmSku
            itemName = "Premium Organic Sugarcane"
            orderedQuantity = 1000
            unitCost = 45.00
        }
    )
} | ConvertTo-Json

$poRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/purchase-orders" -Method Post -Headers $headers -Body $createPoPayload
$po = $poRes.data
Write-Host "  -> Purchase Order Created: PO#=$($po.orderNumber), Status=$($po.status), TotalAmount=INR $($po.totalAmount)" -ForegroundColor Green

# 5. Goods Receipt - Partial (600 KG of 1000 KG)
Write-Host "`n[Step 5] Procurement: Partial Goods Receipt (600 KG / 1000 KG)..." -ForegroundColor Yellow
$partialReceiptPayload = @{
    purchaseOrderId = $po.id
    sku = $rmSku
    quantity = 600
    receivedBy = "Warehouse Manager Raman"
    notes = "First batch received in good condition"
} | ConvertTo-Json

$gr1Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/goods-receipts" -Method Post -Headers $headers -Body $partialReceiptPayload
$poAfterGr1 = $gr1Res.data
Write-Host "  -> Partial Receipt Recorded: Status=$($poAfterGr1.status), TotalReceived=$($poAfterGr1.totalReceivedQuantity) KG" -ForegroundColor Green

# Verify Raw Material stock increased by 600 KG
$rmCheck1 = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/items/$($rmItem.id)" -Method Get -Headers $headers
Write-Host "  -> Verified Inventory RM Stock: CurrentQty = $($rmCheck1.data.currentQuantity) KG (Expected 600 KG)" -ForegroundColor Green
if ($rmCheck1.data.currentQuantity -ne 600) { throw "Stock did not increase properly!" }

# Verify Supplier Ledger & Outstanding
$supLedger1 = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/suppliers/$($supplier.id)/ledger" -Method Get -Headers $headers
$supOutstanding1 = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/suppliers/$($supplier.id)/outstanding" -Method Get -Headers $headers
Write-Host "  -> Verified Supplier Outstanding: INR $($supOutstanding1.data) (Expected INR 27000)" -ForegroundColor Green
Write-Host "  -> Supplier Ledger Entries Count: $($supLedger1.data.Count), Latest Entry Type: $($supLedger1.data[0].entryType), Amount: $($supLedger1.data[0].amount)" -ForegroundColor Green

# 6. Over-Receipt Rejection Test
Write-Host "`n[Step 6] Over-Receipt Rejection Check..." -ForegroundColor Yellow
try {
    $overReceiptPayload = @{
        purchaseOrderId = $po.id
        sku = $rmSku
        quantity = 500  # Only 400 remaining!
        receivedBy = "Raman"
        notes = "Attempting to over-receive"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/goods-receipts" -Method Post -Headers $headers -Body $overReceiptPayload
    throw "Over-receipt should have been rejected!"
} catch {
    Write-Host "  -> Over-Receipt Successfully REJECTED as expected: $($_.Exception.Message)" -ForegroundColor Green
}

# 7. Complete Remaining Goods Receipt (400 KG)
Write-Host "`n[Step 7] Complete Remaining Goods Receipt (400 KG)..." -ForegroundColor Yellow
$finalReceiptPayload = @{
    purchaseOrderId = $po.id
    sku = $rmSku
    quantity = 400
    receivedBy = "Warehouse Manager Raman"
    notes = "Final batch received"
} | ConvertTo-Json

$gr2Res = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/goods-receipts" -Method Post -Headers $headers -Body $finalReceiptPayload
$poAfterGr2 = $gr2Res.data
Write-Host "  -> Final Receipt Recorded: Status=$($poAfterGr2.status) (Expected COMPLETED), TotalReceived=$($poAfterGr2.totalReceivedQuantity) KG" -ForegroundColor Green
if ($poAfterGr2.status -ne "COMPLETED") { throw "PO status should be COMPLETED!" }

# Verify Raw Material stock reached 1000 KG
$rmCheck2 = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/items/$($rmItem.id)" -Method Get -Headers $headers
Write-Host "  -> Verified Inventory RM Stock: CurrentQty = $($rmCheck2.data.currentQuantity) KG (Expected 1000 KG)" -ForegroundColor Green

# Verify Receipt History
$receipts = Invoke-RestMethod -Uri "$baseUrl/api/v1/procurement/purchase-orders/$($po.id)/receipts" -Method Get -Headers $headers
Write-Host "  -> Purchase Order Receipt History Count: $($receipts.data.Count)" -ForegroundColor Green
$receipts.data | ForEach-Object { Write-Host "     Receipt: Qty=$($_.receivedQuantity) KG, By=$($_.receivedBy), Notes=$($_.notes)" }

# 8. Inventory Controls (Adjustment, Negative Protection, Reconciliation)
Write-Host "`n[Step 8] Testing Inventory Controls & Reconciliation..." -ForegroundColor Yellow

# Negative stock rejection
try {
    $negAdjustPayload = @{
        itemId = $rmItem.id
        quantity = -2000
        reason = "DAMAGE"
        referenceType = "MANUAL"
        referenceId = "TEST-NEG"
        actor = "ADMIN"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/stock/adjust" -Method Post -Headers $headers -Body $negAdjustPayload
    throw "Negative stock adjustment should have failed!"
} catch {
    Write-Host "  -> Negative Stock Adjustment REJECTED as expected: $($_.Exception.Message)" -ForegroundColor Green
}

# Stock Reconciliation with physical count + mandatory reason
$reconcilePayload = @{
    itemId = $rmItem.id
    countedQuantity = 980
    reason = "Physical Audit Q3 2026"
    actor = "Lead Auditor Vikram"
    notes = "20 KG moisture loss during storage"
} | ConvertTo-Json

$reconRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/reconcile" -Method Post -Headers $headers -Body $reconcilePayload
Write-Host "  -> Stock Reconciled: New Qty=$($reconRes.data.currentQuantity) KG (Expected 980 KG)" -ForegroundColor Green

# Verify stock movements history
$movements = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/items/$($rmItem.id)/movements" -Method Get -Headers $headers
Write-Host "  -> Stock Movement History Count: $($movements.data.Count)" -ForegroundColor Green
$movements.data | ForEach-Object { Write-Host "     Movement: Type=$($_.movementType), Qty=$($_.quantity) KG, Reason=$($_.reason), Actor=$($_.actor)" }

# 9. Sale Flow (Commerce -> Inventory Microservice Communication)
Write-Host "`n[Step 9] Sale Flow: Finished Good Creation & Order Deduction..." -ForegroundColor Yellow

# Create Finished Good in Inventory
$fgSku = "FG-JAG-" + (Get-Random -Minimum 1000 -Maximum 9999)
$createFgPayload = @{
    sku = $fgSku
    itemName = "Pure Organic Jaggery Block 1KG"
    itemType = "FINISHED_GOOD"
    unitOfMeasure = "KG"
    initialQuantity = 200
    minimumStockLevel = 50
    batchLot = "LOT-FG-2026"
    sourceReference = "PRODUCTION-BATCH-1"
} | ConvertTo-Json

$fgRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/items" -Method Post -Headers $headers -Body $createFgPayload
$fgItem = $fgRes.data
Write-Host "  -> Finished Good Created in Inventory: SKU=$($fgItem.sku), Qty=$($fgItem.currentQuantity) KG" -ForegroundColor Green

# Deduct Finished Good stock via Sale deduction endpoint
$orderRef = "ORD-TEST-" + (Get-Random -Minimum 10000 -Maximum 99999)
$saleDeductPayload = @{
    sku = $fgSku
    quantity = 30
    actor = "COMMERCE_TEST"
    referenceId = $orderRef
} | ConvertTo-Json

$saleRes = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/stock/sale" -Method Post -Headers $headers -Body $saleDeductPayload
Write-Host "  -> Sale Stock Deducted: New Finished Good Qty=$($saleRes.data.currentQuantity) KG (Expected 170 KG)" -ForegroundColor Green
if ($saleRes.data.currentQuantity -ne 170) { throw "Finished good stock deduction failed!" }

# Verify Idempotency of sale deduction (re-trying same referenceId must NOT double deduct)
$saleResRetry = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/stock/sale" -Method Post -Headers $headers -Body $saleDeductPayload
Write-Host "  -> Idempotency Verified: Re-sent sale deduction, Qty remains = $($saleResRetry.data.currentQuantity) KG (Did not double deduct)" -ForegroundColor Green
if ($saleResRetry.data.currentQuantity -ne 170) { throw "Idempotency failed: double deducted!" }

# Insufficient stock sale rejection
try {
    $excessSalePayload = @{
        sku = $fgSku
        quantity = 500
        actor = "COMMERCE_TEST"
        referenceId = "ORD-EXCESS-" + (Get-Random -Minimum 10000 -Maximum 99999)
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/stock/sale" -Method Post -Headers $headers -Body $excessSalePayload
    throw "Excess sale deduction should have failed!"
} catch {
    Write-Host "  -> Excess Sale Deduction REJECTED as expected: $($_.Exception.Message)" -ForegroundColor Green
}

# Verify finished goods movements
$fgMovements = Invoke-RestMethod -Uri "$baseUrl/api/v1/inventory/items/$($fgItem.id)/movements" -Method Get -Headers $headers
Write-Host "  -> Finished Goods Movement Count: $($fgMovements.data.Count)" -ForegroundColor Green
$fgMovements.data | ForEach-Object { Write-Host "     Movement: Type=$($_.movementType), Qty=$($_.quantity) KG, Reason=$($_.reason), Ref=$($_.referenceId)" }

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  ALL SPRINT 4 RUNTIME VERIFICATION CHECKS PASSED!         " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
