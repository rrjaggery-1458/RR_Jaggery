# Script to stop all running Spring Boot services from services-pids.txt
if (Test-Path "services-pids.txt") {
    $lines = Get-Content "services-pids.txt"
    foreach ($line in $lines) {
        if ($line.Trim()) {
            $parts = $line.Split(":")
            $name = $parts[0]
            $procId = [int]$parts[1]
            try {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Write-Output "Stopped $name (PID: $procId)"
            } catch {
                Write-Output "Process $procId already terminated"
            }
        }
    }
    Remove-Item "services-pids.txt" -Force -ErrorAction SilentlyContinue
} else {
    Write-Output "No services-pids.txt found."
}
