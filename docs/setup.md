# Setup Wizard

## Overview

`mobilectl setup` is a comprehensive interactive wizard that guides you through complete project configuration. Instead of manually creating `mobileops.yaml`, the wizard detects your project settings and generates the complete configuration in one unified flow.

**Key Features:**
- 🚀 One unified wizard (not scattered setup)
- 🔍 Auto-detects project type and configuration
- ✅ A-Z coverage through 8 setup phases
- 📝 Generates complete `mobileops.yaml`
- 🤖 Optional CI/CD workflow generation
- 📋 Creates setup documentation
- 💾 Backs up existing configuration

---

## Quick Start

### First-Time Setup

```bash
mobilectl setup
```

The wizard will guide you through 8 phases:

1. **Project Information** - Name, package, type, version
2. **Build Configuration** - Flavors, signing, platform settings
3. **Deployment Destinations** - Firebase, Play Console, TestFlight
4. **Version Management** - Auto-increment, bump strategy
5. **Changelog** - Format, output, generation
6. **Deployment Groups** - Flavor grouping
7. **CI/CD Setup** - GitHub Actions, GitLab CI
8. **Review & Confirm** - Summary and generation

### What Gets Generated

```
├── mobileops.yaml                      # Complete configuration
├── .github/workflows/                  # (optional)
│   └── mobilectl-deploy.yml           # GitHub Actions workflow
├── .gitlab-ci.yml                      # (optional) GitLab CI
└── docs/
    └── SETUP.md                        # Setup summary
```

---

## Basic Usage

### Run the Wizard

```bash
mobilectl setup
```

**Output:**
```
🚀 mobilectl Setup Wizard
════════════════════════════════════════════════════════════

📱 Welcome to mobilectl!
Let's set up your project for mobile deployment.

Press Enter to continue...
```

### Overwrite Existing Configuration

```bash
mobilectl setup --force
```

Overwrites existing `mobileops.yaml` (creates backup first).

### Custom Output Path

```bash
mobilectl setup --output custom-config.yaml
```

Generates configuration to a custom path.

---

## Setup Phases

### Phase 1: Project Information

The wizard auto-detects your project and asks for confirmation:

```
1️⃣  PROJECT INFORMATION
────────────────────────────────────────────

📁 Project Type
Auto-detected: Flutter
Use this? (Y/n) y ✓

📝 App Name
App name [MyApp]:

📦 Package Name
Auto-detected: com.example.myapp ✓ Use this? (Y/n) y ✓

🏢 Organization
Organization [My Company]:

📍 Current Version
Version [1.0.0]:
```

**Auto-Detection:**
- Project type (Android Native, Flutter, React Native, iOS)
- App name from `pubspec.yaml`, `strings.xml`, or directory name
- Package name from `build.gradle.kts` or `AndroidManifest.xml`
- Version from `pubspec.yaml` or build files

### Phase 2: Build Configuration

Configure build settings for each platform:

```
2️⃣  BUILD SETUP
────────────────────────────────────────────

🤖 Android Build Configuration

🎯 Product Flavors
Detected from build.gradle.kts:
  ✓ free
  ✓ paid
  ✓ premium

Add more? (y/n) n ✓

🔐 Default Flavor
Which is default?
[1] free
[2] paid
[3] premium
> 1 ✓

🔒 Signing Configuration
Keystore path [release.jks]:
Key alias [release-key]:
(Passwords will be set via environment variables)
```

**Auto-Detection:**
- Android product flavors from `build.gradle.kts` or `build.gradle`
- iOS project/workspace files
- Xcode scheme names

### Phase 3: Deployment Destinations

Configure where to deploy your builds:

```
3️⃣  DEPLOYMENT DESTINATIONS
────────────────────────────────────────────

Select where to deploy:

🔥 Firebase App Distribution
Credentials found: credentials/firebase-adminsdk.json ✓
Enable? (Y/n) y ✓
Test groups (comma-separated): qa-team, beta-testers ✓

🎮 Google Play Console
Credentials found: credentials/play-console.json ✓
Enable? (Y/n) y ✓

✈️ TestFlight
API key found: credentials/app-store-connect-api-key.json ✓
Enable? (Y/n) y ✓
Team ID: ABC123DEF ✓

📁 Local Filesystem
Enable for testing? (y/n) n ✓
```

**Auto-Detection:**
- Firebase service account JSON files
- Google Play Console credentials
- App Store Connect API keys
- `google-services.json` location

### Phase 4: Version Management

Configure automatic version bumping:

