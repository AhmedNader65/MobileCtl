# mobilectl

Modern DevOps automation for Android apps. Build, version, and deploy with a single command.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.3.2-green.svg)](https://github.com/AhmedNader65/MobileCtl/releases)

## ✨ Features

- 🚀 **One-Line Installation** — Install in seconds on any platform
- 🏗️ **Android Build Automation** — AAB/APK builds with signing support
- 📦 **Google Play Deployment** — Direct upload to Play Console with track management
- 🔥 **Firebase Distribution** — Deploy to testers instantly
- 🔢 **Version Management** — Automatic semantic versioning
- 📝 **Changelog Generation** — Auto-generated from git commits
- 🎯 **Multi-Flavor Support** — Build and deploy multiple variants
- 🤖 **Setup Wizard** — Interactive configuration in minutes
- ⚡ **Fast & Reliable** — Built with Kotlin, no Ruby dependencies

## 🚀 Quick Start

### Installation

**macOS / Linux:**
```bash
curl -sSL https://raw.githubusercontent.com/AhmedNader65/MobileCtl/master/install.sh | bash
```

**Windows (PowerShell):**
```powershell
irm https://raw.githubusercontent.com/AhmedNader65/MobileCtl/master/install.ps1 | iex
```

### Setup Your Project

```bash
cd your-android-project
mobilectl setup
```

The interactive wizard will guide you through configuration in 8 steps.

### Deploy Your App

```bash
# Deploy to Firebase
mobilectl deploy firebase

# Deploy to Google Play Console
mobilectl deploy playstore

# Deploy all variants
mobilectl deploy --all-variants
```

## 📋 What's Supported

### ✅ Currently Available

**Android:**
- ✅ AAB/APK builds with signing
- ✅ Multi-flavor builds
- ✅ Firebase App Distribution
- ✅ Google Play Console (Internal, Alpha, Beta, Production)
- ✅ Local filesystem deployment
- ✅ Automatic version bumping
- ✅ Changelog generation

**General:**
- ✅ Setup wizard
- ✅ Version management
- ✅ Git-based changelog
- ✅ CI/CD workflow generation

### 🔜 Coming Soon

- 🔜 iOS builds (Xcode)
- 🔜 TestFlight deployment
- 🔜 App Store deployment
- 🔜 Notifications (Slack, Email)
- 🔜 Release notes automation

## 📖 Documentation

**Full documentation:** https://ahmednader65.github.io/MobileCtl/

- [Installation Guide](https://ahmednader65.github.io/MobileCtl/guide/installation)
- [Setup Wizard](https://ahmednader65.github.io/MobileCtl/guide/setup-wizard)
- [Configuration Reference](https://ahmednader65.github.io/MobileCtl/reference/configuration)
- [Deployment Guide](https://ahmednader65.github.io/MobileCtl/guide/deployment)
- [Command Reference](https://ahmednader65.github.io/MobileCtl/reference/commands)

## 🎯 Common Commands

```bash
# Setup
mobilectl setup                       # Interactive setup wizard
mobilectl info                        # Show project info

# Build
mobilectl build                       # Build release APK/AAB
mobilectl build --flavor production   # Build specific flavor

# Deploy
mobilectl deploy firebase             # Deploy to Firebase
mobilectl deploy playstore            # Deploy to Play Console
mobilectl deploy --all-variants       # Deploy all flavors

# Version
mobilectl version bump patch          # Bump patch version (1.0.0 → 1.0.1)
mobilectl version bump minor          # Bump minor version (1.0.0 → 1.1.0)
mobilectl version bump major          # Bump major version (1.0.0 → 2.0.0)

# Changelog
mobilectl changelog generate          # Generate changelog from commits
```

## 🔧 Configuration

Create `mobileops.yaml` in your project root (or use `mobilectl setup`):

```yaml
app:
  name: MyApp
  identifier: com.example.myapp
  version: 1.0.0

build:
  android:
    enabled: true
    flavors:
      - production
      - staging
    defaultFlavor: production
    keyStore: release.jks
    keyAlias: release-key

deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase-service-account.json
      testGroups:
        - qa-team
        - beta-testers

    playConsole:
      enabled: true
      serviceAccount: credentials/play-console-service-account.json
      track: internal
      status: draft

version:
  enabled: true
  autoIncrement: true
  bumpStrategy: patch
```

[📖 Full Configuration Reference](https://ahmednader65.github.io/MobileCtl/reference/configuration)

## 🛠️ Requirements

**For Android:**
- JDK 11 or later
- Android SDK with build tools
- Gradle 7.0+

**For Deployment:**
- Firebase: Service account JSON
- Google Play: Service account JSON with Play Console access

[📖 Credential Setup Guides](https://ahmednader65.github.io/MobileCtl/guide/setup-wizard)

## 🏗️ Example Workflow

```bash
# 1. Setup project
cd my-android-app
mobilectl setup

# 2. Build and test
mobilectl build

# 3. Bump version
mobilectl version bump minor

# 4. Generate changelog
mobilectl changelog generate

# 5. Deploy to testers
mobilectl deploy firebase

# 6. Deploy to Play Console
mobilectl deploy playstore --track internal
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

## 🌟 Show Your Support

If mobilectl helps your workflow, please give it a star ⭐

---

**Made with ❤️ for Android developers**

📦 [Latest Release](https://github.com/AhmedNader65/MobileCtl/releases) | 📖 [Documentation](https://ahmednader65.github.io/MobileCtl/) | 🐛 [Report Issue](https://github.com/AhmedNader65/MobileCtl/issues)
