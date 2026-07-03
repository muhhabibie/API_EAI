#!/bin/bash

# Detect OS
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    echo "Windows environment detected (Git Bash / MSYS2)."
    echo "Delegating execution to optimized PowerShell script..."
    
    # Check if user requested telemetry
    WITH_TELEMETRY=""
    if [[ "$1" == "--telemetry" || "$1" == "-t" ]]; then
        WITH_TELEMETRY="-WithTelemetry"
    fi
    
    powershell.exe -ExecutionPolicy Bypass -File ./start_services.ps1 $WITH_TELEMETRY
    exit 0
fi

# Native Linux / macOS Startup
mkdir -p logs

# JVM optimizations for local development
JVM_OPTS="-Xmx256m -XX:TieredStopAtLevel=1"

# Check if telemetry option is set
USE_TELEMETRY=false
if [[ "$1" == "--telemetry" || "$1" == "-t" ]]; then
    USE_TELEMETRY=true
fi

# Array of services (name port jar)
services=(
    "api-gateway 8080 api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar"
    "auth-service 8081 auth-service/target/auth-service-0.0.1-SNAPSHOT.jar"
    "product-service 8082 product-service/target/product-service-0.0.1-SNAPSHOT.jar"
    "customer-service 8083 customer-service/target/customer-service-0.0.1-SNAPSHOT.jar"
    "order-service 8084 order-service/target/order-service-0.0.1-SNAPSHOT.jar"
    "inventory-service 8085 inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar"
    "shipping-service 8086 shipping-service/target/shipping-service-0.0.1-SNAPSHOT.jar"
    "payment-service 8087 payment-service/target/payment-service-0.0.1-SNAPSHOT.jar"
)

echo "============================================="
echo "   Starting EAI Microservices (Linux/Mac)    "
echo "============================================="
echo "Mode: $([ "$USE_TELEMETRY" = true ] && echo "WITH OpenTelemetry Monitoring" || echo "OPTIMIZED (Fast Startup, Low Memory)")"
echo ""

for item in "${services[@]}"; do
    read -r name port jar <<< "$item"
    
    # Check if port is in use
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo "⚠️  Service $name port $port is already in use! Skipping..."
        continue
    fi
    
    echo "Starting $name on port $port..."
    
    RUN_OPTS="$JVM_OPTS"
    if [ "$USE_TELEMETRY" = true ] && [ -f "opentelemetry-javaagent.jar" ]; then
        RUN_OPTS="$RUN_OPTS -javaagent:opentelemetry-javaagent.jar -Dotel.service.name=$name -Dotel.exporter.otlp.endpoint=http://localhost:4317 -Dotel.exporter.otlp.protocol=grpc -Dotel.logs.exporter=none"
    fi
    
    nohup java $RUN_OPTS -jar $jar > logs/$name.log 2>&1 &
    
    sleep 3
done

echo ""
echo "All services started in background!"
echo "Logs are located in the './logs/' directory."
echo "Check dashboard at: http://localhost:8080/admin/index.html"