```
4️⃣  VERSION MANAGEMENT
────────────────────────────────────────────

📌 Auto-Increment on Deploy
Automatically bump version? (Y/n) y ✓

Default strategy:
[1] patch
[2] minor
[3] major
[4] auto
> 1 ✓

📄 Files to Update
Detected:
  ✓ pubspec.yaml
  ✓ app/build.gradle.kts

Add more? (y/n) n ✓
```

**Auto-Detection:**
- Version files (`pubspec.yaml`, `build.gradle.kts`, `package.json`)
- Current version numbers

### Phase 5: Changelog

Configure automatic changelog generation:

```
5️⃣  CHANGELOG
────────────────────────────────────────────

📝 Generate Changelogs
Generate on deploy? (Y/n) y ✓

Format:
[1] markdown
[2] html
[3] json
> 1 ✓

Output file [CHANGELOG.md]:

Start from:
[1] auto-detect
[2] specific-tag
[3] last-release
> 1 ✓

Append to existing CHANGELOG.md? (Y/n) y ✓
```

### Phase 6: Deployment Groups

Create flavor groups for easy multi-variant deployment:

```
6️⃣  DEPLOYMENT GROUPS
────────────────────────────────────────────

🎯 Create Flavor Groups for Easy Deployment

Suggested groups:
  1. Production Release (all flavors)
  2. QA Testing (debug builds)
  3. Individual flavor groups

Create suggested groups? (Y/n) y ✓

Add custom group? (y/n) n ✓
```

**Benefits:**
- Deploy multiple flavors with one command
- Group related builds together
- Simplify CI/CD pipelines

### Phase 7: CI/CD Setup

Generate workflow files for popular CI/CD platforms:

```
7️⃣  CI/CD SETUP
────────────────────────────────────────────

🤖 GitHub Actions
Generate example workflow? (Y/n) y ✓

Triggers:
  On tag push (v*.*.*)? (Y/n) y ✓
  Manual dispatch? (Y/n) y ✓
  On PR (test only)? (Y/n) y ✓

→ Workflow saved to .github/workflows/mobilectl-deploy.yml ✓

🦊 GitLab CI
Generate example pipeline? (y/n) n ✓
```

**Generated Workflows Include:**
- Build jobs for each platform
- Version bumping
- Changelog generation
- Deployment steps
- Proper secret handling
- PR testing

### Phase 8: Review & Confirm

Review your configuration before generation:

```
8️⃣  REVIEW & CONFIRM
────────────────────────────────────────────

📋 Configuration Summary

Project:
  • Name: MyApp
  • Package: com.example.myapp
  • Type: Flutter
  • Version: 1.0.0

Android Build:
  • Flavors: free, paid, premium
  • Default: free
  • Signing: release.jks

Deployment:
  • Firebase (qa-team, beta-testers)
  • Play Console
  • TestFlight

Version:
  • Auto-increment: patch
  • Files: 2

Changelog:
  • Auto-generate: markdown
  • Output: CHANGELOG.md

Groups:
  • production (3 flavors)
  • qa (1 flavor)

CI/CD:
  • GitHub Actions configured

Everything looks good? (Y/n) y ✓

✅ Configuration saved to: mobileops.yaml

📄 Also created:
  • .github/workflows/mobilectl-deploy.yml
  • docs/SETUP.md (setup summary)

🎉 Ready to deploy!
Try: mobilectl deploy --all-variants -y
```

---

## Options

### Force Overwrite

Skip confirmation when config already exists:

```bash
mobilectl setup --force
# or
mobilectl setup -f
```

**Behavior:**
- Backs up existing config to `mobileops.backup.TIMESTAMP.yaml`
- Overwrites without prompting
- Shows backup location

### Custom Output Path

Generate config to a different location:

```bash
mobilectl setup --output path/to/config.yaml
# or
mobilectl setup -o path/to/config.yaml
```

**Use Cases:**
- Multiple environments
- Testing configurations
- Custom project structures

---

## Auto-Detection

The wizard intelligently detects your project configuration:

### Project Type Detection

| Project Type | Detection Method |
|--------------|------------------|
| **Flutter** | `pubspec.yaml` + `lib/` directory |
| **React Native** | `package.json` with `react-native` dependency |
| **Android Native** | `app/build.gradle.kts` or `build.gradle` |
| **iOS Native** | `.xcodeproj` or `.xcworkspace` files |

### Credentials Detection

The wizard searches for credentials in common locations:

**Firebase:**
```
credentials/firebase-service-account.json
credentials/firebase-adminsdk.json
firebase-service-account.json
```

**Play Console:**
```
credentials/play-console.json
credentials/play-console-service-account.json
play-console.json
```

**App Store Connect:**
```
credentials/app-store-connect-api-key.json
credentials/appstore-api-key.json
```

