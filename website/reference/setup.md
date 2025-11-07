# setup

Interactive wizard to generate complete `mobileops.yaml` configuration.

## Synopsis

```bash
mobilectl setup [OPTIONS]
```

## Description

The `setup` command launches an interactive wizard that guides you through 8 phases to create a complete MobileCtl configuration. It auto-detects your project settings and generates:

- Complete `mobileops.yaml` configuration file
- Optional GitHub Actions workflow
- Optional GitLab CI pipeline
- Setup documentation

This is the recommended way to configure MobileCtl for the first time.

## Options

### `--force`, `-f`

Overwrite existing configuration without prompting.

```bash
mobilectl setup --force
```

Creates a backup before overwriting:
- Backup name: `mobileops.backup.YYYYMMDD_HHMMSS.yaml`
- Location: Same directory as `mobileops.yaml`

### `--output PATH`, `-o PATH`

Generate configuration to a custom path.

```bash
mobilectl setup --output custom-config.yaml
mobilectl setup -o config/mobile.yaml
```

## Wizard Phases

The interactive wizard consists of 8 phases:

### 1. Project Information

- Auto-detects project type (Android, iOS, Flutter, React Native)
- Prompts for app name, package, version
- Confirms or allows manual entry

### 2. Build Configuration

- Detects Android product flavors
- Configures keystore and signing
- Sets up iOS scheme and configuration

### 3. Deployment Destinations

- Configures Firebase App Distribution
- Sets up Google Play Console
- Configures Apple TestFlight/App Store
- Optional local deployment

### 4. Version Management

- Enables auto-increment
- Sets bump strategy (patch/minor/major)
- Configures files to update

### 5. Changelog

- Enables changelog generation
- Sets format (markdown/html/json)
- Configures output file

### 6. Deployment Groups

- Creates flavor groups
- Groups related builds
- Simplifies batch deployment

### 7. CI/CD Setup

- Generates GitHub Actions workflow
- Optionally generates GitLab CI
- Configures triggers and secrets

### 8. Review & Confirm

- Shows configuration summary
- Confirms before generation
- Displays generated files

## Exit Status

| Code | Description |
|------|-------------|
| `0` | Setup completed successfully |
| `1` | Setup cancelled or error occurred |

## Examples

### Basic Setup

Run the wizard with default settings:

```bash
mobilectl setup
```

### Force Overwrite

Overwrite existing configuration:

```bash
mobilectl setup --force
```

### Custom Output

Generate to a different location:

```bash
mobilectl setup --output ~/configs/mobileops.yaml
```

## Output Files

The wizard generates these files:

| File | Description | Always Created |
|------|-------------|----------------|
| `mobileops.yaml` | Main configuration | ✅ Yes |
| `.github/workflows/mobilectl-deploy.yml` | GitHub Actions | ❌ Optional |
| `.gitlab-ci.yml` | GitLab CI | ❌ Optional |
| `docs/SETUP.md` | Setup summary | ✅ Yes |

## Auto-Detection

The wizard auto-detects:

### Project Type

| File/Directory | Detected Type |
|---------------|---------------|
| `pubspec.yaml` + `lib/` | Flutter |
| `package.json` with `react-native` | React Native |
| `build.gradle.kts` | Android Native |
| `.xcodeproj`/`.xcworkspace` | iOS Native |

### Configuration

- App name from `pubspec.yaml`, `strings.xml`, or directory
- Package name from `build.gradle.kts` or `AndroidManifest.xml`
- Version from `pubspec.yaml` or build files
- Android flavors from `build.gradle.kts`
- iOS schemes from Xcode project

### Credentials

Searches in common locations:

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

## Notes

### Security

- Never commit credential files to version control
- Add `credentials/` to `.gitignore`
- Use environment variables for passwords

### CI/CD Secrets

If you generate GitHub Actions workflow, add these secrets to your repository:

- `ANDROID_KEY_PASSWORD` - Android keystore password
- `ANDROID_STORE_PASSWORD` - Android store password
- `FIREBASE_SERVICE_ACCOUNT` - Firebase service account JSON
- `APP_STORE_CONNECT_API_KEY` - App Store Connect API key JSON

### Backup

When using `--force`, the wizard creates a timestamped backup:

```bash
$ mobilectl setup --force
⚠ Backing up existing config
✓ Backup created: mobileops.backup.20251107_123456.yaml
```

### Re-running Setup

You can run `setup` multiple times to update configuration:

1. Backup current config: `cp mobileops.yaml mobileops.yaml.bak`
2. Run setup: `mobilectl setup --force`
3. Compare: `diff mobileops.yaml mobileops.yaml.bak`
4. Merge desired changes

## Troubleshooting

### Config Already Exists

When a config file already exists:

```
⚠ Configuration file already exists: mobileops.yaml

Options:
  1. Overwrite (backup will be created)
  2. Cancel
  3. Specify different output path

Choice [1/2/3]:
```

**Solutions:**
- Choose option 1 to backup and overwrite
- Use `--force` flag to skip prompt
- Use `--output` to specify different path

### Credentials Not Found

If credentials aren't detected:

```
🔥 Firebase App Distribution
No credentials found.
Enable? (y/n) y
Service account JSON path: ▋
```

**Solutions:**
- Place files in `credentials/` directory
- Provide full path when prompted
- Configure manually in `mobileops.yaml` later

### Permission Denied

```
❌ Error: Failed to save configuration: Permission denied
```

**Solutions:**
- Check file/directory permissions
- Use `--output` with writable path
- Run with appropriate permissions

## See Also

- [`build`](build.md) - Build your app after setup
- [`deploy`](deploy.md) - Deploy using generated configuration
- [`version`](version.md) - Manage app versions
- [`changelog`](changelog.md) - Generate changelogs
- [Configuration Reference](configuration.md) - All config options
- [Setup Wizard Guide](/guide/setup-wizard) - Detailed setup guide

## Related Guides

- [Getting Started](/guide/getting-started) - Complete beginner guide
- [Quick Start](/guide/quick-start) - 5-minute tutorial
- [CI/CD Integration](/guide/ci-cd) - Automating deployments
- [Configuration](/guide/configuration) - Understanding config options
