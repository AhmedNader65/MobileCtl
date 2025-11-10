# Advanced Examples

Complex scenarios, flavor management, and enterprise setups.

## Flavor Groups Management

### Basic Flavor Groups

Organize your flavors into logical groups for easier deployment:

```yaml
deploy:
  enabled: true
  default_group: production  # Used when no CLI flag specified

  flavorGroups:
    production:
      name: Production
      description: Production builds for app stores
      flavors:
        - productionRelease

    testing:
      name: Testing
      description: QA and internal testing
      flavors:
        - staging
        - development
        - qa

    freemium:
      name: Freemium Tiers
      description: Free and paid versions
      flavors:
        - free
        - paid
```

```bash
# Deploy using default group
mobilectl deploy

# Deploy specific group
mobilectl deploy --flavor-group testing
mobilectl deploy -G freemium
```

### Multi-Environment Flavor Groups

Complex flavor structure with multiple environments:

```yaml
build:
  android:
    enabled: true
    flavors:
      - productionRelease
      - productionDebug
      - stagingRelease
      - stagingDebug
      - developmentRelease
      - developmentDebug
      - qaRelease
      - qaDebug

deploy:
  default_group: production

  flavorGroups:
    # Environment-based groups
    production:
      name: Production
      description: Production releases only
      flavors:
        - productionRelease

    staging:
      name: Staging
      description: Staging environment
      flavors:
        - stagingRelease
        - stagingDebug

    development:
      name: Development
      description: Development environment
      flavors:
        - developmentRelease
        - developmentDebug

    qa:
      name: QA
      description: QA testing environment
      flavors:
        - qaRelease
        - qaDebug

    # Purpose-based groups
    all-releases:
      name: All Releases
      description: All release builds across environments
      flavors:
        - productionRelease
        - stagingRelease
        - developmentRelease
        - qaRelease

    all-debug:
      name: All Debug
      description: All debug builds for development
      flavors:
        - productionDebug
        - stagingDebug
        - developmentDebug
        - qaDebug

    internal-testing:
      name: Internal Testing
      description: Builds for internal testing
      flavors:
        - stagingRelease
        - developmentDebug
        - qaRelease

  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase.json
      testGroups:
        - qa-team
        - beta-testers
        - internal

    playConsole:
      enabled: true
      serviceAccount: credentials/play-console.json
      packageName: com.example.app
```

**Usage examples:**

```bash
# Deploy production (uses default_group)
mobilectl deploy

# Deploy to staging environment
mobilectl deploy -G staging

# Deploy all release builds
mobilectl deploy --flavor-group all-releases

# Deploy for internal testing
mobilectl deploy --flavor-group internal-testing

# Deploy all flavors with version bump
mobilectl deploy --all-flavors --bump-version minor --changelog
```

### App Variant Groups (Free/Paid/Premium)

Manage different app tiers and monetization models:

```yaml
build:
  android:
    enabled: true
    flavors:
      - free
      - paid
      - premium
      - enterprise
      - freeTrial
      - educational

deploy:
  default_group: public-releases

  flavorGroups:
    public-releases:
      name: Public Releases
      description: Consumer-facing app variants
      flavors:
        - free
        - paid
        - premium

    premium-tiers:
      name: Premium Tiers
      description: Paid versions only
      flavors:
        - paid
        - premium
        - enterprise

    special-editions:
      name: Special Editions
      description: Trial and educational versions
      flavors:
        - freeTrial
        - educational

    complete-lineup:
      name: Complete Lineup
      description: All app variants
      flavors:
        - free
        - paid
        - premium
        - enterprise
        - freeTrial
        - educational

    store-releases:
      name: Store Releases
      description: Versions for app stores only
      flavors:
        - free
        - paid
        - premium
        - enterprise

  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase.json

    playConsole:
      enabled: true
      serviceAccount: credentials/play-console.json
```

**Deployment strategies:**

```bash
# Deploy public versions to stores
mobilectl deploy -G public-releases

# Deploy all premium tiers for testing
mobilectl deploy --flavor-group premium-tiers

# Deploy special editions to beta testers
mobilectl deploy --flavor-group special-editions

# Full deployment of all variants
mobilectl deploy --flavor-group complete-lineup --confirm
```

