# Command Reference

Complete reference for all MobileCtl commands.

## Overview

MobileCtl provides a unified CLI with four main commands:

| Command | Description |
|---------|-------------|
| [`version`](/reference/version) | Manage app version numbers |
| [`build`](/reference/build) | Build Android and iOS apps |
| [`deploy`](/reference/deploy) | Deploy to app stores and distribution platforms |
| [`changelog`](/reference/changelog) | Generate and manage changelogs |

## Global Options

These options work with all commands:

| Option | Description |
|--------|-------------|
| `--help`, `-h` | Show help for command |
| `--version` | Show MobileCtl version |

## Command Structure

All commands follow this structure:

```bash
mobilectl <command> [subcommand] [arguments] [options]
```

### Examples

```bash
mobilectl version show
mobilectl version bump patch --dry-run
mobilectl build android production release
mobilectl deploy firebase --interactive
mobilectl changelog generate --append
```

## Common Options

Many commands share these common options:

### Verbosity

| Option | Description |
|--------|-------------|
| `--verbose`, `-v` | Enable verbose output |
| `--quiet`, `-q` | Suppress non-essential output |

### Dry Run

| Option | Description |
|--------|-------------|
| `--dry-run` | Show what would be done without executing |

### Confirmation

| Option | Description |
|--------|-------------|
| `--confirm`, `-y` | Skip confirmation prompts (for CI/CD) |

## Quick Reference

### Version Commands

```bash
mobilectl version show                    # Show current version
mobilectl version bump <level>            # Bump version (major/minor/patch)
mobilectl version restore [backup]        # Restore from backup
```

### Build Commands

```bash
mobilectl build <platform> [flavor] [type]   # Build app
mobilectl build android                      # Build Android (auto-detect)
mobilectl build ios                          # Build iOS
mobilectl build all                          # Build both platforms
```

### Deploy Commands

```bash
mobilectl deploy [platform] [destination]    # Deploy app
mobilectl deploy firebase                    # Deploy to Firebase
mobilectl deploy testflight                  # Deploy to TestFlight
mobilectl deploy --interactive               # Interactive mode
mobilectl deploy --all-flavors               # Deploy all flavors
```

### Changelog Commands

```bash
mobilectl changelog generate              # Generate changelog
mobilectl changelog show                  # Show current changelog
mobilectl changelog update                # Update existing changelog
mobilectl changelog restore [backup]      # Restore from backup
```

## Command Details

### version

Manage your app's version number with semantic versioning support.

[Full version command reference →](/reference/version)

```bash
# Show current version
mobilectl version show

# Bump patch version (1.0.0 → 1.0.1)
mobilectl version bump patch

# Bump minor version (1.0.0 → 1.1.0)
mobilectl version bump minor

# Bump major version (1.0.0 → 2.0.0)
mobilectl version bump major

# Preview without making changes
mobilectl version bump patch --dry-run

# Restore from backup
mobilectl version restore
```

### build

Build your Android and iOS apps with automatic configuration detection.

[Full build command reference →](/reference/build)

```bash
# Build Android with auto-detection
mobilectl build android

# Build specific flavor and type
mobilectl build android production release

# Build iOS
mobilectl build ios

# Build all platforms
mobilectl build all

# Preview build without executing
mobilectl build android --dry-run
```

### deploy

Deploy your apps to Firebase, TestFlight, Play Console, and App Store.

[Full deploy command reference →](/reference/deploy)

```bash
# Interactive deployment
mobilectl deploy --interactive

# Deploy to Firebase
mobilectl deploy firebase

# Deploy to TestFlight
mobilectl deploy testflight

# Deploy all flavors
mobilectl deploy --all-flavors

# Deploy with version bump and changelog
mobilectl deploy --bump-version patch --changelog
```

### changelog

Generate beautiful changelogs from your git commit history.

[Full changelog command reference →](/reference/changelog)

```bash
# Generate changelog
mobilectl changelog generate

# Preview without saving
mobilectl changelog generate --dry-run

# Generate and append to existing
mobilectl changelog generate --append

# Generate from specific tag
mobilectl changelog generate --from-tag v1.0.0

# Show current changelog
mobilectl changelog show
```

## Exit Codes

MobileCtl uses standard exit codes:

| Code | Meaning |
|------|---------|
| `0` | Success |
| `1` | General error |
| `2` | Invalid command or arguments |
| `3` | Configuration error |
| `4` | Build error |
| `5` | Deploy error |

Use in scripts:

```bash
mobilectl build android
if [ $? -eq 0 ]; then
  echo "Build successful"
else
  echo "Build failed"
  exit 1
fi
```

## Environment Variables

MobileCtl respects these environment variables:

| Variable | Description |
|----------|-------------|
| `MOBILECTL_CONFIG` | Path to config file (default: `mobileops.yaml`) |
| `MOBILECTL_KEY_PASSWORD` | Android keystore password |
| `MOBILECTL_STORE_PASSWORD` | Android store password |
| `ANDROID_HOME` | Android SDK location |
| `JAVA_HOME` | Java SDK location |

## Configuration

All commands read from `mobileops.yaml` by default. You can specify a different config file:

```bash
mobilectl --config custom-config.yaml build android
```

See the [Configuration Reference](/reference/configuration) for details.

## Examples

### Complete Release Workflow

```bash
# 1. Bump version
mobilectl version bump minor

# 2. Generate changelog
mobilectl changelog generate

# 3. Build all platforms
mobilectl build all

# 4. Deploy to all destinations
mobilectl deploy --all-flavors
```

### One-Command Deploy

```bash
mobilectl deploy \
  --bump-version patch \
  --changelog \
  --all-flavors \
  --confirm
```

### CI/CD Pipeline

```bash
# GitHub Actions example
mobilectl deploy firebase \
  --confirm \
  --verbose \
  --skip-build
```

## Getting Help

### Command-Specific Help

```bash
mobilectl version --help
mobilectl build --help
mobilectl deploy --help
mobilectl changelog --help
```

### Subcommand Help

```bash
mobilectl version bump --help
mobilectl changelog generate --help
```

## Next Steps

Explore detailed documentation for each command:

- [version command →](/reference/version)
- [build command →](/reference/build)
- [deploy command →](/reference/deploy)
- [changelog command →](/reference/changelog)

Or check out:

- [Configuration Reference](/reference/configuration)
- [Real-World Examples](/examples/)
- [CI/CD Integration](/guide/ci-cd)