**Google Services:**
```
app/google-services.json
app/src/main/google-services.json
android/app/google-services.json
```

### Build Configuration Detection

**Android Flavors:**
- Parses `build.gradle.kts` (Kotlin DSL)
- Parses `build.gradle` (Groovy)
- Extracts flavor names from `productFlavors` block

**iOS Projects:**
- Finds `.xcworkspace` (preferred) or `.xcodeproj`
- Extracts scheme names
- Locates `Info.plist` files

**Version Files:**
- `pubspec.yaml` (Flutter)
- `app/build.gradle.kts` (Android)
- `package.json` (React Native)
- `Info.plist` (iOS)

---

## Generated Configuration

### Complete mobileops.yaml

The wizard generates a complete, valid configuration:

```yaml
app:
  name: MyApp
  identifier: com.example.myapp
  version: 1.0.0

build:
  android:
    enabled: true
    default_flavor: free
    default_type: release
    flavors:
      - free
      - paid
      - premium
    key_store: release.jks
    key_alias: release-key
    key_password: ${ANDROID_KEY_PASSWORD}
    store_password: ${ANDROID_STORE_PASSWORD}
    use_env_for_passwords: true

  ios:
    enabled: true
    project_path: ios/MyApp.xcworkspace
    scheme: MyApp
    configuration: Release
    code_sign_identity: iPhone Distribution
    provisioning_profile: MyApp Distribution

version:
  enabled: true
  current: 1.0.0
  auto_increment: true
  bump_strategy: patch
  files_to_update:
    - pubspec.yaml
    - app/build.gradle.kts

changelog:
  enabled: true
  format: markdown
  output_file: CHANGELOG.md
  append: true
  include_breaking_changes: true
  include_contributors: true
  include_stats: true

deploy:
  enabled: true

  android:
    artifact_path: build/outputs/apk/${flavor}/release/app-${flavor}-release.apk

    firebase:
      enabled: true
      service_account: credentials/firebase-adminsdk.json
      google_services: app/google-services.json
      test_groups:
        - qa-team
        - beta-testers

    play_console:
      enabled: true
      service_account: credentials/play-console.json
      package_name: com.example.myapp

    local:
      enabled: false
      output_dir: build/deploy

  ios:
    artifact_path: build/outputs/ipa/${scheme}.ipa

    testflight:
      enabled: true
      api_key_path: credentials/app-store-connect-api-key.json
      bundle_id: com.example.myapp
      team_id: ABC123DEF

    app_store:
      enabled: false
      api_key_path: credentials/app-store-connect-api-key.json
      bundle_id: com.example.myapp
      team_id: ABC123DEF

  flavor_groups:
    production:
      name: production
      description: All production flavors
      flavors:
        - free
        - paid
        - premium

    qa:
      name: qa
      description: QA testing flavors
      flavors:
        - debug

  default_group: production

notify:
  slack:
    enabled: false
  email:
    enabled: false
  webhook:
    enabled: false

report:
  enabled: false
```

### GitHub Actions Workflow

Generated workflow includes:

```yaml
name: Mobile App Deployment

on:
  push:
    tags:
      - 'v*.*.*'

  workflow_dispatch:
    inputs:
      environment:
        description: 'Deployment environment'
        required: true
        type: choice
        options:
          - production
          - staging
          - dev

jobs:
  deploy-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Build Android app
        env:
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
          ANDROID_STORE_PASSWORD: ${{ secrets.ANDROID_STORE_PASSWORD }}
        run: mobilectl build

      - name: Deploy to Firebase
        env:
          FIREBASE_SERVICE_ACCOUNT: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
        run: mobilectl deploy --platform android --destination firebase -y
```

### Setup Summary

Generated `docs/SETUP.md` includes:

```markdown
# mobilectl Setup Summary

Generated: 2025-11-07 12:34:56

## Project
- Name: MyApp
- Package: com.example.myapp
- Type: Flutter
- Version: 1.0.0

## Configuration
- Config file: `mobileops.yaml`
- Build flavors: 3
- Deployment groups: 2

## Next Steps
1. Review the generated `mobileops.yaml`
2. Set environment variables for passwords:
   - `ANDROID_KEY_PASSWORD`
   - `ANDROID_STORE_PASSWORD`
3. Try deploying: `mobilectl deploy --all-variants -y`

## Documentation
- See `docs/` for more information
- Run `mobilectl --help` for available commands
```

---

## Next Steps After Setup

### 1. Set Environment Variables

For Android signing:

```bash
export ANDROID_KEY_PASSWORD=your-key-password
export ANDROID_STORE_PASSWORD=your-store-password
```

### 2. Build Your App

```bash
mobilectl build
```

### 3. Deploy

