#!/bin/bash

# Detect OS
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    echo "Windows environment detected (Git Bash / MSYS2)."
    echo "Delegating termination to PowerShell script..."
    powershell.exe -ExecutionPolicy Bypass -File ./stop_services.ps1
    exit 0
fi

# Native Linux / macOS Termination
echo "============================================="
echo "   Stopping EAI Microservices (Linux/Mac)    "
echo "============================================="
echo "Stopping all microservices..."

# Kill java processes running the snapshots or the OTel agent
pkill -f "SNAPSHOT.jar" || pkill -f "opentelemetry-javaagent" || echo "No running microservices found."

echo "All microservices stopped."
