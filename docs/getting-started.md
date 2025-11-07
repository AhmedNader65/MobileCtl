# Getting Started with mobilectl

Welcome to mobilectl! This guide will help you set up and start using mobilectl in your mobile project.

## Prerequisites

Before you begin, make sure you have:

- ✅ A mobile project (Android, iOS, Flutter, or React Native)
- ✅ Java JDK 17 or higher installed
- ✅ Git initialized in your project
- ✅ (Optional) Credentials for deployment destinations

## Installation

### Download

Coming soon — Homebrew, direct download, etc.

For now, build from source:

```bash
git clone https://github.com/AhmedNader65/MobileCtl.git
cd MobileCtl
./gradlew :cli:installDist

# Add to PATH
export PATH="$PATH:$(pwd)/cli/build/install/cli/bin"
```

### Verify Installation

```bash
mobilectl --version
```

You should see the mobilectl version number.

---

## Quick Setup (5 Minutes)

### Step 1: Run the Setup Wizard

Navigate to your project directory and run:

```bash
cd /path/to/your/mobile/project
mobilectl setup
```

### Step 2: Follow the Wizard

The wizard will guide you through 8 phases:

```
🚀 mobilectl Setup Wizard
════════════════════════════════════════════════════════════

📱 Welcome to mobilectl!
Let's set up your project for mobile deployment.
```

Answer the prompts (most have auto-detected defaults):

1. **Project Information** - Confirm or enter app name, package, version
2. **Build Configuration** - Configure flavors and signing
3. **Deployment Destinations** - Set up Firebase, Play Console, TestFlight
4. **Version Management** - Enable auto-increment
5. **Changelog** - Configure changelog generation
6. **Deployment Groups** - Group flavors for easy deployment
7. **CI/CD Setup** - Generate workflow files
8. **Review & Confirm** - Review and generate configuration

### Step 3: Review Generated Files

The wizard creates:

```
✅ Configuration saved to: mobileops.yaml
✅ GitHub Actions workflow created
✅ Setup summary saved to: docs/SETUP.md

🎉 Ready to deploy!
```

### Step 4: Set Environment Variables

If you're using Android signing, set passwords:

```bash
export ANDROID_KEY_PASSWORD=your-key-password
export ANDROID_STORE_PASSWORD=your-store-password
```

Add these to your `~/.bashrc`, `~/.zshrc`, or CI/CD secrets.

### Step 5: Try It Out!

Build your app:

```bash
mobilectl build
```

Deploy to configured destinations:

```bash
mobilectl deploy --all-variants
```

🎉 **That's it!** You're now using mobilectl.

---

## What's Next?

### Learn the Commands

Explore what mobilectl can do:

```bash
mobilectl --help
```

Key commands:
- `mobilectl build` - [Build your app](build.md)
- `mobilectl deploy` - Deploy to destinations
- `mobilectl version` - [Manage versions](version.md)
- `mobilectl changelog` - [Generate changelogs](changelog.md)
- `mobilectl info` - Show project info

### Configure Advanced Features

Edit `mobileops.yaml` to customize:

- **Deployment groups** - Group flavors for batch deployment
- **Notifications** - Slack, email, webhook alerts
- **Reports** - Generate deployment reports
- **CI/CD integration** - Customize generated workflows

See [Configuration Reference](config-reference.md) for all options.

### Set Up CI/CD

If you generated GitHub Actions workflow:

1. Push to GitHub
2. Add secrets to repository settings:
   - `ANDROID_KEY_PASSWORD`
   - `ANDROID_STORE_PASSWORD`
   - `FIREBASE_SERVICE_ACCOUNT`
3. Create a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
4. Watch your automated deployment! 🚀

### Join the Community

