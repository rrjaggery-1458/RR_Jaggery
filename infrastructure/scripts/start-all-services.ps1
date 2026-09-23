# Script to start all 8 Spring Boot services with lightweight memory footprint
$services = @(
    @{ Name = "auth-service"; Jar = "services\auth-service\target\auth-service-1.0.0-SNAPSHOT.jar"; Port = 8081; Url = "http://localhost:8081/api/v1/auth/health" },
    @{ Name = "commerce-service"; Jar = "services\commerce-service\target\commerce-service-1.0.0-SNAPSHOT.jar"; Port = 8082; Url = "http://localhost:8082/api/v1/commerce/health" },
    @{ Name = "customer-ledger-service"; Jar = "services\customer-ledger-service\target\customer-ledger-service-1.0.0-SNAPSHOT.jar"; Port = 8083; Url = "http://localhost:8083/api/v1/customers/health" },
    @{ Name = "inventory-service"; Jar = "services\inventory-service\target\inventory-service-1.0.0-SNAPSHOT.jar"; Port = 8084; Url = "http://localhost:8084/api/v1/inventory/health" },
    @{ Name = "procurement-service"; Jar = "services\procurement-service\target\procurement-service-1.0.0-SNAPSHOT.jar"; Port = 8085; Url = "http://localhost:8085/api/v1/procurement/health" },
    @{ Name = "production-service"; Jar = "services\production-service\target\production-service-1.0.0-SNAPSHOT.jar"; Port = 8086; Url = "http://localhost:8086/api/v1/production/health" },
    @{ Name = "finance-service"; Jar = "services\finance-service\target\finance-service-1.0.0-SNAPSHOT.jar"; Port = 8087; Url = "http://localhost:8087/api/v1/finance/health" },
    @{ Name = "notification-service"; Jar = "services\notification-service\target\notification-service-1.0.0-SNAPSHOT.jar"; Port = 8088; Url = "http://localhost:8088/api/v1/notifications/health" }
)

Write-Output "=== LAUNCHING 8 SPRING BOOT MICROSERVICES ==="
$pids = @()

foreach ($svc in $services) {
    if (Test-Path $svc.Jar) {
        $alreadyUp = $false
        try {
            $test = Invoke-RestMethod -Uri $svc.Url -TimeoutSec 1 -ErrorAction SilentlyContinue
            if ($test.success -eq $true) {
                $alreadyUp = $true
                Write-Output " [ALREADY RUNNING] $($svc.Name) on port $($svc.Port)"
            }
        } catch {}

        if (-not $alreadyUp) {
            $jvmArgs = @("-Xms64m", "-Xmx128m", "-XX:TieredStopAtLevel=1", "-jar", $svc.Jar)
            $p = Start-Process java -ArgumentList $jvmArgs -PassThru -WindowStyle Hidden
            $pids += "$($svc.Name):$($p.Id)"
            Write-Output " [LAUNCHED] $($svc.Name) (PID: $($p.Id)) on port $($svc.Port)"
            Start-Sleep -Milliseconds 800
        }
    } else {
        Write-Error "Jar not found: $($svc.Jar)"
    }
}

if ($pids.Count -gt 0) {
    $pids | Out-File -FilePath "services-pids.txt" -Append -Encoding utf8
}

Write-Output "Waiting for microservices to initialize..."
Start-Sleep -Seconds 10
