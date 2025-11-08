# mobilectl Installation Guide

## Quick Install (Recommended)

### macOS / Linux

```bash
curl -sSL https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.sh | bash
```

### Windows (PowerShell)

```powershell
irm https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.ps1 | iex
```

**What the installer does:**
- ✅ Auto-detects your operating system
- ✅ Downloads the latest release from GitHub
- ✅ Extracts to `~/.mobilectl/bin/` (or `%USERPROFILE%\.mobilectl\bin\` on Windows)
- ✅ Adds mobilectl to your PATH
- ✅ Verifies installation

## Manual Installation

### 1. Download

Download the latest release for your platform:

- **Windows**: [mobilectl-windows.zip](https://github.com/AhmedNader65/MobileCtl/releases/latest/download/mobilectl-windows.zip)
- **macOS**: [mobilectl-macos.tar.gz](https://github.com/AhmedNader65/MobileCtl/releases/latest/download/mobilectl-macos.tar.gz)
- **Linux**: [mobilectl-linux.tar.gz](https://github.com/AhmedNader65/MobileCtl/releases/latest/download/mobilectl-linux.tar.gz)

### 2. Extract

**macOS / Linux:**
```bash
tar -xzf mobilectl-*.tar.gz
sudo mv mobilectl /usr/local/bin/
chmod +x /usr/local/bin/mobilectl
```

**Windows:**
```powershell
# Extract the zip file
Expand-Archive mobilectl-windows.zip -DestinationPath C:\mobilectl

# Add to PATH manually via System Environment Variables
# Or run this in PowerShell (as Administrator):
[Environment]::SetEnvironmentVariable("Path", "$env:Path;C:\mobilectl\bin", "Machine")
```

### 3. Verify Installation

```bash
mobilectl --version
```

You should see output like:
```
mobilectl version 1.0.0
```

## Post-Installation

### First Time Setup

Run the interactive setup wizard to configure your project:

```bash
mobilectl setup
```

This will guide you through:
1. Project Information
2. Build Configuration
3. Deployment Destinations
4. Version Management
5. Changelog Settings
6. Deployment Groups
7. CI/CD Setup

### Verify Your Configuration

After setup, verify everything is configured correctly:

```bash
mobilectl info
```

## Troubleshooting

### Command not found

If you get `command not found: mobilectl`:

**macOS / Linux:**
```bash
# Add to current session
export PATH="$PATH:$HOME/.mobilectl/bin"

# Add permanently to your shell config
echo 'export PATH="$PATH:$HOME/.mobilectl/bin"' >> ~/.bashrc  # or ~/.zshrc
source ~/.bashrc  # or ~/.zshrc
```

**Windows:**
- Restart your terminal/PowerShell
- Or manually add `%USERPROFILE%\.mobilectl\bin` to your system PATH

### Permission Denied (macOS / Linux)

```bash
chmod +x ~/.mobilectl/bin/mobilectl
```

### SSL/Certificate Errors

If you encounter SSL errors during download:

**macOS / Linux:**
```bash
curl -sSLk https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.sh | bash
```

**Windows:**
```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
irm https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.ps1 | iex
```

## Updating mobilectl

To update to the latest version, simply run the install script again:

**macOS / Linux:**
```bash
curl -sSL https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.sh | bash
```

**Windows:**
```powershell
irm https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.ps1 | iex
```

Or download and extract the latest release manually.

## Uninstallation

### macOS / Linux

```bash
# Remove binary
rm -rf ~/.mobilectl

# Remove from shell config (if added)
# Edit ~/.bashrc or ~/.zshrc and remove the mobilectl PATH line
```

### Windows

```powershell
# Remove installation directory
Remove-Item -Path "$env:USERPROFILE\.mobilectl" -Recurse -Force

# Remove from PATH
# You'll need to manually remove from System Environment Variables
# Or use PowerShell (as Administrator):
$path = [Environment]::GetEnvironmentVariable("Path", "User")
$newPath = ($path.Split(';') | Where-Object { $_ -notlike '*mobilectl*' }) -join ';'
[Environment]::SetEnvironmentVariable("Path", $newPath, "User")
```

## System Requirements

- **macOS**: macOS 10.15 (Catalina) or later
- **Linux**: Ubuntu 18.04+ / Debian 10+ / Fedora 30+ or equivalent
- **Windows**: Windows 10 or later, PowerShell 5.1+

### For Android Development:
- JDK 11 or later
- Android SDK with build tools
- Gradle 7.0+

### For iOS Development:
- macOS required
- Xcode 13.0+
- CocoaPods (for React Native/Flutter)

## Next Steps

After installation, check out:

- [Setup Guide](docs/setup.md) - Complete configuration guide
- [Quick Start](README.md#quick-start) - Get started in 5 minutes
- [Configuration Reference](docs/config-reference.md) - All configuration options
- [Examples](examples/) - Example projects

## Getting Help

- **Issues**: [GitHub Issues](https://github.com/AhmedNader65/MobileCtl/issues)
- **Documentation**: [docs/](docs/)
- **Discussions**: [GitHub Discussions](https://github.com/AhmedNader65/MobileCtl/discussions)
