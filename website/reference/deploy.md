# deploy

Deploy your apps to Firebase, TestFlight, Play Console, and App Store with a single command.

## Synopsis

```bash
mobilectl deploy [platform] [destination] [options]
```

## Arguments

| Argument | Required | Description | Values |
|----------|----------|-------------|--------|
| `platform` | No | Target platform | `android`, `ios`, `all` |
| `destination` | No | Deployment destination | `firebase`, `testflight`, `play-console`, `app-store`, `local` |

## Options

### Basic Options

| Option | Short | Description |
|--------|-------|-------------|
| `--interactive` | `-i` | Interactive mode to choose platforms/destinations |
| `--verbose` | `-v` | Verbose output with detailed logs |
| `--dry-run` | | Preview deployment without executing |
| `--confirm` | `-y` | Skip confirmation prompts (for CI/CD) |

### Build Options

| Option | Description |
|--------|-------------|
| `--skip-build` | Skip building, use existing artifacts |
| `--bump-version <level>` | `-B` | Bump version before deploy (major/minor/patch/none) |
| `--changelog` | `-C` | Generate changelog before deploy |

### Flavor Options

| Option | Short | Description |
|--------|-------|-------------|
| `--all-flavors` | `-A` | Deploy all configured flavors |
| `--flavor-group <name>` | `-G` | Deploy specific flavor group |
| `--flavors <list>` | `-f` | Deploy specific flavors (comma-separated) |
| `--exclude <list>` | `-E` | Exclude specific flavors (comma-separated) |

### Deployment Options

| Option | Short | Description |
|--------|-------|-------------|
| `--env <environment>` | `-e` | Environment (dev/staging/production) |
| `--notes <text>` | `-n` | Release notes for deployment |
| `--groups <list>` | `-g` | Test groups (comma-separated) |

## Basic Usage

### Interactive Mode

The easiest way to deploy:

```bash
mobilectl deploy --interactive
```

Interactive wizard:
```
? Select platform: (Use arrow keys)
  ❯ Android
    iOS
    Both

? Select destination:
  ❯ Firebase App Distribution
    Google Play Console
    Local

? Select flavors:
  ❯ ◉ production
    ◯ staging
    ◯ development

? Bump version before deploy? (Y/n) y
? Version bump level: (Use arrow keys)
  ❯ patch
    minor
    major

? Generate changelog? (Y/n) y

? Proceed with deployment? (Y/n) y
```

### Quick Deploy

```bash
# Deploy to Firebase
mobilectl deploy firebase

# Deploy to TestFlight
mobilectl deploy testflight

# Deploy all flavors to all destinations
mobilectl deploy --all-flavors
```

### Complete Release

```bash
mobilectl deploy \
  --bump-version patch \
  --changelog \
  --all-flavors \
  --confirm
```

## Deployment Destinations

### Firebase App Distribution

Deploy Android and iOS apps to Firebase for testing.

```bash
# Android to Firebase
mobilectl deploy android firebase

# iOS to Firebase
mobilectl deploy ios firebase

# Both platforms
mobilectl deploy firebase
```

#### Configuration

```yaml
deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase-service-account.json
      releaseNotes: "Automated upload"
      testGroups:
        - qa-team
        - beta-testers
```

#### Setup Requirements

1. Firebase project created
2. Service account JSON key downloaded
3. Firebase App Distribution enabled

