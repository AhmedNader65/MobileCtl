# Deployment

Deploy your apps to Firebase, TestFlight, and app stores with intelligent flavor management.

## Overview

MobileCtl supports multiple deployment destinations for both Android and iOS, with powerful flavor group management for deploying different app variants.

## Basic Deployment

```bash
# Deploy with default configuration
mobilectl deploy

# Deploy to specific destination
mobilectl deploy firebase
mobilectl deploy testflight

# Interactive mode
mobilectl deploy --interactive
```

## Flavor Management

### Deploy All Flavors

Deploy all configured flavors at once:

```bash
mobilectl deploy --all-flavors
```

### Deploy Specific Flavor Group

Define flavor groups in your config for organized deployments:

```yaml
deploy:
  default_group: production  # Used when no flag specified

  flavor_groups:
    production:
      name: Production
      description: Production builds for release
      flavors:
        - free
        - paid
        - premium

    testing:
      name: Testing
      description: Internal testing builds
      flavors:
        - qa
        - staging
```

Then deploy specific groups:

```bash
# Deploy production group (or use default_group)
mobilectl deploy
mobilectl deploy --flavor-group production
mobilectl deploy -G production

# Deploy testing group
mobilectl deploy --flavor-group testing
```

### Deploy Specific Flavors

Deploy only specific flavors:

```bash
# Deploy only free and paid flavors
mobilectl deploy --flavors free,paid
mobilectl deploy -f free,paid
```

### Exclude Flavors

Deploy all flavors except specific ones:

```bash
# Deploy all except qa and staging
mobilectl deploy --all-flavors --exclude qa,staging
mobilectl deploy -A -E qa,staging
```

## Complete Release Workflow

Combine deployment with version bumping and changelog generation:

```bash
# Full release workflow
mobilectl deploy \
  --bump-version patch \
  --changelog \
  --flavor-group production \
  --confirm

# Deploy all flavors with version bump
mobilectl deploy \
  --all-flavors \
  --bump-version minor \
  -C \
  -y
```

## Supported Destinations

### Android
- **Firebase App Distribution** - Beta testing and QA
- **Google Play Console** - Production releases
- **Local filesystem** - Build archives

### iOS
- **TestFlight** - Beta testing
- **App Store** - Production releases
- **Local filesystem** - Build archives

## Configuration

### Basic Configuration

```yaml
deploy:
  enabled: true

  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase.json
      testGroups:
        - qa-team
        - beta-testers

  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey.p8
      bundleId: com.example.app
      teamId: ABC123DEF
```

### Advanced Configuration with Flavor Groups

```yaml
deploy:
  enabled: true
  default_group: production

  flavor_groups:
    production:
      name: Production
      description: All production app variants
      flavors: [free, paid, premium, enterprise]

    testing:
      name: Testing
      description: QA and staging builds
      flavors: [qa, staging, dev]

    freemium:
      name: Freemium
      description: Free and paid tiers only
      flavors: [free, paid]

  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase-service-account.json
      releaseNotes: "New version available for testing"
      testGroups: [qa-team, beta-testers]

    playConsole:
      enabled: true
      serviceAccount: credentials/play-console.json
      packageName: com.example.myapp

  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey_ABC123.p8
      bundleId: com.example.myapp
      teamId: XYZ123

    appStore:
      enabled: false
      apiKeyPath: credentials/AuthKey_ABC123.p8
      bundleId: com.example.myapp
      teamId: XYZ123
```

## Common Workflows

### QA Testing Workflow

```bash
# Build and deploy testing flavors to Firebase
mobilectl deploy --flavor-group testing
```

### Production Release Workflow

```bash
# Deploy production builds with version bump
mobilectl deploy \
  --flavor-group production \
  --bump-version minor \
  --changelog \
  --confirm
```

### Hotfix Workflow

```bash
# Quick patch deployment
mobilectl deploy \
  --flavors production \
  --bump-version patch \
  --notes "Critical bug fix" \
  -y
```

### Multi-Destination Deployment

```bash
# Deploy to multiple destinations (configured in YAML)
mobilectl deploy --all-flavors

# This will automatically deploy to all enabled destinations:
# - Firebase App Distribution
# - Google Play Console
# - TestFlight
```

## Deployment Options

| Option | Short | Description |
|--------|-------|-------------|
| `--all-flavors` | `-A` | Deploy all configured flavors |
| `--flavor-group <name>` | `-G` | Deploy specific flavor group |
| `--flavors <list>` | `-f` | Deploy specific flavors (comma-separated) |
| `--exclude <list>` | `-E` | Exclude specific flavors |
| `--bump-version <level>` | `-B` | Bump version before deploy (major, minor, patch) |
| `--changelog` | `-C` | Generate changelog before deploy |
| `--confirm` | `-y` | Skip confirmation prompt |
| `--skip-build` | | Deploy existing artifacts without building |
| `--dry-run` | | Show what would be deployed without deploying |
| `--interactive` | `-i` | Interactive mode for selecting options |

## See Also

- [Deploy Command Reference →](/reference/deploy)
- [Deploy Configuration →](/reference/config-deploy)
- [CI/CD Integration →](/guide/ci-cd)
- [Setup Wizard →](/guide/setup-wizard)
