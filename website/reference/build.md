# build

Build Android and iOS apps with automatic configuration detection.

## Synopsis

```bash
mobilectl build [platform] [flavor] [type] [options]
```

## Arguments

| Argument | Required | Description | Values |
|----------|----------|-------------|--------|
| `platform` | No | Target platform | `android`, `ios`, `all` |
| `flavor` | No | Build flavor | `production`, `staging`, etc. |
| `type` | No | Build type | `debug`, `release` |

## Options

| Option | Short | Description |
|--------|-------|-------------|
| `--verbose` | `-v` | Verbose output with detailed logs |
| `--dry-run` | | Preview build configuration without building |

## Basic Usage

### Auto-Detection

```bash
# Auto-detect platform, flavor, and type
mobilectl build

# Build Android with defaults
mobilectl build android

# Build iOS with defaults
mobilectl build ios

# Build both platforms
mobilectl build all
```

### Specific Build

```bash
# Android release build
mobilectl build android release

# iOS production release
mobilectl build ios production release

# Android staging debug
mobilectl build android staging debug
```

## Platform-Specific Builds

### Android

Build Android APK or AAB files.

```bash
# Default build (uses defaultType from config)
mobilectl build android

# Release build
mobilectl build android release

# Specific flavor and type
mobilectl build android production release
```

#### What It Does

1. Detects Android project structure
2. Configures Gradle based on flavor and type
3. Runs `./gradlew bundle<Flavor><Type>`
4. Signs APK/AAB with configured keystore
5. Outputs artifact location

#### Output

```
Building Android app...

Platform: Android
Flavor: production
Type: release
Signing: Enabled

Running: ./gradlew bundleProductionRelease

Build successful!

Output: android/app/build/outputs/apk/production/release/app-production-release.apk
Size: 24.5 MB
Duration: 1m 23s
```

### iOS

Build iOS IPA files.

```bash
# Default build
mobilectl build ios

# Release build
mobilectl build ios release

# Specific configuration
mobilectl build ios production release
```

#### What It Does

1. Detects Xcode project/workspace
2. Configures build settings
3. Runs `xcodebuild` with configured scheme
4. Archives and exports IPA
5. Signs with provisioning profile
6. Outputs artifact location

#### Output

```
Building iOS app...

Platform: iOS
Scheme: Runner
Configuration: Release
Signing: Enabled

Running: xcodebuild -scheme Runner -configuration Release

Build successful!

Output: build/ios/iphoneos/Runner.app
Size: 18.2 MB
Duration: 2m 15s
```

### Multi-Platform

Build both Android and iOS in sequence:

```bash
mobilectl build all
```

Output:
```
Building for all platforms...

[1/2] Building Android...
✓ Android build successful (1m 23s)

[2/2] Building iOS...
✓ iOS build successful (2m 15s)

All builds completed successfully!
Total duration: 3m 38s
```

## Flavors

Build flavors represent different app variants (environments, brands, etc.).

### Android Flavors

Defined in `build.gradle`:

```gradle
android {
    flavorDimensions "environment"
    productFlavors {
        production {
            dimension "environment"
            applicationIdSuffix ""
        }
        staging {
            dimension "environment"
            applicationIdSuffix ".staging"
        }
        development {
            dimension "environment"
            applicationIdSuffix ".dev"
        }
    }
}
```

Build specific flavor:

```bash
mobilectl build android production release
mobilectl build android staging debug
mobilectl build android development debug
```

### iOS Schemes

Defined in Xcode as schemes:

```bash
# Use specific scheme
mobilectl build ios ProductionRelease
```

Configure in `mobileops.yaml`:

```yaml
build:
  ios:
    scheme: Production
    configuration: Release
```

## Build Types

### Android Build Types

| Type | Description | Use Case |
|------|-------------|----------|
| `debug` | Debuggable, not optimized | Development |
| `release` | Optimized, signed | Production |

### iOS Configurations

| Configuration | Description | Use Case |
|---------------|-------------|----------|
| `Debug` | Debuggable, not optimized | Development |
| `Release` | Optimized, signed | Production |

## Configuration

Configure builds in `mobileops.yaml`:

```yaml
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

  ios:
    enabled: true
    projectPath: .
    scheme: Runner
    configuration: Release
    codeSignIdentity: "iPhone Distribution"
    provisioningProfile: "path/to/profile.mobileprovision"
```

[See full configuration reference →](/reference/config-build)

## Code Signing

### Android Signing

#### Keystore Configuration

```yaml
build:
  android:
    keyStore: release.keystore
    keyAlias: my-app-key
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}
```

#### Generate Keystore

```bash
keytool -genkey -v -keystore release.keystore \
  -alias my-app-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

#### Environment Variables

```bash
export MOBILECTL_KEY_PASSWORD="your_key_password"
export MOBILECTL_STORE_PASSWORD="your_store_password"
```

### iOS Signing

#### Code Signing Configuration

```yaml
build:
  ios:
    codeSignIdentity: "iPhone Distribution"
    provisioningProfile: "path/to/profile.mobileprovision"
