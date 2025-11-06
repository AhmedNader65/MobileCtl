# Examples

Real-world examples and use cases for MobileCtl.

## Overview

This section provides practical examples of using MobileCtl in various scenarios. Each example includes complete configuration and command examples.

## Quick Examples

### Simple Flutter App

```yaml
# mobileops.yaml
app:
  name: MyFlutterApp
  identifier: com.example.flutter
  version: 1.0.0

build:
  android:
    enabled: true
    defaultType: release
  ios:
    enabled: true
    scheme: Runner

version:
  enabled: true
  current: 1.0.0
  filesToUpdate:
    - pubspec.yaml

changelog:
  enabled: true
```

```bash
# Build for both platforms
mobilectl build all

# Bump version and generate changelog
mobilectl version bump patch
mobilectl changelog generate

# Deploy to Firebase
mobilectl deploy firebase
```

### React Native App

```yaml
# mobileops.yaml
app:
  name: MyReactNativeApp
  identifier: com.example.rn
  version: 1.0.0

build:
  android:
    enabled: true
    flavors: [production, staging]
  ios:
    enabled: true
    scheme: MyApp

version:
  filesToUpdate:
    - package.json
    - android/app/build.gradle
    - ios/MyApp/Info.plist

deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase.json
  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey.p8
      teamId: ABC123
```

```bash
# Complete release workflow
mobilectl deploy \
  --bump-version minor \
  --changelog \
  --all-flavors
```

## Example Categories

### By Platform

- [Android App →](/examples/android)
  - Native Android with multiple flavors
  - Firebase deployment
  - Play Console integration

- [iOS App →](/examples/ios)
  - Native iOS with Xcode schemes
  - TestFlight deployment
  - App Store submission

- [Multi-Platform →](/examples/multi-platform)
  - Flutter/React Native apps
  - Shared configuration
  - Cross-platform deployment

### By Use Case

- [CI/CD Workflows →](/examples/ci-cd)
  - GitHub Actions
  - GitLab CI
  - Jenkins
  - Manual scripts

- [Advanced Scenarios →](/examples/advanced)
  - Multiple environments
  - Flavor groups
  - Custom workflows
  - Enterprise deployments

## Common Workflows

### Development Workflow

```bash
# Daily development
mobilectl build android staging debug

# Test release build
mobilectl build android staging release --dry-run
mobilectl build android staging release

# Deploy to testers
mobilectl deploy firebase --flavors staging
```

### Release Workflow

```bash
# 1. Update version
mobilectl version bump minor

# 2. Generate changelog
mobilectl changelog generate

# 3. Build production
mobilectl build all

# 4. Deploy
mobilectl deploy --all-flavors --confirm

# 5. Commit and tag
git add .
git commit -m "chore: release v1.1.0"
git tag v1.1.0
git push --tags
```

### Hotfix Workflow

```bash
# 1. Bump patch version
mobilectl version bump patch

# 2. Build and deploy immediately
mobilectl deploy \
  --flavors production \
  --notes "Critical bug fix" \
  --confirm
```

## Platform-Specific Examples

### Android Only

```yaml
app:
  name: AndroidApp

build:
  android:
    enabled: true
    flavors:
      - production
      - staging
      - development
    defaultType: release
    keyStore: release.keystore
    keyAlias: my-key
    keyPassword: ${KEY_PASSWORD}

  ios:
    enabled: false

deploy:
  android:
    firebase:
      enabled: true
      testGroups: [qa, beta]
    playConsole:
      enabled: true
      packageName: com.example.android
```

### iOS Only

```yaml
app:
  name: iOSApp

build:
  android:
    enabled: false

  ios:
    enabled: true
    scheme: MyApp
    configuration: Release
    codeSignIdentity: "iPhone Distribution"

deploy:
  ios:
    testflight:
      enabled: true
      bundleId: com.example.ios
      teamId: ABC123
```

## Environment Examples

### Development Environment

```yaml
# mobileops.dev.yaml
app:
  name: MyApp (Dev)

build:
  android:
    defaultFlavor: development
    defaultType: debug

deploy:
  android:
    firebase:
      testGroups: [developers]
    playConsole:
      enabled: false
```

```bash
mobilectl --config mobileops.dev.yaml build android
```

### Staging Environment

```yaml
# mobileops.staging.yaml
app:
  name: MyApp (Staging)

build:
  android:
    defaultFlavor: staging
    defaultType: release

deploy:
  android:
    firebase:
      testGroups: [qa-team, stakeholders]
```

### Production Environment