### Regional/Localized Flavor Groups

Manage region-specific app builds:

```yaml
build:
  android:
    flavors:
      - global
      - northAmerica
      - europe
      - asia
      - latam
      - mena

deploy:
  default_group: global

  flavorGroups:
    global:
      name: Global
      description: Worldwide release
      flavors:
        - global

    western-markets:
      name: Western Markets
      description: North America and Europe
      flavors:
        - northAmerica
        - europe

    eastern-markets:
      name: Eastern Markets
      description: Asia and MENA
      flavors:
        - asia
        - mena

    emerging-markets:
      name: Emerging Markets
      description: Latin America and MENA
      flavors:
        - latam
        - mena

    all-regions:
      name: All Regions
      description: All regional variants
      flavors:
        - global
        - northAmerica
        - europe
        - asia
        - latam
        - mena
```

```bash
# Deploy to western markets first
mobilectl deploy -G western-markets

# Then eastern markets
mobilectl deploy -G eastern-markets

# Finally, emerging markets
mobilectl deploy -G emerging-markets

# Or deploy everywhere at once
mobilectl deploy --all-flavors
```

## Advanced Deployment Workflows

### Staged Rollout Workflow

```bash
# Phase 1: Deploy to internal testing
mobilectl deploy \
  --flavor-group internal-testing \
  --bump-version patch \
  --notes "Release candidate for v1.2.1"

# Phase 2: Deploy to beta testers (after QA approval)
mobilectl deploy \
  --flavor-group staging \
  --notes "Beta release v1.2.1"

# Phase 3: Deploy to production (after beta testing)
mobilectl deploy \
  --flavor-group production \
  --changelog \
  --confirm
```

### Selective Deployment with Exclusions

```bash
# Deploy all flavors except development and qa
mobilectl deploy \
  --all-flavors \
  --exclude development,qa

# Deploy production group but exclude debug builds
mobilectl deploy \
  --flavor-group production \
  --exclude productionDebug

# Deploy everything except specific flavors
mobilectl deploy -A -E freeTrial,educational
```

### Combined Flavor and Version Management

```bash
# Release workflow: bump + changelog + deploy group
mobilectl deploy \
  --flavor-group production \
  --bump-version minor \
  --changelog \
  --confirm

# Hotfix workflow: patch + specific flavor
mobilectl deploy \
  --flavors productionRelease \
  --bump-version patch \
  --notes "Critical security fix" \
  -y

# Beta workflow: prerelease + testing group
mobilectl deploy \
  --flavor-group internal-testing \
  --bump-version prerelease \
  --notes "Internal beta build"
```

## Enterprise Setup

### Complete Enterprise Configuration

