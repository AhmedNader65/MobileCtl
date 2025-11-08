# mobilectl Windows Installation Script
# Usage: irm https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.ps1 | iex

$ErrorActionPreference = "Stop"

$REPO = "AhmedNader65/MobileCtl"
$INSTALL_DIR = "$env:USERPROFILE\.mobilectl"
$BIN_DIR = "$INSTALL_DIR\bin"

Write-Host "📦 Installing mobilectl..." -ForegroundColor Cyan

# Detect architecture
$ARCH = if ([Environment]::Is64BitOperatingSystem) { "x64" } else { "x86" }
Write-Host "🔍 Detected: Windows ($ARCH)" -ForegroundColor Gray

# Get latest release
Write-Host "🔎 Fetching latest release..." -ForegroundColor Gray
try {
    $releaseInfo = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/latest"
    $LATEST_VERSION = $releaseInfo.tag_name

    if (-not $LATEST_VERSION) {
        throw "Failed to fetch latest version"
    }

    Write-Host "📥 Downloading mobilectl $LATEST_VERSION..." -ForegroundColor Cyan
} catch {
    Write-Host "❌ Failed to fetch latest version: $_" -ForegroundColor Red
    exit 1
}

# Download URL
$DOWNLOAD_URL = "https://github.com/$REPO/releases/download/$LATEST_VERSION/mobilectl-windows.zip"
$TEMP_ZIP = "$env:TEMP\mobilectl.zip"

# Download
try {
    Write-Host "📥 Downloading from: $DOWNLOAD_URL" -ForegroundColor Gray
    Invoke-WebRequest -Uri $DOWNLOAD_URL -OutFile $TEMP_ZIP -UseBasicParsing
} catch {
    Write-Host "❌ Download failed: $_" -ForegroundColor Red
    Write-Host "   Please check your internet connection or try again later." -ForegroundColor Yellow
    exit 1
}

# Install
Write-Host "📂 Installing to $BIN_DIR..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $BIN_DIR | Out-Null

# Extract
try {
    Expand-Archive -Path $TEMP_ZIP -DestinationPath $BIN_DIR -Force
} catch {
    Write-Host "❌ Extraction failed: $_" -ForegroundColor Red
    exit 1
}

# Cleanup
Remove-Item -Path $TEMP_ZIP -Force

# Add to PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$BIN_DIR*") {
    Write-Host "🔧 Adding to PATH..." -ForegroundColor Cyan

    try {
        $newPath = "$currentPath;$BIN_DIR"
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")

        # Update current session
        $env:Path = "$env:Path;$BIN_DIR"

        Write-Host "✅ Added to system PATH" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Could not add to PATH automatically" -ForegroundColor Yellow
        Write-Host "   Please add manually: $BIN_DIR" -ForegroundColor Yellow
    }
}

# Verify installation
Write-Host ""
Write-Host "✅ mobilectl installed successfully!" -ForegroundColor Green
Write-Host ""

try {
    $mobilectlPath = Get-Command mobilectl -ErrorAction SilentlyContinue
    if ($mobilectlPath) {
        $version = & mobilectl --version 2>$null
        if (-not $version) { $version = "unknown" }

        Write-Host "📍 Location: $($mobilectlPath.Source)" -ForegroundColor Gray
        Write-Host "📌 Version: $version" -ForegroundColor Gray
        Write-Host ""
        Write-Host "🚀 Get started:" -ForegroundColor Cyan
        Write-Host "   mobilectl setup    # Configure your project" -ForegroundColor White
        Write-Host "   mobilectl --help   # View all commands" -ForegroundColor White
    } else {
        Write-Host "⚠️  mobilectl installed but not available in current session" -ForegroundColor Yellow
        Write-Host "   Please restart your terminal/PowerShell" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Installation complete, but verification failed" -ForegroundColor Yellow
    Write-Host "   Please restart your terminal and run: mobilectl --version" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "📚 Documentation: https://github.com/$REPO" -ForegroundColor Gray
Write-Host ""
Write-Host "💡 Tip: You may need to restart your terminal for PATH changes to take effect" -ForegroundColor Yellow