Deploy to all configured destinations:

```bash
mobilectl deploy --all-variants -y
```

Or deploy a specific flavor group:

```bash
mobilectl deploy --group production
```

### 4. Set Up CI/CD Secrets

If you generated GitHub Actions workflow, add secrets:

1. Go to repository Settings → Secrets
2. Add required secrets:
   - `ANDROID_KEY_PASSWORD`
   - `ANDROID_STORE_PASSWORD`
   - `FIREBASE_SERVICE_ACCOUNT`
   - etc.

---

## Examples

### Flutter Project

```bash
$ mobilectl setup

🚀 mobilectl Setup Wizard
════════════════════════════════════════════════════════════

📁 Project Type
Auto-detected: Flutter ✓

📝 App Name
App name [my_flutter_app]: MyFlutterApp

📦 Package Name
Auto-detected: com.example.flutter ✓

# ... continues through all phases ...

✅ Configuration saved to: mobileops.yaml
🎉 Ready to deploy!
```

### Android Native Project

```bash
$ mobilectl setup

🚀 mobilectl Setup Wizard
════════════════════════════════════════════════════════════

📁 Project Type
Auto-detected: Android (native) ✓

🎯 Product Flavors
Detected from build.gradle.kts:
  ✓ free
  ✓ paid

# ... continues with Android-specific configuration ...
```

### React Native Project

```bash
$ mobilectl setup

🚀 mobilectl Setup Wizard
════════════════════════════════════════════════════════════

📁 Project Type
Auto-detected: React Native ✓

# ... supports both Android and iOS configuration ...
```

---

## Troubleshooting

### Config Already Exists

```
⚠ Configuration file already exists: mobileops.yaml

Options:
  1. Overwrite (backup will be created)
  2. Cancel
  3. Specify different output path

Choice [1/2/3]:
```

**Solution:** Choose option 1 to backup and overwrite, or use `--force` flag.

### Credentials Not Found

If credentials aren't auto-detected:

```
🔥 Firebase App Distribution
No credentials found.
Enable? (y/n) y
Service account JSON path: credentials/firebase-adminsdk.json
```

**Solution:** Provide the path manually when prompted.

### No Flavors Detected

```
🎯 Product Flavors
No flavors detected.

Add flavors? (y/n) y
Enter flavor names (comma-separated): free, paid, premium
```

**Solution:** Enter flavors manually or configure later in `mobileops.yaml`.

### Permission Denied

```
❌ Error: Failed to save configuration: Permission denied
```

**Solution:** Run with appropriate permissions or change output path:
```bash
mobilectl setup --output ~/configs/mobileops.yaml
```

---

## Configuration Reference

After setup, you can manually edit `mobileops.yaml`. See:
- [Configuration Reference](config-reference.md) - Complete config options
- [Build Command](build.md) - Build configuration
- [Version Management](version.md) - Version bumping
- [Changelog](changelog.md) - Changelog generation

---

## Tips & Best Practices

### Keep Credentials Secure

- ✅ Store credentials in `credentials/` directory
- ✅ Add `credentials/` to `.gitignore`
- ✅ Use environment variables for passwords
- ❌ Never commit credentials to git

### Use Deployment Groups

```bash
# Instead of deploying each flavor separately:
mobilectl deploy android free
mobilectl deploy android paid
mobilectl deploy android premium

# Use a group:
mobilectl deploy --group production
```

### Version Control Your Config

```bash
# Add to git:
git add mobileops.yaml
git commit -m "chore: Add mobilectl configuration"

# But not credentials:
echo "credentials/" >> .gitignore
```

### Customize Generated Workflows

The generated CI/CD workflows are templates. Customize them:

```yaml
# .github/workflows/mobilectl-deploy.yml
# Add custom steps:
- name: Run tests
  run: ./gradlew test

- name: Custom notification
  run: ./scripts/notify-team.sh
```

### Re-run Setup to Update

If you need to change configuration:

```bash
# Backup current config
cp mobileops.yaml mobileops.yaml.bak

# Re-run setup
mobilectl setup --force

# Compare and merge changes
diff mobileops.yaml mobileops.yaml.bak
```

---

## Support

Need help with setup?

- 📖 Read the [configuration reference](config-reference.md)
- 💬 Open an [issue on GitHub](https://github.com/AhmedNader65/MobileCtl/issues)
- 🔍 Check [troubleshooting section](#troubleshooting)

---

**Related Commands:**
- [`mobilectl build`](build.md) - Build your app
- [`mobilectl deploy`](deploy.md) - Deploy builds
- [`mobilectl version`](version.md) - Manage versions
- [`mobilectl changelog`](changelog.md) - Generate changelogs
