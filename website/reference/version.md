# version

Manage your app's version number with semantic versioning support.

## Synopsis

```bash
mobilectl version <subcommand> [options]
```

## Subcommands

| Subcommand | Description |
|------------|-------------|
| `show` | Display current version |
| `bump <level>` | Bump version (major, minor, or patch) |
| `restore [backup]` | Restore version from backup |

## version show

Display the current app version from your configuration.

### Usage

```bash
mobilectl version show [options]
```

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--verbose` | `-v` | Show detailed version information |

### Examples

```bash
# Show current version
mobilectl version show

# Show with details
mobilectl version show --verbose
```

### Output

```
Current version: 1.2.3

Version source: mobileops.yaml
Files to update:
  - pubspec.yaml
  - package.json
  - android/app/build.gradle
```

## version bump

Bump the version number following semantic versioning.

### Usage

```bash
mobilectl version bump <level> [options]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `level` | Yes | Version level: `major`, `minor`, or `patch` |

### Bump Levels

| Level | Example | Use Case |
|-------|---------|----------|
| `major` | 1.2.3 → 2.0.0 | Breaking changes |
| `minor` | 1.2.3 → 1.3.0 | New features (backward compatible) |
| `patch` | 1.2.3 → 1.2.4 | Bug fixes |

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--verbose` | `-v` | Verbose output |
| `--dry-run` | | Preview changes without applying |
| `--skip-backup` | | Skip creating backup (not recommended) |

### Examples

```bash
# Bump patch version
mobilectl version bump patch

# Bump minor version
mobilectl version bump minor

# Bump major version
mobilectl version bump major

# Preview without applying
mobilectl version bump patch --dry-run

# Verbose output
mobilectl version bump minor --verbose
```

### What It Does

1. **Reads current version** from `mobileops.yaml`
2. **Calculates new version** based on level
3. **Creates automatic backup** of all version files
4. **Updates version** in:
   - `mobileops.yaml`
   - All files listed in `filesToUpdate`
5. **Validates changes** to ensure correctness
6. **Reports results** with summary

### File Updates

MobileCtl automatically updates version in multiple files:

#### pubspec.yaml (Flutter/Dart)
```yaml
version: 1.2.3+4
```

#### package.json (React Native)
```json
{
  "version": "1.2.3"
}
```

#### build.gradle (Android)
```gradle
versionName "1.2.3"
versionCode 4
```

#### Info.plist (iOS)
```xml
<key>CFBundleShortVersionString</key>
<string>1.2.3</string>
```

### Configuration

Configure version bumping in `mobileops.yaml`:

```yaml
version:
  enabled: true
  current: 1.0.0
  autoIncrement: false
  bumpStrategy: patch
  filesToUpdate:
    - pubspec.yaml
    - package.json
    - android/app/build.gradle
    - ios/Runner/Info.plist
```

[See full configuration reference →](/reference/config-version)

## version restore

Restore version from an automatic backup.

### Usage

```bash
mobilectl version restore [backup] [options]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `backup` | No | Backup ID to restore (omit to list backups) |

### Examples

```bash
# List available backups
mobilectl version restore

# Restore specific backup
mobilectl version restore version-backup-2024-01-15-14-30-00
```

### Listing Backups

When called without arguments, shows available backups:

```
Available version backups:

1. version-backup-2024-01-15-14-30-00
   Version: 1.2.3 → 1.2.4
   Date: 2024-01-15 14:30:00
   Files: 4 files

2. version-backup-2024-01-14-10-15-00
   Version: 1.2.2 → 1.2.3
   Date: 2024-01-14 10:15:00
   Files: 4 files

To restore, run:
  mobilectl version restore <backup-id>
```

### Restoring

```bash
mobilectl version restore version-backup-2024-01-15-14-30-00
```

Output:
```
Restoring version backup...

Restored files:
  ✓ mobileops.yaml
  ✓ pubspec.yaml
  ✓ package.json
  ✓ android/app/build.gradle

Version restored: 1.2.4 → 1.2.3
```

## Backup System

MobileCtl automatically creates backups before any version change.

### Backup Location

Backups are stored in:
```
.mobilectl/backups/version/
```

### Backup Contents

