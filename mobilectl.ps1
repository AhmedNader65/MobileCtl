#!/usr/bin/env pwsh

# Get the directory where this script is located
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $scriptDir "cli/build/libs/mobilectl.jar"

# Check if JAR exists
if (-Not (Test-Path $jarPath)) {
    Write-Host "JAR not found at $jarPath"
    Write-Host "Building mobilectl..."
    Push-Location $scriptDir
    & .\gradlew.bat cli:build
    Pop-Location

    if (-Not (Test-Path $jarPath)) {
        Write-Host "Build failed"
        exit 1
    }
}

# Run mobilectl
java -jar $jarPath @args
