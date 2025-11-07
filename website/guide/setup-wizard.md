# Setup Wizard

The interactive setup wizard is the easiest way to configure MobileCtl for your project. It guides you through 8 comprehensive phases to generate a complete `mobileops.yaml` configuration.

## Overview

Instead of manually creating a configuration file, the setup wizard:

- 🔍 **Auto-detects** your project type and settings
- ✅ **Validates** configuration as you go
- 📝 **Generates** complete `mobileops.yaml`
- 🤖 **Creates** CI/CD workflows (optional)
- 📋 **Documents** your setup

## Quick Start

Run the wizard in your project directory:

```bash
cd /path/to/your/mobile/project
mobilectl setup
```

Follow the interactive prompts through 8 phases to complete your configuration.

## The 8 Setup Phases

### Phase 1: Project Information

The wizard detects and confirms your project details:

```
1️⃣  PROJECT INFORMATION
────────────────────────────────────────────

📁 Project Type
Auto-detected: Flutter ✓

📝 App Name
[MyApp]:

📦 Package Name
Auto-detected: com.example.myapp ✓

📍 Current Version
[1.0.0]:
```

**Auto-detects:**
- Project type (Android, iOS, Flutter, React Native)
- App name from `pubspec.yaml` or build files
- Package/bundle identifier
- Current version

### Phase 2: Build Configuration

Configure build settings for each platform:

```
2️⃣  BUILD SETUP
────────────────────────────────────────────

🎯 Product Flavors
Detected from build.gradle.kts:
  ✓ free
  ✓ paid
  ✓ premium

🔐 Default Flavor
[1] free [2] paid [3] premium
> 1 ✓

🔒 Signing Configuration
Keystore path [release.jks]:
Key alias [release-key]:
```

**Configures:**
- Android flavors and build types
- Keystore and signing
- iOS scheme and configuration
- Code signing identity

### Phase 3: Deployment Destinations

Set up where to deploy your builds:

```
3️⃣  DEPLOYMENT DESTINATIONS
────────────────────────────────────────────

🔥 Firebase App Distribution
Credentials found: credentials/firebase-adminsdk.json ✓
Enable? (Y/n) y ✓
Test groups: qa-team, beta-testers ✓

🎮 Google Play Console
Enable? (Y/n) y ✓

✈️ TestFlight
Enable? (Y/n) y ✓
Team ID: ABC123DEF ✓
```

**Supports:**
- Firebase App Distribution
- Google Play Console
- Apple TestFlight
- App Store
- Local filesystem

### Phase 4: Version Management

Configure automatic version bumping:

```
4️⃣  VERSION MANAGEMENT
────────────────────────────────────────────

📌 Auto-Increment on Deploy
Automatically bump version? (Y/n) y ✓

Default strategy:
[1] patch [2] minor [3] major [4] auto
> 1 ✓

📄 Files to Update
  ✓ pubspec.yaml
  ✓ app/build.gradle.kts
```

**Features:**
- Semantic versioning
- Multi-file updates
- Automatic backup
- Custom bump strategies

### Phase 5: Changelog

Configure changelog generation:

```
5️⃣  CHANGELOG
────────────────────────────────────────────

📝 Generate Changelogs
Generate on deploy? (Y/n) y ✓

Format: [1] markdown [2] html [3] json
> 1 ✓

Output file [CHANGELOG.md]:

Append to existing? (Y/n) y ✓
```

**Options:**
- Markdown, HTML, or JSON format
- Auto-generate from commits
- Conventional commit parsing
- Breaking changes detection

### Phase 6: Deployment Groups

Create flavor groups for batch deployment:

```
6️⃣  DEPLOYMENT GROUPS
────────────────────────────────────────────

🎯 Create Flavor Groups

Production Release
  Flavors: free, paid, premium ✓

QA Testing
  Flavors: debug ✓
```

**Benefits:**
- Deploy multiple flavors at once
- Group related builds
- Simplify CI/CD

::: tip
Deploy all production flavors with: `mobilectl deploy --group production`
:::