```

#### Automatic Signing (Xcode 8+)

Let Xcode handle signing:

```yaml
build:
  ios:
    automaticSigning: true
    teamId: "XXXXXXXXXX"
```

## Output Artifacts

### Android

#### APK (Android Package)

```
android/app/build/outputs/apk/production/release/app-production-release.apk
```

Used for:
- Manual installation
- Direct distribution
- Testing

#### AAB (Android App Bundle)

```
android/app/build/outputs/bundle/production/release/app-production-release.aab
```

Used for:
- Google Play Store
- Optimized delivery

### iOS

#### APP (iOS Application)

```
build/ios/iphoneos/Runner.app
```

Used for:
- Simulator testing
- Device installation

#### IPA (iOS App Store Package)

```
build/ios/iphoneos/Runner.ipa
```

Used for:
- App Store distribution
- TestFlight
- Enterprise distribution

## Dry Run

Preview build configuration without actually building:

```bash
mobilectl build android --dry-run
```

Output:
```
Dry run mode - no builds will be executed

Build Configuration:
  Platform: Android
  Flavor: production
  Type: release
  Signing: Enabled
  Keystore: release.keystore

Would execute:
  ./gradlew bundleProductionRelease

Output would be:
  android/app/build/outputs/apk/production/release/app-production-release.apk
```

## Verbose Mode

Get detailed build logs:

```bash
mobilectl build android --verbose
```

Output includes:
- Full Gradle output
- Dependency resolution
- Compilation steps
- Signing process
- Performance metrics

## Build Performance

### Cache Management

MobileCtl preserves Gradle/Xcode caches for faster builds.

### Incremental Builds

Only changed files are recompiled:

```bash
# First build: 2m 30s
mobilectl build android

# Incremental build: 45s
mobilectl build android
```

### Clean Build

Force clean build:

```bash
# Android
cd android && ./gradlew clean
mobilectl build android

# iOS
rm -rf build/ios
mobilectl build ios
```

## Error Handling

### Build Failed

```
Error: Android build failed
Exit code: 1

Build log:
  > Task :app:compileProductionReleaseKotlin FAILED

  Error: Unresolved reference: someFunction

Suggestion: Check your code for compilation errors
Full logs: android/app/build/reports/
```

### Signing Failed

```
Error: Code signing failed
Reason: Keystore not found

Path: release.keystore

Suggestions:
  1. Check keystore path in mobileops.yaml
  2. Generate keystore: keytool -genkey -v -keystore release.keystore
  3. Set environment variables: MOBILECTL_KEY_PASSWORD, MOBILECTL_STORE_PASSWORD
```

### Platform Not Detected

```
Error: Platform not detected

Suggestions:
  1. Ensure you're in a mobile project directory
  2. Check for android/ or ios/ directories
  3. Specify platform explicitly: mobilectl build android
```

## Integration with Other Commands

### Build Before Deploy

```bash
# Manual
mobilectl build all
mobilectl deploy --all-flavors

# Automatic (deploy builds if needed)
mobilectl deploy --all-flavors
```

### Skip Build During Deploy

```bash
# Use existing artifacts
mobilectl deploy --skip-build
```

## Advanced Usage

### Multiple Flavors

Build all flavors sequentially:

```bash
for flavor in production staging development; do
  mobilectl build android $flavor release
done
```

### Custom Output Directory

Configure in build settings:

```yaml
build:
  android:
    outputDir: build/custom/android
  ios:
    outputDir: build/custom/ios
```

### Build Hooks (Coming Soon)

```yaml
build:
  hooks:
    preBuild: ./scripts/pre-build.sh
    postBuild: ./scripts/post-build.sh
```

## Troubleshooting

### Gradle Errors

```bash
# Clean Gradle cache
cd android
./gradlew clean
rm -rf .gradle build

# Rebuild
mobilectl build android
```

### Xcode Errors

```bash
# Clean Xcode build
rm -rf build/ios
rm -rf ~/Library/Developer/Xcode/DerivedData

# Rebuild
mobilectl build ios
```

### Memory Issues

Increase heap size:

```gradle
// android/gradle.properties
org.gradle.jvmargs=-Xmx4096m
```

### Slow Builds

Enable parallel builds:

```gradle
// android/gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
```

## Best Practices

### 1. Use Flavors for Environments

```bash
mobilectl build android development debug      # Dev
mobilectl build android staging release        # Staging
mobilectl build android production release     # Production
```

### 2. Store Signing Credentials Securely

```bash
# Use environment variables
export MOBILECTL_KEY_PASSWORD="..."
export MOBILECTL_STORE_PASSWORD="..."

# Never commit credentials to git
echo ".env" >> .gitignore
```

### 3. Test Builds Locally

```bash
# Preview first
mobilectl build android --dry-run

# Then build
mobilectl build android
```

### 4. Use CI/CD for Production

```yaml
# .github/workflows/build.yml
- name: Build APK
  run: mobilectl build android production release --verbose
```

## See Also

- [Configuration: Build](/reference/config-build)
- [Deploy Command](/reference/deploy)
- [Build Automation Guide](/guide/build-automation)
- [CI/CD Integration](/guide/ci-cd)
