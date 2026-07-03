Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   Stopping EAI Microservices (Windows)      " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# Find all java.exe processes running our jars or telemetry agent
$processes = Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Where-Object {
    $_.CommandLine -like "*api-gateway*" -or
    $_.CommandLine -like "*auth-service*" -or
    $_.CommandLine -like "*product-service*" -or
    $_.CommandLine -like "*customer-service*" -or
    $_.CommandLine -like "*order-service*" -or
    $_.CommandLine -like "*inventory-service*" -or
    $_.CommandLine -like "*shipping-service*" -or
    $_.CommandLine -like "*payment-service*" -or
    $_.CommandLine -like "*opentelemetry-javaagent*"
}

if ($processes) {
    foreach ($p in $processes) {
        Write-Host "Stopping PID $($p.ProcessId): $($p.CommandLine -replace ' -','`n -')" -ForegroundColor Yellow
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "`nAll microservices stopped successfully." -ForegroundColor Green
} else {
    Write-Host "`nNo running microservices found." -ForegroundColor Gray
}