### Phase 7: CI/CD Setup

Generate workflow files for your CI/CD platform:

```
7️⃣  CI/CD SETUP
────────────────────────────────────────────

🤖 GitHub Actions
Generate example workflow? (Y/n) y ✓

Triggers:
  ✓ On tag push (v*.*.*)
  ✓ Manual dispatch
  ✓ On PR (test only)

→ Workflow saved to .github/workflows/mobilectl-deploy.yml ✓
```

**Generates:**
- GitHub Actions workflow
- GitLab CI pipeline
- Tag-based triggers
- Manual dispatch
- Proper secret handling

### Phase 8: Review & Confirm

Review your configuration before generation:

```
8️⃣  REVIEW & CONFIRM
────────────────────────────────────────────

📋 Configuration Summary

Project: MyApp (com.example.myapp)
Build: 3 flavors (free, paid, premium)
Deploy: Firebase, Play Console, TestFlight
Version: Auto-increment (patch)
Changelog: Enabled (markdown)
CI/CD: GitHub Actions configured

Everything looks good? (Y/n) y ✓

✅ Configuration saved to: mobileops.yaml
🎉 Ready to deploy!
```

## Generated Files

The wizard creates these files:

```
project/
├── mobileops.yaml              # Complete configuration
├── .github/workflows/
│   └── mobilectl-deploy.yml   # GitHub Actions (optional)
├── .gitlab-ci.yml              # GitLab CI (optional)
└── docs/
    └── SETUP.md                # Setup summary
```

## Command Options

### Force Overwrite

Overwrite existing configuration:

```bash
mobilectl setup --force
```

Creates a backup before overwriting:
```
⚠ Backing up existing config
✓ Backup created: mobileops.backup.20251107_123456.yaml
```

### Custom Output Path

Generate configuration to a different location:

```bash
mobilectl setup --output custom-config.yaml
```

## Auto-Detection

The wizard intelligently detects your project configuration:

### Project Type

| Detection Method | Project Type |
|-----------------|-------------|
| `pubspec.yaml` + `lib/` | Flutter |
| `package.json` with `react-native` | React Native |
| `build.gradle.kts` | Android Native |
| `.xcodeproj` or `.xcworkspace` | iOS Native |

### Credentials

Searches for credentials in common locations:

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
```

**App Store Connect:**
```
credentials/app-store-connect-api-key.json
credentials/appstore-api-key.json
```

### Build Configuration

**Android Flavors:**
- Parses `build.gradle.kts` or `build.gradle`
- Extracts `productFlavors` block
- Detects flavor names automatically

**iOS Configuration:**
- Finds `.xcworkspace` or `.xcodeproj`
- Detects scheme names
- Locates `Info.plist` files

## Example Configurations

### Flutter Project

```yaml
app:
  name: MyFlutterApp
  identifier: com.example.flutter
  version: 1.0.0

build:
  android:
    enabled: true
    default_flavor: production
    default_type: release
  ios:
    enabled: true
    project_path: ios/Runner.xcworkspace
    scheme: Runner

deploy:
  android:
    firebase:
      enabled: true
      service_account: credentials/firebase-adminsdk.json
      test_groups: [qa-team, beta-testers]
  ios:
    testflight:
      enabled: true
      api_key_path: credentials/app-store-connect-api-key.json
      team_id: ABC123DEF

version:
  enabled: true
  auto_increment: true
  bump_strategy: patch
  files_to_update:
    - pubspec.yaml

changelog:
  enabled: true
  format: markdown
  output_file: CHANGELOG.md
```

### Android Native

```yaml
app:
  name: MyAndroidApp
  identifier: com.example.android
  version: 1.0.0

build:
  android:
    enabled: true
    flavors: [free, paid, premium]
    default_flavor: free
    default_type: release
    key_store: release.jks
    key_alias: release-key
    key_password: ${ANDROID_KEY_PASSWORD}
    store_password: ${ANDROID_STORE_PASSWORD}

