#!/bin/bash
set -e

REPO="AhmedNader65/MobileCtl"
INSTALL_DIR="$HOME/.mobilectl"
BIN_DIR="$INSTALL_DIR/bin"

echo "📦 Installing mobilectl..."

# Detect OS and architecture
OS="$(uname -s)"
ARCH="$(uname -m)"

case "$OS" in
    Linux*)
        PLATFORM="linux"
        SHELL_CONFIG="$HOME/.bashrc"
        ;;
    Darwin*)
        PLATFORM="macos"
        SHELL_CONFIG="$HOME/.zshrc"
        if [ ! -f "$SHELL_CONFIG" ]; then
            SHELL_CONFIG="$HOME/.bash_profile"
        fi
        ;;
    *)
        echo "❌ Unsupported OS: $OS"
        exit 1
        ;;
esac

echo "🔍 Detected: $OS ($ARCH)"

# Get latest release
echo "🔎 Fetching latest release..."
LATEST_VERSION=$(curl -s "https://api.github.com/repos/$REPO/releases/latest" \
    | grep '"tag_name":' \
    | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$LATEST_VERSION" ]; then
    echo "❌ Failed to fetch latest version"
    exit 1
fi

echo "📥 Downloading mobilectl $LATEST_VERSION..."

# Download URL
DOWNLOAD_URL="https://github.com/$REPO/releases/download/$LATEST_VERSION/mobilectl-$PLATFORM.tar.gz"

# Download
if ! curl -sSL "$DOWNLOAD_URL" -o /tmp/mobilectl.tar.gz; then
    echo "❌ Download failed. Please check your internet connection."
    exit 1
fi

# Install
echo "📂 Installing to $BIN_DIR..."
mkdir -p "$BIN_DIR"

# Extract
tar -xzf /tmp/mobilectl.tar.gz -C "$BIN_DIR"
chmod +x "$BIN_DIR/mobilectl"

# Cleanup
rm /tmp/mobilectl.tar.gz

# Add to PATH
if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    echo "🔧 Adding to PATH..."

    # Add to shell config
    if [ -f "$SHELL_CONFIG" ]; then
        if ! grep -q "mobilectl" "$SHELL_CONFIG"; then
            echo "" >> "$SHELL_CONFIG"
            echo "# mobilectl" >> "$SHELL_CONFIG"
            echo "export PATH=\"\$PATH:$BIN_DIR\"" >> "$SHELL_CONFIG"
            echo "✅ Added to $SHELL_CONFIG"
            echo "   Run: source $SHELL_CONFIG"
        fi
    fi

    # Also add to current session
    export PATH="$PATH:$BIN_DIR"
fi

# Verify installation
echo ""
echo "✅ mobilectl installed successfully!"
echo ""

if command -v mobilectl &> /dev/null; then
    VERSION=$(mobilectl --version 2>/dev/null || echo "unknown")
    echo "📍 Location: $(which mobilectl)"
    echo "📌 Version: $VERSION"
    echo ""
    echo "🚀 Get started:"
    echo "   mobilectl setup    # Configure your project"
    echo "   mobilectl --help   # View all commands"
else
    echo "⚠️  mobilectl installed but not in PATH for current session"
    echo "   Run: export PATH=\"\$PATH:$BIN_DIR\""
    echo "   Or restart your terminal"
fi

echo ""
echo "📚 Documentation: https://github.com/$REPO"