- ⭐ Star the project on [GitHub](https://github.com/AhmedNader65/MobileCtl)
- 🐛 Report issues or request features
- 📖 Read the full [documentation](.)

---

## Common First Tasks

### Build Your App

```bash
# Build all platforms
mobilectl build

# Build specific platform
mobilectl build android
mobilectl build ios

# Build with verbose output
mobilectl build --verbose
```

[Learn more about building →](build.md)

### Deploy Your First Build

```bash
# Deploy to Firebase
mobilectl deploy --platform android --destination firebase

# Deploy all flavors
mobilectl deploy --all-variants

# Deploy a flavor group
mobilectl deploy --group production
```

### Bump Your Version

```bash
# Bump patch version (1.0.0 → 1.0.1)
mobilectl version bump patch

# Bump minor version (1.0.0 → 1.1.0)
mobilectl version bump minor

# Bump major version (1.0.0 → 2.0.0)
mobilectl version bump major
```

[Learn more about versioning →](version.md)

### Generate a Changelog

```bash
# Generate changelog from commits
mobilectl changelog generate

# Preview without writing
mobilectl changelog generate --dry-run

# Append to existing changelog
mobilectl changelog generate --append
```

[Learn more about changelogs →](changelog.md)

---

## Troubleshooting

### Setup Wizard Issues

**Config already exists:**
```bash
# Force overwrite (creates backup)
mobilectl setup --force
```

**Credentials not found:**

Place credentials in `credentials/` directory:
```
project/
├── credentials/
│   ├── firebase-adminsdk.json
│   ├── play-console.json
│   └── app-store-connect-api-key.json
└── mobileops.yaml
```

### Build Issues

**Platform not detected:**
```bash
# Check with verbose mode
mobilectl build --verbose

# Verify project structure
ls -la  # Should see build.gradle.kts, pubspec.yaml, etc.
```

**Missing dependencies:**

Make sure you have required tools installed:
- Android: Android SDK, Gradle
- iOS: Xcode, CocoaPods
- Flutter: Flutter SDK

### Deploy Issues

**Credentials error:**
```bash
# Verify credential files exist
ls -la credentials/

# Check file permissions
chmod 600 credentials/*.json
```

**Firebase upload fails:**

Make sure:
1. Service account has "Firebase App Distribution Admin" role
2. `google-services.json` is in correct location
3. App is registered in Firebase Console

---

## Examples by Project Type

### Flutter Project

```bash
# Setup
cd my_flutter_app
mobilectl setup
# Select "Flutter" when prompted
# Wizard auto-detects pubspec.yaml

# Build
mobilectl build  # Builds both Android and iOS

# Deploy
mobilectl deploy --all-variants
```

### Android Native

```bash
# Setup
cd my_android_app
mobilectl setup
# Select "Android (native)"
# Wizard detects flavors from build.gradle.kts

# Build specific flavor
mobilectl build android production release

# Deploy to Play Console
mobilectl deploy --platform android --destination play-console
```

### React Native

```bash
# Setup
cd my_react_native_app
mobilectl setup
# Select "React Native"
# Wizard detects both platforms

# Build both platforms
mobilectl build all

# Deploy
mobilectl deploy --all-variants
```

### iOS Native

```bash
# Setup
cd my_ios_app
mobilectl setup
# Select "iOS (native)"
# Wizard finds .xcworkspace

# Build
mobilectl build ios

# Deploy to TestFlight
mobilectl deploy --platform ios --destination testflight
```

---

## Tips for Success

### 1. Use Version Control

Add `mobileops.yaml` to git but exclude credentials:

```bash
# .gitignore
credentials/
*.keystore
*.jks
```

### 2. Use Environment Variables

Never hardcode passwords:

```yaml
# mobileops.yaml
build:
  android:
    key_password: ${ANDROID_KEY_PASSWORD}
    store_password: ${ANDROID_STORE_PASSWORD}
```

### 3. Use Deployment Groups

Group related flavors for easy deployment:

```yaml
deploy:
  flavor_groups:
    production:
      name: production
      description: All production flavors
      flavors: [free, paid, premium]
```

Deploy all production flavors at once:
```bash
mobilectl deploy --group production
```

### 4. Automate with CI/CD

Use the generated GitHub Actions workflow:

```bash
# Trigger deployment with tag
git tag v1.0.0
git push origin v1.0.0

# Or trigger manually from GitHub Actions UI
```

### 5. Keep Documentation Updated

After changes, regenerate setup docs:

```bash
mobilectl setup --force
```

---

## Next Steps

✅ You've set up mobilectl!

**Continue learning:**
- 📖 [Setup Wizard Guide](setup.md) - Detailed setup documentation
- 🏗️ [Build Command](build.md) - Building your apps
- 📦 Deploy Guide (coming soon) - Deployment strategies
- 🔢 [Version Management](version.md) - Version bumping
- 📝 [Changelog Generation](changelog.md) - Changelog automation
- ⚙️ [Configuration Reference](config-reference.md) - All config options

**Get help:**
- 💬 Ask questions on [GitHub Issues](https://github.com/AhmedNader65/MobileCtl/issues)
- 📚 Read the full [documentation](.)
- ⭐ Star the project if it helps you!

---

**Ready to automate your mobile deployments!** 🚀

---

**Made with ❤️ for mobile developers**
