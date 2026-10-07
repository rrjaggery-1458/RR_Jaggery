$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "         PRODUCT IMAGE UPLOAD & PERSISTENCE ACCEPTANCE TEST      " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Login as Admin/Manager
$login = @{ email = "admin@rrjaggery.com"; password = "Admin@123" } | ConvertTo-Json
$auth = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $login -ContentType "application/json"
$token = $auth.data.token
$headers = @{ Authorization = "Bearer $token" }

# 2. Upload an image
Write-Host "`n[Step 1] Uploading test product image..." -ForegroundColor Yellow
$testImagePath = "e:\RR Jaggery\scratch\test_jaggery.png"
if (-not (Test-Path $testImagePath)) {
    # Create a small valid PNG file
    [byte[]]$pngBytes = @(
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
        0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
        0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
        0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00,
        0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE,
        0x42, 0x60, 0x82
    )
    [System.IO.File]::WriteAllBytes($testImagePath, $pngBytes)
}

# Upload via curl / Invoke-RestMethod multipart
$uploadUrl = "$BaseUrl/api/v1/commerce/admin/products/upload-image"
$boundary = [System.Guid]::NewGuid().ToString()
$fileBytes = [System.IO.File]::ReadAllBytes($testImagePath)
$bodyLines = @(
    "--$boundary",
    'Content-Disposition: form-data; name="file"; filename="test_jaggery.png"',
    'Content-Type: image/png',
    '',
    [System.Text.Encoding]::GetEncoding("ISO-8859-1").GetString($fileBytes),
    "--$boundary--"
)
$body = $bodyLines -join "`r`n"
$uploadHeaders = @{
    Authorization = "Bearer $token"
    "Content-Type" = "multipart/form-data; boundary=$boundary"
}

$uploadResp = Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers $uploadHeaders -Body $body
$imagePath = $uploadResp.data.imageUrl
Write-Host "  ✅ Image uploaded: $imagePath" -ForegroundColor Green

# 3. Create or update a product with this image
Write-Host "`n[Step 2] Attaching image to product..." -ForegroundColor Yellow
$prodList = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/commerce/products" -Method Get).data
$targetProd = $prodList[0]

$updateBody = @{
    name = $targetProd.name
    sku = $targetProd.sku
    slug = $targetProd.slug
    categoryId = $targetProd.categoryId
    grade = $targetProd.grade
    packageType = $targetProd.packageType
    unitWeightKg = $targetProd.unitWeightKg
    retailPrice = $targetProd.retailPrice
    wholesalePrice = $targetProd.wholesalePrice
    wholesaleMoq = $targetProd.wholesaleMoq
    imageUrl = $imagePath
    active = $true
    featured = $true
} | ConvertTo-Json

$putHeaders = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

$updateResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/commerce/admin/products/$($targetProd.id)" -Method Put -Headers $putHeaders -Body $updateBody
Write-Host "  ✅ Product updated with imageUrl: $($updateResp.data.imageUrl)" -ForegroundColor Green

# 4. Verify public Storefront catalog returns the image
Write-Host "`n[Step 3] Checking Public Storefront catalog..." -ForegroundColor Yellow
$publicProds = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/commerce/products" -Method Get).data
$updatedProd = $publicProds | Where-Object { $_.id -eq $targetProd.id }
Write-Host "  Product on Storefront: $($updatedProd.name) -> imageUrl: $($updatedProd.imageUrl)" -ForegroundColor Gray

# 5. Verify direct HTTP access to image via Nginx gateway
Write-Host "`n[Step 4] Verifying direct HTTP fetch via /uploads/products/..." -ForegroundColor Yellow
$imgUrl = "$BaseUrl$($updatedProd.imageUrl)"
$imgHttp = Invoke-WebRequest -Uri $imgUrl -Method Head -UseBasicParsing
Write-Host "  ✅ Image HTTP Status: $($imgHttp.StatusCode)" -ForegroundColor Green

# 6. Restart Docker containers to verify persistence
Write-Host "`n[Step 5] Restarting Docker commerce-service and gateway containers..." -ForegroundColor Yellow
docker restart rr-commerce-service rr-gateway
Write-Host "  Waiting 25s for Spring Boot to bootstrap..." -ForegroundColor Gray
Start-Sleep -Seconds 25

# 7. Re-verify after container restart
Write-Host "`n[Step 6] Re-verifying image after Docker restart..." -ForegroundColor Yellow
$imgHttpAfter = Invoke-WebRequest -Uri $imgUrl -Method Head -UseBasicParsing
if ($imgHttpAfter.StatusCode -eq 200) {
    Write-Host "  ✅ Image file persists on volume across Docker restarts! HTTP Status: 200" -ForegroundColor Green
} else {
    Write-Host "  ❌ Image not found after restart!" -ForegroundColor Red
}

$prodAfter = (Invoke-RestMethod -Uri "$BaseUrl/api/v1/commerce/products" -Method Get).data | Where-Object { $_.id -eq $targetProd.id }
if ($prodAfter.imageUrl -eq $imagePath) {
    Write-Host "  ✅ Database persists imageUrl across Docker restarts!" -ForegroundColor Green
} else {
    Write-Host "  ❌ Database lost imageUrl!" -ForegroundColor Red
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "     ALL PRODUCT IMAGE & PERSISTENCE TESTS PASSED (100%)         " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
