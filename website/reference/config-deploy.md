# Deploy Configuration

Complete reference for deployment configuration in `mobileops.yaml`.

## Overview

The `deploy` section controls how and where your apps are deployed.

## Basic Structure

```yaml
deploy:
  enabled: true
  defaultGroup: production

  android:
    enabled: true
    artifactPath: build/outputs/apk/release/app-release.apk
    firebase: { }
    playConsole: { }
    local: { }

  ios:
    enabled: true
    artifactPath: build/outputs/ipa/release/app.ipa
    testflight: { }
    appStore: { }

  flavorGroups: { }
```

## Top-Level Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable deployment feature |
| `defaultGroup` | String | `null` | Default flavor group to deploy |

## Android Deployment

### Basic Configuration

```yaml
deploy:
  android:
    enabled: true
    artifactPath: build/outputs/apk/release/app-release.apk
```

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable Android deployment |
| `artifactPath` | String | Auto-detected | Path to APK/AAB file |

### Firebase App Distribution

Deploy Android apps to Firebase for testing.

```yaml
deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase-service-account.json
      googleServices: google-services.json  # Optional
      releaseNotes: "Automated upload from MobileCtl"
      testGroups:
        - qa-team
        - beta-testers
```

#### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable Firebase deployment |
| `serviceAccount` | String | **Required** | Path to Firebase service account JSON |
| `googleServices` | String | `null` | Path to google-services.json (optional) |
| `releaseNotes` | String | `"Automated upload"` | Release notes for testers |
| `testGroups` | String[] | `["qa-team"]` | Test groups to distribute to |

#### Setup

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create or select project
   - Enable App Distribution

2. **Generate Service Account**
   - Project Settings → Service Accounts
   - Click "Generate New Private Key"
   - Save as `firebase-service-account.json`

3. **Configure MobileCtl**
   ```yaml
   deploy:
     android:
       firebase:
         enabled: true
         serviceAccount: credentials/firebase-service-account.json
   ```

### Google Play Console

Deploy to Google Play Console (Internal Testing, Alpha, Beta, Production).

```yaml
deploy:
  android:
    playConsole:
      enabled: true
      serviceAccount: credentials/play-console-key.json
      packageName: com.example.myapp
```

#### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable Play Console deployment |
| `serviceAccount` | String | **Required** | Path to Play Console service account JSON |
| `packageName` | String | **Required** | App package name |

#### Setup

1. **Create Service Account**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create service account
   - Download JSON key

