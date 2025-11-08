# Configuration File

Complete reference for the `mobileops.yaml` configuration file.

## Overview

MobileCtl uses a single YAML configuration file called `mobileops.yaml` in your project root. This file controls all aspects of building, versioning, and deploying your mobile apps.

## File Location

MobileCtl looks for `mobileops.yaml` in:

1. Current directory
2. Parent directories (up to git root)
3. Custom path via `--config` flag

```bash
# Use custom config file
mobilectl --config custom-config.yaml build android
```

## Basic Structure

```yaml
# App metadata
app:
  name: MyApp
  identifier: com.example.myapp
  version: 1.0.0

# Build configuration
build:
  android: { }
  ios: { }

# Version management
version:
  enabled: true
  current: 1.0.0

# Changelog generation
changelog:
  enabled: true
  outputFile: CHANGELOG.md

# Deployment
deploy:
  android: { }
  ios: { }

# Notifications
notify:
  slack: { }
  email: { }

# Reporting
report:
  enabled: false

# Environment variables
env:
  KEY: value
```

## Configuration Sections

| Section | Description | Reference |
|---------|-------------|-----------|
| [`app`](/reference/config-app) | App metadata (name, identifier, version) | [Learn more →](/reference/config-app) |
| [`build`](/reference/config-build) | Build settings for Android and iOS | [Learn more →](/reference/config-build) |
| [`version`](/reference/config-version) | Version management configuration | [Learn more →](/reference/config-version) |
| [`changelog`](/reference/config-changelog) | Changelog generation settings | [Learn more →](/reference/config-changelog) |
| [`deploy`](/reference/config-deploy) | Deployment destinations and settings | [Learn more →](/reference/config-deploy) |
| [`notify`](/reference/config-notifications) | Notification configurations | [Learn more →](/reference/config-notifications) |
| `report` | Build and deploy reporting | [Learn more →](#report-configuration) |
| `env` | Environment variables | [Learn more →](#environment-variables) |

## Complete Example

```yaml
# mobileops.yaml - Complete configuration example

app:
  name: MyAwesomeApp
  identifier: com.example.awesome
  version: 1.0.0

build:
  android:
    enabled: true
    defaultFlavor: production
    defaultType: release
    flavors:
      - production
      - staging
      - development
    keyStore: release.keystore
    keyAlias: my-app-key
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}
    useEnvForPasswords: true

  ios:
    enabled: true
    projectPath: .
    scheme: Runner
    configuration: Release
    destination: generic/platform=iOS
    codeSignIdentity: "iPhone Distribution"
    provisioningProfile: "path/to/profile.mobileprovision"

version:
  enabled: true
  current: 1.0.0
  autoIncrement: false
  bumpStrategy: patch
  filesToUpdate:
    - pubspec.yaml
    - package.json
    - android/app/build.gradle
    - ios/Runner/Info.plist

changelog:
  enabled: true
  format: markdown
  outputFile: CHANGELOG.md
  fromTag: null
  append: true
  useLastState: true
  includeBreakingChanges: true
  includeContributors: true
  includeStats: true
  includeCompareLinks: true
  groupByVersion: true

  commitTypes:
    - type: feat
      title: Features
      emoji: ✨
    - type: fix
      title: Bug Fixes
      emoji: 🐛
    - type: docs
      title: Documentation
      emoji: 📚

  releases:
    1.0.0:
      highlights:
        - "Initial production release"
      breakingChanges: []
      contributors:
        - "John Doe <john@example.com>"

deploy:
  enabled: true

  android:
    enabled: true
    artifactPath: build/outputs/apk/release/app-release.apk

    firebase:
      enabled: true
      serviceAccount: credentials/firebase-service-account.json
      releaseNotes: "Automated upload from MobileCtl"
      testGroups:
        - qa-team
        - beta-testers

    playConsole:
      enabled: false
      serviceAccount: credentials/play-console-key.json
      packageName: com.example.awesome

    local:
      enabled: true
      outputDir: build/deploy

  ios:
    enabled: true
    artifactPath: build/outputs/ipa/release/app.ipa

    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey_ABC123.p8
      bundleId: com.example.awesome
      teamId: XYZ123

    appStore:
      enabled: false
      apiKeyPath: credentials/AuthKey_ABC123.p8
      bundleId: com.example.awesome
      teamId: XYZ123

  flavorGroups:
    production:
      name: Production
      description: Production builds
      flavors:
        - productionRelease
    testing:
      name: Testing
      description: Testing builds
      flavors:
        - staging
        - development

notify:
  slack:
    enabled: true
    webhookUrl: ${SLACK_WEBHOOK_URL}

  email:
    enabled: false
    recipients:
      - team@example.com

  webhook:
    enabled: false
    url: https://api.example.com/webhook

report:
  enabled: false
  format: html
  outputPath: ./build-reports

env:
  ENVIRONMENT: production
  API_URL: https://api.example.com
```

## Environment Variables

### Using Environment Variables

Reference environment variables in your config using `${VAR_NAME}` syntax:

```yaml
build:
  android:
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}

notify:
  slack:
    webhookUrl: ${SLACK_WEBHOOK_URL}

deploy:
  android:
    firebase:
      serviceAccount: ${FIREBASE_SERVICE_ACCOUNT_PATH}
```

### Setting Environment Variables

**In shell:**
```bash
export MOBILECTL_KEY_PASSWORD="your_password"
export MOBILECTL_STORE_PASSWORD="your_password"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/..."
```

**In .env file:**
```bash
# .env
MOBILECTL_KEY_PASSWORD=your_password
MOBILECTL_STORE_PASSWORD=your_password
SLACK_WEBHOOK_URL=https://hooks.slack.com/...
```

**In CI/CD:**
```yaml
# GitHub Actions
env:
  MOBILECTL_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  MOBILECTL_STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
```

### Custom Environment Variables

Define custom variables in the `env` section:

```yaml
env:
  API_URL: https://api.example.com
  ENVIRONMENT: production
  FEATURE_FLAG: "true"
```

Access in your app:
- Android: Via BuildConfig
- iOS: Via Info.plist or environment

## Report Configuration

Configure build and deployment reporting:

```yaml
report:
  enabled: true
  format: html        # html or json
  outputPath: ./build-reports
```

### HTML Report

Generates a beautiful HTML report:

```html
Build Report - MyAwesomeApp v1.0.1
===================================

Build Information:
  Platform: Android
  Flavor: production
  Type: release
  Duration: 1m 32s
  Status: ✓ Success

Deployment:
  Destination: Firebase
  URL: https://appdistribution.firebase.dev/i/abc123
  Test Groups: qa-team, beta-testers
  Status: ✓ Success
```

### JSON Report

Machine-readable format for processing:

```json
{
  "version": "1.0.1",
  "build": {
    "platform": "android",
    "flavor": "production",
    "type": "release",
    "duration": 92,
    "status": "success"
  },
  "deploy": {
    "destination": "firebase",
    "url": "https://appdistribution.firebase.dev/i/abc123",
    "status": "success"
  }
}
```

## Validation

MobileCtl validates your configuration on startup:

```bash
mobilectl build android
```

If errors are found:

```
Configuration Error: mobileops.yaml

  ✗ build.android.keyStore: File not found: release.keystore
    Suggestion: Create keystore or update path

  ✗ deploy.android.firebase.serviceAccount: Invalid JSON file
    Suggestion: Verify service account key is valid JSON

Fix these errors and try again.
```

## Configuration Defaults

MobileCtl provides sensible defaults for most settings:

```yaml
# These are the defaults if not specified

build:
  android:
    enabled: true
    defaultType: release
  ios:
    enabled: true
    configuration: Release

version:
  enabled: true
  autoIncrement: false
  bumpStrategy: patch

changelog:
  enabled: false
  format: markdown
  outputFile: CHANGELOG.md
  append: true

deploy:
  enabled: false

notify:
  slack:
    enabled: false
  email:
    enabled: false
  webhook:
    enabled: false

report:
  enabled: false
  format: html
```

## Configuration Inheritance

### Global Config

Create a global config in your home directory:

```bash
~/.mobilectl/config.yaml
```

Project config overrides global config.

### Environment-Specific Configs

```yaml
# mobileops.yaml - Base config
app:
  name: MyApp

# Override with environment
# mobileops.staging.yaml
app:
  name: MyApp (Staging)
```

Use with:
```bash
mobilectl --config mobileops.staging.yaml deploy
```

## Best Practices

### 1. Store Credentials Securely

```yaml
# ❌ Don't do this
build:
  android:
    keyPassword: "my_password_123"

# ✅ Do this
build:
  android:
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
```

### 2. Use Version Control

```bash
# Commit config to git
git add mobileops.yaml

# But not credentials
echo "credentials/" >> .gitignore
echo ".env" >> .gitignore
```

### 3. Document Custom Settings

```yaml
# Purpose: Production build configuration
# Author: DevOps Team
# Last Updated: 2024-01-15

app:
  name: MyApp
  # ... rest of config
```

### 4. Validate Before Commit

```bash
# Test config
mobilectl build android --dry-run

# Then commit
git add mobileops.yaml
git commit -m "chore: update build config"
```

### 5. Use Comments

```yaml
build:
  android:
    # Release keystore (stored in credentials/)
    keyStore: credentials/release.keystore

    # Password loaded from environment
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
```

## Migrating from Other Tools

### From Fastlane

```ruby
# Fastfile
lane :deploy do
  gradle(task: "bundleRelease")
  firebase_app_distribution(
    groups: "qa-team"
  )
end
```

Becomes:

```yaml
# mobileops.yaml
build:
  android:
    defaultType: release

deploy:
  android:
    firebase:
      enabled: true
      testGroups: [qa-team]
```

### From Manual Scripts

```bash
#!/bin/bash
./gradlew assembleRelease
firebase appdistribution:distribute \
  app-release.apk \
  --groups "qa-team"
```

Becomes:

```bash
mobilectl deploy firebase
```

## Troubleshooting

### Config File Not Found

```
Error: Configuration file not found: mobileops.yaml

Suggestions:
  1. Create mobileops.yaml in project root
  2. Or specify path: mobilectl --config path/to/config.yaml
```

### Invalid YAML

```
Error: Invalid YAML syntax in mobileops.yaml:3

  app:
    name MyApp  <- Missing colon

Suggestion: Fix YAML syntax error
```

### Environment Variable Not Set

```
Error: Environment variable not set: MOBILECTL_KEY_PASSWORD

Suggestion: Set environment variable:
  export MOBILECTL_KEY_PASSWORD="your_password"
```

## See Also

Detailed configuration references:

- [App Configuration →](/reference/config-app)
- [Build Configuration →](/reference/config-build)
- [Version Configuration →](/reference/config-version)
- [Changelog Configuration →](/reference/config-changelog)
- [Deploy Configuration →](/reference/config-deploy)
- [Notifications →](/reference/config-notifications)

Guides:

- [Getting Started →](/guide/getting-started)
- [Configuration Guide →](/guide/configuration)
- [Environment Variables →](/guide/environment)