```yaml
# mobileops.prod.yaml
app:
  name: MyApp

build:
  android:
    defaultFlavor: production
    defaultType: release

deploy:
  android:
    firebase:
      testGroups: [beta-testers]
    playConsole:
      enabled: true
```

## Team Scenarios

### Solo Developer

```yaml
# Simple setup for one developer
app:
  name: SoloApp
  version: 1.0.0

build:
  android:
    enabled: true
  ios:
    enabled: true

deploy:
  android:
    local:
      enabled: true
      outputDir: releases/
```

```bash
# Quick build and save locally
mobilectl build all
mobilectl deploy local
```

### Small Team

```yaml
# Shared config for small team
app:
  name: TeamApp

build:
  android:
    flavors: [production, staging]

deploy:
  android:
    firebase:
      enabled: true
      testGroups: [team, qa]

notify:
  slack:
    enabled: true
    webhookUrl: ${SLACK_WEBHOOK}
```

### Enterprise Team

```yaml
# Enterprise setup with multiple environments
app:
  name: EnterpriseApp

build:
  android:
    flavors:
      - productionRelease
      - productionDebug
      - stagingRelease
      - stagingDebug
      - developmentDebug

deploy:
  flavorGroups:
    production:
      flavors: [productionRelease]
    internal:
      flavors: [stagingRelease, developmentDebug]

  android:
    firebase:
      enabled: true
    playConsole:
      enabled: true

notify:
  slack:
    enabled: true
  email:
    enabled: true
    recipients:
      - dev-team@company.com
      - qa-team@company.com
  webhook:
    enabled: true
    url: https://api.company.com/deploys
```

## Integration Examples

### With Git Hooks

```bash
# .git/hooks/pre-push
#!/bin/bash
echo "Running pre-push checks..."

# Build to ensure no errors
mobilectl build all --dry-run

# Generate changelog
mobilectl changelog generate --dry-run
```

### With npm Scripts

```json
{
  "scripts": {
    "build:android": "mobilectl build android",
    "build:ios": "mobilectl build ios",
    "build:all": "mobilectl build all",
    "deploy:staging": "mobilectl deploy --flavors staging",
    "deploy:prod": "mobilectl deploy --flavors production",
    "release": "mobilectl deploy --bump-version minor --changelog --all-flavors"
  }
}
```

```bash
npm run build:all
npm run deploy:staging
npm run release
```

### With Make

```makefile
# Makefile
.PHONY: build deploy release

build:
\tmobilectl build all

deploy-staging:
\tmobilectl deploy --flavors staging

deploy-prod:
\tmobilectl deploy --flavors production --confirm

release:
\tmobilectl version bump minor
\tmobilectl changelog generate
\tmobilectl build all
\tmobilectl deploy --all-flavors --confirm
```

```bash
make build
make deploy-staging
make release
```

## Testing Examples

### Dry Run Everything

```bash
# Test build configuration
mobilectl build android --dry-run

# Test version bump
mobilectl version bump patch --dry-run

# Test changelog
mobilectl changelog generate --dry-run

# Test deployment
mobilectl deploy --dry-run
```

### Verbose Debugging

```bash
# Detailed output for troubleshooting
mobilectl build android --verbose
mobilectl deploy firebase --verbose
```

## Advanced Configuration

### Multi-Flavor Multi-Environment

```yaml
deploy:
  flavorGroups:
    # Production variants
    prod-all:
      flavors:
        - productionRelease
        - productionDebug

    # Staging variants
    staging-all:
      flavors:
        - stagingRelease
        - stagingDebug

    # Development variants
    dev-all:
      flavors:
        - developmentRelease
        - developmentDebug

    # Testing group
    testing:
      flavors:
        - stagingRelease
        - developmentDebug
```

```bash
mobilectl deploy --flavor-group prod-all
mobilectl deploy --flavor-group testing
```

### Custom Release Notes

```yaml
deploy:
  android:
    firebase:
      releaseNotes: |
        🚀 Release ${VERSION}

        What's New:
        - Feature A
        - Feature B

        Bug Fixes:
        - Fixed issue X
        - Fixed issue Y

        Build Info:
        - Build: ${BUILD_NUMBER}
        - Date: ${BUILD_DATE}
        - Branch: ${GIT_BRANCH}
```

## Next Steps

Explore detailed examples:

- [Android Examples →](/examples/android)
- [iOS Examples →](/examples/ios)
- [Multi-Platform Examples →](/examples/multi-platform)
- [CI/CD Examples →](/examples/ci-cd)
- [Advanced Scenarios →](/examples/advanced)

Or check out:

- [Command Reference →](/reference/commands)
- [Configuration Guide →](/guide/configuration)
- [Getting Started →](/guide/getting-started)