[Learn more →](/guide/deployment#firebase)

### TestFlight

Deploy iOS apps to Apple TestFlight.

```bash
mobilectl deploy testflight
```

#### Configuration

```yaml
deploy:
  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey_XXXXXXXX.p8
      bundleId: com.example.myapp
      teamId: XXXXXXXXXX
```

#### Setup Requirements

1. App Store Connect account
2. API key created and downloaded
3. App registered in App Store Connect

[Learn more →](/guide/deployment#testflight)

### Google Play Console

Deploy Android apps to Google Play Console.

```bash
mobilectl deploy play-console
```

#### Configuration

```yaml
deploy:
  android:
    playConsole:
      enabled: true
      serviceAccount: credentials/play-console-key.json
      packageName: com.example.myapp
```

#### Setup Requirements

1. Google Play Console account
2. Service account created
3. App registered in Play Console

[Learn more →](/guide/deployment#play-console)

### App Store

Deploy iOS apps to App Store.

```bash
mobilectl deploy app-store
```

#### Configuration

```yaml
deploy:
  ios:
    appStore:
      enabled: true
      apiKeyPath: credentials/AuthKey_XXXXXXXX.p8
      bundleId: com.example.myapp
      teamId: XXXXXXXXXX
```

[Learn more →](/guide/deployment#app-store)

### Local

Copy artifacts to local directory.

```bash
mobilectl deploy local
```

#### Configuration

```yaml
deploy:
  android:
    local:
      enabled: true
      outputDir: build/deploy
  ios:
    local:
      enabled: true
      outputDir: build/deploy
```

## Flavor Management

### All Flavors

Deploy all configured flavors:

```bash
mobilectl deploy --all-flavors
```

Example output:
```
Deploying all flavors...

[1/3] production
  ✓ Build successful
  ✓ Uploaded to Firebase
  ✓ Uploaded to TestFlight

[2/3] staging
  ✓ Build successful
  ✓ Uploaded to Firebase

[3/3] development
  ✓ Build successful
  ✓ Uploaded to Firebase

All deployments completed!
Success: 3/3
Duration: 8m 42s
```

### Flavor Groups

Define groups for easier deployment:

```yaml
deploy:
  flavorGroups:
    production:
      name: Production
      flavors:
        - productionRelease
    testing:
      name: Testing
      flavors:
        - staging
        - development
```

Deploy group:

```bash
mobilectl deploy --flavor-group production
mobilectl deploy --flavor-group testing
```

### Specific Flavors

```bash
# Deploy specific flavors
mobilectl deploy --flavors production,staging

# Exclude flavors
mobilectl deploy --all-flavors --exclude development
```

## Version Bumping

### Automatic Version Bump

```bash
# Bump patch version before deploy
mobilectl deploy --bump-version patch

# Bump minor version
mobilectl deploy --bump-version minor

# Bump major version
mobilectl deploy --bump-version major
```

### With Changelog

```bash
# Bump version and generate changelog
mobilectl deploy --bump-version patch --changelog
```

This will:
1. Bump version (e.g., 1.0.0 → 1.0.1)
2. Generate changelog from commits
3. Build artifacts with new version
4. Deploy to configured destinations

## Release Notes

### Inline Notes

```bash
mobilectl deploy --notes "Fixed login bug and improved performance"
```

### From File

```bash
mobilectl deploy --notes "$(cat RELEASE_NOTES.md)"
```

### From Changelog

```bash
# Generate changelog first
mobilectl changelog generate

# Deploy with changelog as notes
mobilectl deploy --changelog
```

## Deployment Workflow

### What Happens During Deploy

1. **Pre-flight checks**
   - Validate configuration
   - Check credentials
   - Verify artifacts exist (or build)

2. **Version management** (if requested)
   - Bump version
   - Generate changelog
   - Update version files

3. **Build** (if needed)
   - Build for target platform(s)
   - Sign artifacts
   - Verify outputs

4. **Upload**
   - Upload to each destination
   - Set release notes
   - Configure test groups

5. **Notifications** (if configured)
   - Send Slack notification
   - Send email
   - Trigger webhooks

6. **Reporting**
   - Generate deployment report
   - Save to configured location

### Example Output

```
Starting deployment...

✓ Configuration validated
✓ Credentials verified

[Version Management]
✓ Bumped version: 1.0.0 → 1.0.1
✓ Updated 4 version files
✓ Generated changelog

[Build]
✓ Building Android (production, release)
  Duration: 1m 32s
  Output: app-production-release.apk (24.5 MB)

[Deploy]
✓ Uploading to Firebase
  URL: https://appdistribution.firebase.dev/i/abc123
  Test groups: qa-team, beta-testers
  Duration: 45s

✓ Uploading to TestFlight
  Build: 1.0.1 (42)
  Status: Processing
  Duration: 2m 18s

[Notifications]
✓ Sent Slack notification
✓ Sent email to team@example.com

[Report]
✓ Generated deployment report
  Location: build/reports/deploy-2024-01-15.html

Deployment completed successfully!
Total duration: 5m 23s
```

## Environment Variables

Set environment-specific variables:

```bash
# Deploy to staging
mobilectl deploy --env staging

# Deploy to production
mobilectl deploy --env production
```

Configuration:

```yaml
deploy:
  environments:
    staging:
      firebase:
        appId: "1:123:android:abc"
    production:
      firebase:
        appId: "1:456:android:def"
```

## Dry Run

Preview deployment without executing:

```bash
mobilectl deploy --dry-run
```

Output:
```
Dry run mode - no deployments will be executed

Deployment Plan:

Platform: Android
Flavors: production, staging
Destinations:
  - Firebase App Distribution
  - Google Play Console

Steps:
  1. Bump version: 1.0.0 → 1.0.1
  2. Generate changelog
  3. Build 2 variants
  4. Upload to 2 destinations
  5. Send notifications

Would deploy:
  - app-production-release.apk → Firebase, Play Console
  - app-staging-release.apk → Firebase

No actual deployment performed.
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Deploy

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Setup JDK
        uses: actions/setup-java@v2
        with:
          java-version: '17'

      - name: Deploy to Firebase
        env:
          MOBILECTL_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          MOBILECTL_STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        run: |
          mobilectl deploy firebase \
            --confirm \
            --verbose \
            --notes "Released from GitHub Actions"
```

### GitLab CI

```yaml
deploy:
  stage: deploy
  script:
    - mobilectl deploy --all-flavors --confirm
  only:
    - tags
  environment:
    name: production
```

## Configuration

Complete deployment configuration:

```yaml
deploy:
  enabled: true
  defaultGroup: production

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
      packageName: com.example.myapp

    local:
      enabled: true
      outputDir: build/deploy

  ios:
    enabled: true
    artifactPath: build/outputs/ipa/release/app.ipa

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

  flavorGroups:
    production:
      name: Production
      flavors: [productionRelease]
    testing:
      name: Testing
      flavors: [staging, development]
```

[See full configuration reference →](/reference/config-deploy)

## Notifications

Get notified about deployments:

```yaml
notify:
  slack:
    enabled: true
    webhookUrl: ${SLACK_WEBHOOK_URL}
  email:
    enabled: true
    recipients:
      - team@example.com
  webhook:
    enabled: true
    url: https://api.example.com/webhook
```

[Learn more →](/reference/config-notifications)

## Error Handling

### Upload Failed

```
Error: Upload to Firebase failed
Status: 403 Forbidden
Reason: Invalid service account credentials

Suggestions:
  1. Verify service account JSON is valid
  2. Check Firebase App Distribution is enabled
  3. Verify service account has required permissions
```

### Build Not Found

```
Error: Build artifact not found
Path: build/outputs/apk/release/app-release.apk

Suggestions:
  1. Run build first: mobilectl build android
  2. Or let deploy build automatically (remove --skip-build)
```

### Credentials Invalid

```
Error: TestFlight upload failed
Reason: Invalid API key

Suggestions:
  1. Verify API key path is correct
  2. Ensure API key has App Manager role
  3. Check teamId and bundleId match
```

## Best Practices

### 1. Test in Staging First

```bash
# Deploy to staging
mobilectl deploy --flavors staging

# Then production
mobilectl deploy --flavors production
```

### 2. Use Version Bumps

```bash
mobilectl deploy --bump-version patch --changelog
```

### 3. Automate with CI/CD

```yaml
# Only deploy from specific branches
only:
  - main
  - release/*
```

### 4. Use Dry Run

```bash
# Preview first
mobilectl deploy --dry-run

# Then execute
mobilectl deploy --confirm
```

### 5. Secure Credentials

```bash
# Use environment variables
export FIREBASE_SERVICE_ACCOUNT="$(cat credentials/firebase.json)"

# Never commit credentials
echo "credentials/" >> .gitignore
```

## Troubleshooting

### Slow Uploads

Enable parallel uploads (coming soon):

```yaml
deploy:
  parallel: true
  maxConcurrency: 3
```

### Timeout Issues

Increase timeout:

```yaml
deploy:
  timeout: 600  # 10 minutes
```

### Missing Artifacts

```bash
# Check artifact path
ls -lh build/outputs/apk/release/

# Update config if needed
```

## See Also

- [Configuration: Deploy](/reference/config-deploy)
- [Build Command](/reference/build)
- [Deployment Guide](/guide/deployment)
- [CI/CD Integration](/guide/ci-cd)
- [Notifications](/reference/config-notifications)
