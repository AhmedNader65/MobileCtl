# Getting Started

Get MobileCtl up and running in your mobile project in just a few minutes.

## Prerequisites

Before you begin, ensure you have:

- **JDK 11 or higher** installed
- **Git** installed and configured
- **Android SDK** (for Android builds)
- **Xcode** (for iOS builds, macOS only)
- A mobile project (Android, iOS, or both)

Check your Java version:

```bash
java -version
# Should show 11 or higher
```

## Installation

### Option 1: Clone and Build (Current)

```bash
# Clone the repository
git clone https://github.com/AhmedNader65/MobileCtl.git
cd MobileCtl

# Build the CLI
./gradlew build

# Add to PATH (optional)
export PATH="$PATH:$(pwd)"
```

### Option 2: Homebrew (Coming Soon)

```bash
brew install mobilectl
```

### Option 3: Direct Download (Coming Soon)

Download the latest release from [GitHub Releases](https://github.com/AhmedNader65/MobileCtl/releases).

## Verify Installation

Check that MobileCtl is installed correctly:

```bash
./mobilectl.sh --help
```

You should see the help output with available commands.

## Create Configuration File

Navigate to your mobile project and create a `mobileops.yaml` file:

```bash
cd /path/to/your/mobile/project
```

Create a basic configuration:

```yaml
# mobileops.yaml
app:
  name: MyAwesomeApp
  identifier: com.example.myapp
  version: 1.0.0

build:
  android:
    enabled: true
    defaultType: release
  ios:
    enabled: true
    scheme: MyApp

version:
  enabled: true
  current: 1.0.0
  filesToUpdate:
    - pubspec.yaml
    - package.json

changelog:
  enabled: true
  outputFile: CHANGELOG.md
```

::: tip
MobileCtl will auto-detect many settings, but explicit configuration gives you more control.
:::

## Your First Build

### Android Build

```bash
mobilectl build android
```

This will:
1. Detect your Android project
2. Use default flavor and build type
3. Compile the APK
4. Sign it (if keystore configured)
5. Output the build location

### iOS Build

```bash
mobilectl build ios
```

This will:
1. Detect your iOS project
2. Use configured scheme
3. Build the IPA
4. Sign it (if provisioning profile configured)
5. Output the build location

### Multi-Platform Build

```bash
mobilectl build all
```

Builds both Android and iOS in sequence.

## Your First Version Bump

Bump your app version:

```bash
mobilectl version bump patch
```

This will:
1. Read current version from config
2. Increment patch version (1.0.0 → 1.0.1)
3. Update all configured files
4. Create automatic backup
5. Validate the changes

View the current version:

```bash
mobilectl version show
```

## Your First Changelog

Generate a changelog from your git commits:

```bash
mobilectl changelog generate
```

This will:
1. Read git commit history
2. Parse conventional commits
3. Group by commit type
4. Add emoji and formatting
5. Create CHANGELOG.md
6. Create automatic backup

Preview before generating:

```bash
mobilectl changelog generate --dry-run
```

## Your First Deployment

::: warning Prerequisites
Make sure you have:
- Built your app (`mobilectl build android`)
- Firebase or TestFlight credentials configured
:::

Deploy to Firebase:

```bash
mobilectl deploy firebase
```

Deploy to TestFlight:

```bash
mobilectl deploy testflight
```

Interactive mode (choose platforms/destinations):

```bash
mobilectl deploy --interactive
```

## Common Workflows

### Release Workflow

Complete release in 3 commands:

```bash
# 1. Bump version
mobilectl version bump minor

# 2. Generate changelog
mobilectl changelog generate

# 3. Build and deploy
mobilectl deploy --all-flavors
```

### Quick Deploy Workflow

All-in-one deploy command:

```bash
mobilectl deploy --bump-version patch --changelog --all-flavors
```

This single command:
1. Bumps version (patch)
2. Generates changelog
3. Builds all flavors
4. Deploys to configured destinations
5. Sends notifications

### CI/CD Workflow

```bash
# In your CI/CD pipeline
mobilectl deploy firebase --confirm --verbose
```

- `--confirm`: Skip confirmation prompts
- `--verbose`: Detailed logging

## Next Steps

Now that you have MobileCtl running, explore these guides:

- [Configuration Guide](/guide/configuration) - Deep dive into config options
- [Build Automation](/guide/build-automation) - Advanced build scenarios
- [Version Management](/guide/version-management) - Version bump strategies
- [Changelog Guide](/guide/changelog) - Customizing changelogs
- [Deployment](/guide/deployment) - Multi-platform deployment
- [CI/CD Integration](/guide/ci-cd) - Automate with GitHub Actions

## Troubleshooting

### Command not found

If you get "command not found", make sure:
1. You've built the project: `./gradlew build`
2. You're using the correct path: `./mobilectl.sh` (not just `mobilectl`)
3. Or add to PATH: `export PATH="$PATH:/path/to/MobileCtl"`

### Config file not found

MobileCtl looks for `mobileops.yaml` in:
1. Current directory
2. Parent directories (up to git root)

Make sure your config file is named correctly.

### Build failures

Check that you have:
- Correct Android SDK installed
- Xcode installed (for iOS)
- Keystore configured (for Android release builds)
- Provisioning profile (for iOS release builds)

Use `--verbose` flag for detailed error messages:

```bash
mobilectl build android --verbose
```

## Getting Help

- **Documentation**: You're reading it!
- **GitHub Issues**: [Report bugs or request features](https://github.com/AhmedNader65/MobileCtl/issues)
- **Examples**: Check out [real-world examples](/examples/)
- **Command Help**: Run any command with `--help`

```bash
mobilectl --help
mobilectl build --help
mobilectl version --help
```

Ready to dive deeper? Continue to [Quick Start Tutorial](/guide/quick-start).