Each backup includes:
- All files listed in `filesToUpdate`
- Metadata (timestamp, old/new version)
- Checksum for verification

### Backup Naming

```
version-backup-YYYY-MM-DD-HH-MM-SS
```

### Manual Cleanup

Backups are kept indefinitely. To clean up old backups:

```bash
# Remove backups older than 30 days
find .mobilectl/backups/version -type d -mtime +30 -exec rm -rf {} \;
```

## Semantic Versioning

MobileCtl follows [Semantic Versioning 2.0.0](https://semver.org/):

```
MAJOR.MINOR.PATCH
```

### Version Format

- **MAJOR**: Incompatible API changes
- **MINOR**: Backward-compatible new features
- **PATCH**: Backward-compatible bug fixes

### Examples

```
1.0.0 → Initial release
1.0.1 → Bug fix (patch)
1.1.0 → New feature (minor)
2.0.0 → Breaking change (major)
```

## Build Number (Version Code)

Some platforms use build numbers:

### Android (versionCode)

Auto-incremented in `build.gradle`:

```gradle
versionCode 123
```

### iOS (CFBundleVersion)

Auto-incremented in `Info.plist`:

```xml
<key>CFBundleVersion</key>
<string>123</string>
```

## Integration with Other Commands

### With Changelog

```bash
# Bump version and generate changelog
mobilectl version bump minor
mobilectl changelog generate
```

### With Deploy

```bash
# Bump version before deploying
mobilectl version bump patch
mobilectl deploy --all-flavors
```

### All-in-One

```bash
# Version bump integrated into deploy
mobilectl deploy --bump-version patch
```

## Error Handling

### Version File Not Found

```
Error: Version file not found: pubspec.yaml
Suggestion: Remove from filesToUpdate or create the file
```

### Invalid Version Format

```
Error: Invalid version format in mobileops.yaml: "1.0"
Expected: MAJOR.MINOR.PATCH (e.g., 1.0.0)
```

### Validation Failure

```
Error: Version update validation failed
File: pubspec.yaml
Expected: 1.0.1
Found: 1.0.0

Backup created: version-backup-2024-01-15-14-30-00
To restore: mobilectl version restore version-backup-2024-01-15-14-30-00
```

## Best Practices

### 1. Always Use Semantic Versioning

```bash
# Breaking changes
mobilectl version bump major

# New features
mobilectl version bump minor

# Bug fixes
mobilectl version bump patch
```

### 2. Never Skip Backups in Production

```bash
# ❌ Don't do this in production
mobilectl version bump patch --skip-backup

# ✅ Let MobileCtl create backups
mobilectl version bump patch
```

### 3. Use Dry Run for Testing

```bash
# Preview changes first
mobilectl version bump minor --dry-run

# Then apply
mobilectl version bump minor
```

### 4. Configure All Version Files

```yaml
version:
  filesToUpdate:
    - pubspec.yaml          # Flutter
    - package.json          # React Native
    - android/app/build.gradle
    - ios/Runner/Info.plist
```

### 5. Combine with Changelog

```bash
# Create a release
mobilectl version bump minor
mobilectl changelog generate
git add .
git commit -m "chore: bump version to 1.1.0"
git tag v1.1.0
git push --tags
```

## Troubleshooting

### Version Not Updating

**Problem**: Version shows updated in config but not in files

**Solution**: Check `filesToUpdate` list and file permissions

```yaml
version:
  filesToUpdate:
    - path/to/your/version/file
```

### Restore Not Working

**Problem**: Cannot restore from backup

**Solution**: Check backup exists

```bash
ls .mobilectl/backups/version/
```

### Permission Denied

**Problem**: Cannot write to version files

**Solution**: Check file permissions

```bash
chmod 644 path/to/version/file
```

## Advanced Usage

### Custom Version Files

Support any file with version numbers:

```yaml
version:
  filesToUpdate:
    - VERSION.txt
    - src/constants/version.ts
    - docs/conf.py
```

### Git Integration

Automatic git tagging (coming soon):

```yaml
version:
  gitTag: true
  gitTagPrefix: "v"
```

## See Also

- [Configuration: Version](/reference/config-version)
- [Changelog Command](/reference/changelog)
- [Deploy Command](/reference/deploy)
- [Version Management Guide](/guide/version-management)
