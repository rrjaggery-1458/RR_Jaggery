# Verify all 8 service health endpoints via HTTP REST calls
$endpoints = @(
    @{ Name = "Auth Service"; Url = "http://localhost:8081/api/v1/auth/health"; Port = 8081; ExpectedSchema = "auth_schema" },
    @{ Name = "Commerce Service"; Url = "http://localhost:8082/api/v1/commerce/health"; Port = 8082; ExpectedSchema = "commerce_schema" },
    @{ Name = "Customer & Ledger Service"; Url = "http://localhost:8083/api/v1/customers/health"; Port = 8083; ExpectedSchema = "customer_schema" },
    @{ Name = "Inventory Service"; Url = "http://localhost:8084/api/v1/inventory/health"; Port = 8084; ExpectedSchema = "inventory_schema" },
    @{ Name = "Procurement Service"; Url = "http://localhost:8085/api/v1/procurement/health"; Port = 8085; ExpectedSchema = "procurement_schema" },
    @{ Name = "Production Service"; Url = "http://localhost:8086/api/v1/production/health"; Port = 8086; ExpectedSchema = "production_schema" },
    @{ Name = "Finance Service"; Url = "http://localhost:8087/api/v1/finance/health"; Port = 8087; ExpectedSchema = "finance_schema" },
    @{ Name = "Notification Service"; Url = "http://localhost:8088/api/v1/notifications/health"; Port = 8088; ExpectedSchema = "notification_schema" }
)

Write-Output "=== TESTING 8 SPRING BOOT HEALTH ENDPOINTS ==="
$passed = 0
$failed = 0

foreach ($ep in $endpoints) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-RestMethod -Uri $ep.Url -Method Get -TimeoutSec 5 -ErrorAction Stop
        $sw.Stop()
        $latency = $sw.ElapsedMilliseconds
        if ($response.success -eq $true -and $response.data.status -eq "UP") {
            Write-Output " [PASS] $($ep.Name) (: $($ep.Port)) -> Status: $($response.data.status), Schema: $($response.data.schema), Latency: $($latency)ms"
            $passed++
        } else {
            Write-Output " [FAIL] $($ep.Name) (: $($ep.Port)) -> Unexpected payload: $($response | ConvertTo-Json -Compress)"
            $failed++
        }
    } catch {
        $sw.Stop()
        Write-Output " [FAIL] $($ep.Name) (: $($ep.Port)) -> Request failed: $($_.Exception.Message)"
        $failed++
    }
}

Write-Output "==============================================="
Write-Output "Summary: Passed: $passed / $($endpoints.Count), Failed: $failed"

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}
