# Quick Start

Get up and running with MobileCtl in 5 minutes.

## Step 1: Install MobileCtl

Clone and build the project:

```bash
git clone https://github.com/AhmedNader65/MobileCtl.git
cd MobileCtl
./gradlew build
```

Add to your PATH (optional):

```bash
export PATH="$PATH:$(pwd)"
```

## Step 2: Navigate to Your Project

```bash
cd /path/to/your/mobile/project
```

## Step 3: Create Configuration

Create `mobileops.yaml` in your project root:

```yaml
app:
  name: MyApp
  identifier: com.example.myapp
  version: 1.0.0

build:
  android:
    enabled: true
  ios:
    enabled: true

version:
  enabled: true
  current: 1.0.0
  filesToUpdate:
    - pubspec.yaml
    - package.json

changelog:
  enabled: true
```

## Step 4: Build Your App

### Android

```bash
mobilectl build android
```

Output:
```
Building Android app...
✓ Build successful (1m 23s)
Output: android/app/build/outputs/apk/release/app-release.apk
```

### iOS

```bash
mobilectl build ios
```

### Both Platforms

```bash
mobilectl build all
```

## Step 5: Manage Version

Show current version:

```bash
mobilectl version show
```

Bump version:

```bash
mobilectl version bump patch
```

Output:
```
Version bumped: 1.0.0 → 1.0.1

Updated files:
  ✓ mobileops.yaml
  ✓ pubspec.yaml
  ✓ package.json

Backup created: version-backup-2024-01-15-14-30-00
```

## Step 6: Generate Changelog

Create changelog from git commits:

```bash
mobilectl changelog generate
```

Output:
```
Generating changelog...

Analyzed commits: 24
  - Features: 8
  - Bug fixes: 10
  - Documentation: 4
  - Other: 2

✓ Changelog generated: CHANGELOG.md
Backup created: changelog-backup-2024-01-15-14-35-00
```

## Step 7: Deploy (Optional)

If you have Firebase or TestFlight configured:

```bash
mobilectl deploy firebase
```

Or use interactive mode:

```bash
mobilectl deploy --interactive
```

## Complete Workflow Example

Here's a typical release workflow:

```bash
# 1. Bump version
mobilectl version bump minor

# 2. Generate changelog
mobilectl changelog generate

# 3. Build for all platforms
mobilectl build all

# 4. Deploy (if configured)
mobilectl deploy --all-flavors
```

Or use the all-in-one command:

```bash
mobilectl deploy --bump-version minor --changelog --all-flavors
```

## What's Next?

Now that you have the basics:

### Learn More

- [Configuration Guide](/guide/configuration) - Customize your setup
- [Build Automation](/guide/build-automation) - Advanced build options
- [Version Management](/guide/version-management) - Version strategies
- [Deployment](/guide/deployment) - Deploy to app stores

### Explore Commands

- [version command](/reference/version) - Full version management
- [build command](/reference/build) - Build reference
- [deploy command](/reference/deploy) - Deployment options
- [changelog command](/reference/changelog) - Changelog features

### See Examples

- [Android Example](/examples/android)
- [iOS Example](/examples/ios)
- [Multi-Platform Example](/examples/multi-platform)
- [CI/CD Examples](/examples/ci-cd)

## Common Tasks

### Daily Development

```bash
# Build debug version
mobilectl build android debug

# Build staging
mobilectl build android staging release

# Deploy to testers
mobilectl deploy firebase --flavors staging
```

### Release to Production

```bash
# Complete release
mobilectl deploy \
  --bump-version minor \
  --changelog \
  --flavors production \
  --confirm
```

### Hotfix

```bash
# Quick patch release
mobilectl version bump patch
mobilectl build all
mobilectl deploy --all-flavors --confirm
```

## Tips

### 1. Use Dry Run

Preview what will happen:

```bash
mobilectl build android --dry-run
mobilectl version bump patch --dry-run
mobilectl deploy --dry-run
```

### 2. Use Verbose Mode

Get detailed output for debugging:

```bash
mobilectl build android --verbose
```

### 3. Check Help

Every command has built-in help:

```bash
mobilectl --help
mobilectl build --help
mobilectl version bump --help
```

### 4. Use Backups

MobileCtl automatically creates backups. Restore if needed:

```bash
# List version backups
mobilectl version restore

# Restore specific backup
mobilectl version restore version-backup-2024-01-15-14-30-00

# List changelog backups
mobilectl changelog restore
```

## Troubleshooting

### Build Fails

```bash
# Check configuration
cat mobileops.yaml

# Use verbose mode
mobilectl build android --verbose

# Check build logs
cat android/app/build/reports/...
```

### Version Not Updating

Ensure files are listed in config:

```yaml
version:
  filesToUpdate:
    - pubspec.yaml
    - package.json
    # Add any other files with version numbers
```

### Deploy Fails

Check credentials are configured:

```yaml
deploy:
  android:
    firebase:
      serviceAccount: credentials/firebase-key.json  # Make sure this exists
```

## Getting Help

- **Documentation**: Browse these docs
- **Examples**: Check [examples section](/examples/)
- **GitHub Issues**: [Report bugs](https://github.com/AhmedNader65/MobileCtl/issues)
- **Command Help**: Run `mobilectl <command> --help`

## Next Steps

Ready to dive deeper?

1. [Complete Configuration Guide](/guide/configuration)
2. [Learn About Build Automation](/guide/build-automation)
3. [Set Up Deployment](/guide/deployment)
4. [Integrate with CI/CD](/guide/ci-cd)

Happy building! 🚀