```yaml
app:
  name: EnterpriseApp
  identifier: com.enterprise.app
  version: 2.0.0

build:
  android:
    enabled: true
    flavors:
      - productionRelease
      - stagingRelease
      - developmentDebug
      - qaRelease
    defaultFlavor: productionRelease
    defaultType: release

    # Secure credential management
    keyStore: ${ANDROID_KEYSTORE_PATH}
    keyAlias: ${ANDROID_KEY_ALIAS}
    keyPassword: ${ANDROID_KEY_PASSWORD}
    storePassword: ${ANDROID_STORE_PASSWORD}

  ios:
    enabled: true
    scheme: EnterpriseApp
    configuration: Release
    codeSignIdentity: ${IOS_CODE_SIGN_IDENTITY}
    provisioningProfile: ${IOS_PROVISIONING_PROFILE}

version:
  enabled: true
  current: 2.0.0
  autoIncrement: true
  bumpStrategy: patch
  filesToUpdate:
    - version.properties
    - package.json
    - android/app/build.gradle
    - ios/EnterpriseApp/Info.plist

changelog:
  enabled: true
  format: markdown
  outputFile: CHANGELOG.md
  includeBreakingChanges: true
  includeContributors: true
  includeStats: true

deploy:
  enabled: true
  default_group: production

  flavorGroups:
    production:
      name: Production
      description: Production releases for app stores
      flavors:
        - productionRelease

    internal:
      name: Internal
      description: Internal testing and QA
      flavors:
        - stagingRelease
        - developmentDebug
        - qaRelease

  android:
    enabled: true
    artifactPath: build/outputs/bundle/release/app-release.aab

    firebase:
      enabled: true
      serviceAccount: ${FIREBASE_SERVICE_ACCOUNT}
      googleServices: android/app/google-services.json
      releaseNotes: "Enterprise release ${VERSION}"
      testGroups:
        - engineering-team
        - qa-team
        - beta-testers
        - stakeholders

    playConsole:
      enabled: true
      serviceAccount: ${PLAY_CONSOLE_SERVICE_ACCOUNT}
      packageName: com.enterprise.app
      track: internal
      status: draft

    local:
      enabled: true
      outputDir: builds/android/

  ios:
    enabled: true
    artifactPath: build/outputs/ipa/EnterpriseApp.ipa

    testflight:
      enabled: true
      apiKeyPath: ${APP_STORE_CONNECT_API_KEY}
      bundleId: com.enterprise.app
      teamId: ${APPLE_TEAM_ID}

    appStore:
      enabled: false
      apiKeyPath: ${APP_STORE_CONNECT_API_KEY}
      bundleId: com.enterprise.app
      teamId: ${APPLE_TEAM_ID}

notify:
  slack:
    enabled: true
    webhookUrl: ${SLACK_WEBHOOK_URL}
    channel: "#deployments"
    notifyOn:
      - success
      - failure

  email:
    enabled: true
    recipients:
      - engineering@enterprise.com
      - qa@enterprise.com
      - product@enterprise.com
    notifyOn:
      - success
      - failure

  webhook:
    enabled: true
    url: https://api.enterprise.com/v1/webhooks/deployments
    events:
      - build_started
      - build_completed
      - deploy_started
      - deploy_completed
      - deploy_failed

report:
  enabled: true
  format: html
  include:
    - build_info
    - git_info
    - build_duration
    - artifact_size
    - test_results
  outputPath: ./build-reports
```

### Enterprise Deployment Commands

```bash
# Production release with full workflow
mobilectl deploy \
  --flavor-group production \
  --bump-version minor \
  --changelog \
  --confirm

# Internal testing deployment
mobilectl deploy \
  --flavor-group internal \
  --notes "Internal build for sprint 42"

# Emergency hotfix
mobilectl deploy \
  --flavors productionRelease \
  --bump-version patch \
  --notes "HOTFIX: Critical security vulnerability" \
  --confirm

# Comprehensive deployment with all options
mobilectl deploy \
  --all-flavors \
  --bump-version major \
  --changelog \
  --notes "Major release v2.0.0" \
  --confirm \
  --verbose
```

## Multi-Platform Advanced Setup

### Flutter with Flavor Groups

```yaml
app:
  name: FlutterApp
  identifier: com.example.flutter

build:
  android:
    enabled: true
    flavors:
      - dev
      - staging
      - production
  ios:
    enabled: true
    scheme: Runner

deploy:
  default_group: production

  flavorGroups:
    production:
      name: Production
      flavors:
        - production
    testing:
      name: Testing
      flavors:
        - dev
        - staging

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
# Deploy to both platforms with testing group
mobilectl deploy --flavor-group testing

# Deploy production to both platforms
mobilectl deploy -G production --bump-version patch --changelog
```

## CI/CD Integration

### GitHub Actions with Flavor Groups

```yaml
name: Deploy

on:
  push:
    branches: [main, staging, develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Deploy based on branch
        run: |
          if [ "${{ github.ref }}" == "refs/heads/main" ]; then
            mobilectl deploy --flavor-group production --confirm
          elif [ "${{ github.ref }}" == "refs/heads/staging" ]; then
            mobilectl deploy --flavor-group staging
          else
            mobilectl deploy --flavor-group development
          fi
```

## See Also

- [Examples Overview →](/examples/)
- [Deployment Guide →](/guide/deployment)
- [Deploy Configuration →](/reference/config-deploy)
- [CI/CD Guide →](/guide/ci-cd)