2. **Grant Permissions**
   - Go to [Play Console](https://play.google.com/console/)
   - Setup → API access
   - Link service account
   - Grant permissions (Release Manager or higher)

3. **Configure MobileCtl**
   ```yaml
   deploy:
     android:
       playConsole:
         enabled: true
         serviceAccount: credentials/play-console-key.json
         packageName: com.example.myapp
   ```

### Local Deployment

Copy APK to local directory.

```yaml
deploy:
  android:
    local:
      enabled: true
      outputDir: build/deploy
```

#### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable local deployment |
| `outputDir` | String | `"build/deploy"` | Output directory path |

## iOS Deployment

### Basic Configuration

```yaml
deploy:
  ios:
    enabled: true
    artifactPath: build/outputs/ipa/release/app.ipa
```

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable iOS deployment |
| `artifactPath` | String | Auto-detected | Path to IPA file |

### TestFlight

Deploy iOS apps to Apple TestFlight for beta testing.

```yaml
deploy:
  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey_ABC123.p8
      bundleId: com.example.myapp
      teamId: XYZ123
```

#### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable TestFlight deployment |
| `apiKeyPath` | String | **Required** | Path to App Store Connect API key (.p8) |
| `bundleId` | String | **Required** | App bundle identifier |
| `teamId` | String | **Required** | Apple Developer Team ID |

#### Setup

1. **Create App Store Connect API Key**
   - Go to [App Store Connect](https://appstoreconnect.apple.com/)
   - Users and Access → Keys
   - Create new key (App Manager role)
   - Download `.p8` file

2. **Find Team ID**
   - Go to [Apple Developer](https://developer.apple.com/account/)
   - Membership → Team ID

3. **Configure MobileCtl**
   ```yaml
   deploy:
     ios:
       testflight:
         enabled: true
         apiKeyPath: credentials/AuthKey_ABC123.p8
         bundleId: com.example.myapp
         teamId: XYZ123
   ```

### App Store

Deploy directly to App Store.

```yaml
deploy:
  ios:
    appStore:
      enabled: true
      apiKeyPath: credentials/AuthKey_ABC123.p8
      bundleId: com.example.myapp
      teamId: XYZ123
```

#### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable App Store deployment |
| `apiKeyPath` | String | **Required** | Path to App Store Connect API key (.p8) |
| `bundleId` | String | **Required** | App bundle identifier |
| `teamId` | String | **Required** | Apple Developer Team ID |

#### Setup

Same as TestFlight setup above.

## Flavor Groups

Organize flavors into groups for easier deployment.

```yaml
deploy:
  flavorGroups:
    production:
      name: Production
      description: Production builds for release
      flavors:
        - productionRelease

    testing:
      name: Testing
      description: Testing and staging builds
      flavors:
        - staging
        - development
        - qa

    all:
      name: All Flavors
      description: All available flavors
      flavors:
        - productionRelease
        - staging
        - development
```

### FlavorGroup Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `name` | String | **Required** | Display name |
| `description` | String | `""` | Description |
| `flavors` | String[] | **Required** | List of flavor names |

### Usage

```bash
# Deploy production group
mobilectl deploy --flavor-group production

# Deploy testing group
mobilectl deploy --flavor-group testing

# Deploy all flavors
mobilectl deploy --flavor-group all
```

## Complete Example

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
      googleServices: android/app/google-services.json
      releaseNotes: |
        Automated upload from MobileCtl

        Version: ${VERSION}
        Build: ${BUILD_NUMBER}
      testGroups:
        - qa-team
        - beta-testers
        - stakeholders

    playConsole:
      enabled: true
      serviceAccount: credentials/play-console-key.json
      packageName: com.example.myapp

    local:
      enabled: true
      outputDir: build/deploy/android

  ios:
    enabled: true
    artifactPath: build/outputs/ipa/Runner.ipa

    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey_ABC123DEF.p8
      bundleId: com.example.myapp
      teamId: ABC123XYZ

    appStore:
      enabled: false
      apiKeyPath: credentials/AuthKey_ABC123DEF.p8
      bundleId: com.example.myapp
      teamId: ABC123XYZ

  flavorGroups:
    production:
      name: Production
      description: Production release builds
      flavors:
        - productionRelease

    testing:
      name: Testing & Staging
      description: Internal testing builds
      flavors:
        - stagingRelease
        - developmentDebug
        - qaRelease

    all:
      name: All Flavors
      description: All available build flavors
      flavors:
        - productionRelease
        - stagingRelease
        - developmentDebug
        - qaRelease
```

## Environment-Specific Configuration

### Using Environment Variables

```yaml
deploy:
  android:
    firebase:
      serviceAccount: ${FIREBASE_SERVICE_ACCOUNT_PATH}
      testGroups: ${FIREBASE_TEST_GROUPS}

  ios:
    testflight:
      apiKeyPath: ${APPSTORE_API_KEY_PATH}
      teamId: ${APPLE_TEAM_ID}
```

Set in your environment:

```bash
export FIREBASE_SERVICE_ACCOUNT_PATH="credentials/firebase-prod.json"
export FIREBASE_TEST_GROUPS="qa-team,beta-testers"
export APPSTORE_API_KEY_PATH="credentials/AuthKey_ABC.p8"
export APPLE_TEAM_ID="ABC123XYZ"
```

### Multiple Environments

#### Development

```yaml
# mobileops.dev.yaml
deploy:
  android:
    firebase:
      serviceAccount: credentials/firebase-dev.json
      testGroups: [developers]
```

#### Staging

```yaml
# mobileops.staging.yaml
deploy:
  android:
    firebase:
      serviceAccount: credentials/firebase-staging.json
      testGroups: [qa-team, stakeholders]
```

#### Production

```yaml
# mobileops.prod.yaml
deploy:
  android:
    firebase:
      serviceAccount: credentials/firebase-prod.json
      testGroups: [beta-testers]
    playConsole:
      enabled: true
```

Use with:

```bash
mobilectl --config mobileops.dev.yaml deploy
mobilectl --config mobileops.staging.yaml deploy
mobilectl --config mobileops.prod.yaml deploy
```

## Best Practices

### 1. Secure Credentials

```yaml
# ❌ Don't commit credentials
deploy:
  android:
    firebase:
      serviceAccount: credentials/firebase-key.json  # In .gitignore

# ✅ Use environment variables
deploy:
  android:
    firebase:
      serviceAccount: ${FIREBASE_SERVICE_ACCOUNT_PATH}
```

### 2. Use Flavor Groups

```yaml
deploy:
  flavorGroups:
    production:
      flavors: [productionRelease]
    testing:
      flavors: [staging, development]
```

```bash
# Easy deployment
mobilectl deploy --flavor-group production
```

### 3. Environment-Specific Test Groups

```yaml
deploy:
  android:
    firebase:
      testGroups:
        - qa-team           # Always
        - ${EXTRA_TESTERS}  # Environment-specific
```

### 4. Descriptive Release Notes

```yaml
deploy:
  android:
    firebase:
      releaseNotes: |
        Version ${VERSION}
        Build ${BUILD_NUMBER}

        Features:
        - New user interface
        - Performance improvements

        Fixes:
        - Login issue resolved
```

### 5. Enable Local Backup

```yaml
deploy:
  android:
    local:
      enabled: true
      outputDir: backups/android
  ios:
    local:
      enabled: true
      outputDir: backups/ios
```

## Troubleshooting

### Firebase Upload Failed

```
Error: Firebase upload failed
Status: 403 Forbidden

Suggestions:
  1. Verify service account JSON is valid
  2. Check Firebase App Distribution is enabled
  3. Ensure service account has required permissions
```

**Solution:**
- Download fresh service account key
- Verify permissions in Firebase Console
- Check app is registered in Firebase

### TestFlight Upload Failed

```
Error: TestFlight upload failed
Reason: Invalid API key

Suggestions:
  1. Verify API key path is correct
  2. Ensure API key has App Manager role
  3. Check teamId and bundleId match
```

**Solution:**
- Verify API key file exists
- Check API key permissions in App Store Connect
- Confirm Team ID and Bundle ID are correct

### Artifact Not Found

```
Error: Build artifact not found
Path: build/outputs/apk/release/app-release.apk

Suggestions:
  1. Run build first: mobilectl build android
  2. Or let deploy build automatically (remove --skip-build)
```

**Solution:**
```bash
# Build first
mobilectl build android release

# Then deploy
mobilectl deploy firebase
```

## See Also

- [Deploy Command →](/reference/deploy)
- [Build Configuration →](/reference/config-build)
- [Notifications →](/reference/config-notifications)
- [Deployment Guide →](/guide/deployment)
- [CI/CD Integration →](/guide/ci-cd)
