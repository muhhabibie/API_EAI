param(
    [switch]$WithTelemetry
)

$services = @(
    @{ Name = "API Gateway"; Port = 8080; Jar = "api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar"; ServiceName = "api-gateway" },
    @{ Name = "Auth Service"; Port = 8081; Jar = "auth-service/target/auth-service-0.0.1-SNAPSHOT.jar"; ServiceName = "auth-service" },
    @{ Name = "Product Service"; Port = 8082; Jar = "product-service/target/product-service-0.0.1-SNAPSHOT.jar"; ServiceName = "product-service" },
    @{ Name = "Customer Service"; Port = 8083; Jar = "customer-service/target/customer-service-0.0.1-SNAPSHOT.jar"; ServiceName = "customer-service" },
    @{ Name = "Order Service"; Port = 8084; Jar = "order-service/target/order-service-0.0.1-SNAPSHOT.jar"; ServiceName = "order-service" },
    @{ Name = "Inventory Service"; Port = 8085; Jar = "inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar"; ServiceName = "inventory-service" },
    @{ Name = "Shipping Service"; Port = 8086; Jar = "shipping-service/target/shipping-service-0.0.1-SNAPSHOT.jar"; ServiceName = "shipping-service" },
    @{ Name = "Payment Service"; Port = 8087; Jar = "payment-service/target/payment-service-0.0.1-SNAPSHOT.jar"; ServiceName = "payment-service" }
)

# Ensure logs dir exists
if (!(Test-Path -Path "logs")) {
    New-Item -ItemType Directory -Path "logs" | Out-Null
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   Starting EAI Microservices (Windows)      " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Mode: $(if ($WithTelemetry) { 'WITH OpenTelemetry Monitoring' } else { 'OPTIMIZED (Fast Startup, Low Memory)' })`n" -ForegroundColor White

foreach ($s in $services) {
    # Check if port is already active (listening state)
    $portActive = Get-NetTCPConnection -LocalPort $s.Port -State Listen -ErrorAction SilentlyContinue

    if ($portActive) {
        Write-Host "⚠️  Service $($s.Name) port $($s.Port) is already in use! Skipping..." -ForegroundColor Yellow
        continue
    }

    Write-Host "Starting $($s.Name) on port $($s.Port)..." -ForegroundColor Green

    # Base JVM arguments for fast startup & low memory
    $jvmArgs = @("-Xmx256m", "-XX:TieredStopAtLevel=1")

    # If telemetry is enabled, add OTel agent
    if ($WithTelemetry) {
        if (Test-Path -Path "opentelemetry-javaagent.jar") {
            $jvmArgs += "-javaagent:opentelemetry-javaagent.jar"
            $jvmArgs += "-Dotel.service.name=$($s.ServiceName)"
            $jvmArgs += "-Dotel.exporter.otlp.endpoint=http://localhost:4317"
            $jvmArgs += "-Dotel.exporter.otlp.protocol=grpc"
            $jvmArgs += "-Dotel.logs.exporter=none"
        } else {
            Write-Host "⚠️  opentelemetry-javaagent.jar not found, running without telemetry." -ForegroundColor Yellow
        }
    }

    $jvmArgs += "-jar"
    $jvmArgs += $s.Jar

    # Log file locations
    $logOut = "logs/$($s.ServiceName).log"
    $logErr = "logs/$($s.ServiceName)-error.log"
    
    # Truncate existing log files
    Clear-Content -Path $logOut -ErrorAction SilentlyContinue
    Clear-Content -Path $logErr -ErrorAction SilentlyContinue

    # Start the process in background hidden
    Start-Process -FilePath "java" -ArgumentList $jvmArgs -WindowStyle Hidden -RedirectStandardOutput $logOut -RedirectStandardError $logErr

    # Small delay to reduce CPU spike
    Start-Sleep -Seconds 3
}

Write-Host "`nAll services started in background!" -ForegroundColor Cyan
Write-Host "Logs are located in the './logs/' directory." -ForegroundColor Cyan
Write-Host "Check dashboard at: http://localhost:8080/admin/index.html" -ForegroundColor Green