deploy:
  android:
    firebase:
      enabled: true
    play_console:
      enabled: true
      package_name: com.example.android

  flavor_groups:
    production:
      name: production
      flavors: [free, paid, premium]
```

## Next Steps

After running the setup wizard:

### 1. Set Environment Variables

For Android signing:

```bash
export ANDROID_KEY_PASSWORD=your-key-password
export ANDROID_STORE_PASSWORD=your-store-password
```

Add to `~/.bashrc` or `~/.zshrc` for persistence.

### 2. Build Your App

```bash
mobilectl build
```

### 3. Deploy

```bash
mobilectl deploy --all-variants
```

### 4. Configure CI/CD Secrets

If you generated GitHub Actions:

1. Go to repository **Settings → Secrets**
2. Add required secrets:
   - `ANDROID_KEY_PASSWORD`
   - `ANDROID_STORE_PASSWORD`
   - `FIREBASE_SERVICE_ACCOUNT` (paste JSON content)

### 5. Create a Release Tag

Trigger your CI/CD pipeline:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Troubleshooting

### Config Already Exists

```
⚠ Configuration file already exists: mobileops.yaml

Options:
  1. Overwrite (backup will be created)
  2. Cancel
  3. Specify different output path
```

**Solution:** Choose option 1 or use `--force` flag.

### Credentials Not Found

If credentials aren't auto-detected:

```
🔥 Firebase App Distribution
No credentials found.
Enable? (y/n) y
Service account JSON path:
```

**Solution:** Provide the path manually or place files in `credentials/` directory.

### No Flavors Detected

```
🎯 Product Flavors
No flavors detected.

Add flavors? (y/n) y
Enter flavor names (comma-separated):
```

**Solution:** Enter flavors manually: `free, paid, premium`

### Permission Denied

```
❌ Error: Failed to save configuration: Permission denied
```

**Solution:** Check file permissions or use custom output path:
```bash
mobilectl setup --output ~/configs/mobileops.yaml
```

## Tips & Best Practices

### 1. Keep Credentials Secure

```bash
# Add to .gitignore
echo "credentials/" >> .gitignore
echo "*.jks" >> .gitignore
echo "*.keystore" >> .gitignore
```

Never commit credentials to version control!

### 2. Use Environment Variables

```yaml
# Good ✓
key_password: ${ANDROID_KEY_PASSWORD}

# Bad ✗
key_password: "actual_password_123"
```

### 3. Use Deployment Groups

```bash
# Instead of:
mobilectl deploy android free
mobilectl deploy android paid
mobilectl deploy android premium

# Use:
mobilectl deploy --group production
```

### 4. Re-run to Update

Need to change configuration?

```bash
# Backup current config
cp mobileops.yaml mobileops.yaml.bak

# Re-run setup
mobilectl setup --force

# Compare and merge
diff mobileops.yaml mobileops.yaml.bak
```

## Related Documentation

- [Configuration Reference](/reference/configuration) - All config options
- [Build Command](/reference/build) - Building your app
- [Deploy Command](/reference/deploy) - Deployment strategies
- [CI/CD Integration](/guide/ci-cd) - Automating with GitHub Actions
- [Getting Started](/guide/getting-started) - Complete beginner's guide

## Common Questions

**Q: Can I run the wizard multiple times?**

Yes! Use `--force` to overwrite. A backup is created automatically.

**Q: What if I don't know my Team ID?**

Find it in App Store Connect → Membership → Team ID

**Q: Can I skip some phases?**

Yes! You can skip optional features by answering "no" to prompts.

**Q: Where should I put credential files?**

Create a `credentials/` directory in your project root:
```
project/
├── credentials/
│   ├── firebase-adminsdk.json
│   ├── play-console.json
│   └── app-store-connect-api-key.json
└── mobileops.yaml
```

**Q: Can I edit the generated config manually?**

Absolutely! The generated `mobileops.yaml` is meant to be customized.

---

::: tip Ready to Deploy?
After setup, you're ready to build and deploy:
```bash
mobilectl build
mobilectl deploy --all-variants
```
:::
