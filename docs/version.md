# Version Management

## Overview

`mobilectl version` manages your app's version across all platforms and configuration files.

**Key Features:**
- 🔍 Auto-detects current version from app files
- ⚠️  Warns on version mismatches
- 💾 Creates backups before bumping
- 🏷️  Creates git tags automatically
- 📝 Updates all version files simultaneously
- ⏮️  Can restore from backups anytime

---

## Commands

### Show Current Version

Display the current version from both app files and config.<br>
```mobilectl version show```



**Output:**
```📱 Detected version: 1.5.0
📋 Config version: 1.0.0
⚠️ Version mismatch!
App files: 1.5.0
Config: 1.0.0
→ Will use app version as source of truth
```


**Options:**
- `--verbose, -v` - Show which files will be updated

**Example:**
```mobilectl version show --verbose```

---

### Bump Version

Bump your app's version following semantic versioning.<br>
```mobilectl version bump [LEVEL]```


**Arguments:**
- `LEVEL` - Bump level: `major`, `minor`, or `patch`

**How It Works:**
1. Auto-detects current version from app files
2. Creates backup with timestamp and git tag
3. Calculates new version using semantic versioning
4. Updates all version files (build.gradle, package.json, mobileops.yaml, etc)
5. Reports all changes

**Examples:**

Bump patch version (1.5.0 → 1.5.1):<br>
```mobilectl version bump patch```

Bump minor version (1.5.0 → 1.6.0):<br>
```mobilectl version bump minor```

Bump major version (1.5.0 → 2.0.0):<br>
```mobilectl version bump major```

**Output:**
```
🔢 Bumping version: 1.5.0 → 1.5.1

📝 Files updated: 4
✅ app/build.gradle.kts
✅ build.gradle.kts
✅ package.json
✅ mobileops.yaml

💾 Backup: 1.5.0-2025-11-05_12-09-00
🏷️ Git tag: v1.5.0

✅ Version bumped: 1.5.0 → 1.5.1
```

**Options:**
- `-v, --verbose` - Show detailed output
- `--dry-run` - Preview changes without modifying files
- `--skip-backup` - Skip creating backup (not recommended)

**Examples:**

Preview changes:<br>
```mobilectl version bump patch --dry-run```


Verbose output:<br>
```mobilectl version bump minor --verbose```


Skip backup:<br>
```mobilectl version bump patch --skip-backup```

---

### Restore from Backup

Restore your app's version from a previous backup.

```mobilectl version restore [BACKUP_NAME]```

**Arguments:**
- `BACKUP_NAME` - (optional) Backup to restore. Leave empty to list available backups.

**List Available Backups:**
```mobilectl version restore```

**Output:**
```📦 Available backups:

- 1.5.1-2025-11-05_12-09-00

- 1.5.0-2025-11-05_12-05-30

- 1.4.5-2025-11-04_18-30-00
Usage: mobilectl version restore <backup-name>
```
  
**Restore Specific Backup:**<br>
```mobilectl version restore 1.5.0-2025-11-05_12-05-30```


**Output:**
```🔄 Restoring backup: 1.5.0-2025-11-05_12-05-30
✅ Backup restored successfully!
```
---

## Configuration

Add version configuration to your `mobileops.yaml`:
```version:
current: "1.5.0"
bump_strategy: "patch"
files_to_update:
- "app/build.gradle.kts"
- "package.json"
```


**Options:** 
- `current` - Current version (auto-updated when you bump)
- `bump_strategy` - Strategy for bumping (currently only `semver` supported)
- `files_to_update` - Additional files to update beyond auto-detected ones
---

## Supported Files

### Auto-Detected

These files are automatically detected and updated:

**Android:**
- `build.gradle.kts` (versionName, version)
- `build.gradle` (versionName, version)
- `app/build.gradle.kts`

**Node/React Native:**
- `package.json` (version field)

**iOS:**
- `Info.plist` (CFBundleShortVersionString)

**Flutter:**
- `pubspec.yaml` (version field)

**mobilectl:**
- `mobileops.yaml` (version.current)

### Custom Files

Add custom files to update in `mobileops.yaml`:

```version:
files_to_update:
- "my-custom-version.txt"
- "VERSION"
```
---

## Version Mismatch

If your app files have a different version than `mobileops.yaml`, mobilectl warns you:
```⚠️ Version mismatch!
App files: 1.5.0
Config: 1.0.0
→ Will use app version as source of truth```
```

**Why This Happens:**
- You manually edited version in code but forgot config
- Config is outdated
- Different branches have different versions

**Resolution:**
1. `mobilectl version show` to see the mismatch
2. Run `mobilectl version bump` to sync everything

---

## Workflow Example

```1. Check current version
mobilectl version show

2. Preview what version bump would do
mobilectl version bump patch --dry-run

3. Bump version (creates backup + updates all files)
mobilectl version bump patch --verbose

4. Verify git tag was created
git tag -l

5. If something went wrong, restore
mobilectl version restore 1.5.0-2025-11-05_12-05-30

6. Or try again
mobilectl version bump patch
```
---

## Backups

Backups are stored in `.mobilectl/backups/`:
```.mobilectl/backups/
├── 1.5.0-2025-11-05_12-09-00/
│ ├── app/build.gradle.kts
│ ├── build.gradle.kts
│ ├── package.json
│ └── mobileops.yaml
├── 1.4.9-2025-11-05_11-30-00/
│ └── ...
└── 1.4.8-2025-11-04_09-15-00/
└── ...
```

Each backup contains:
- Timestamp of when backup was created
- All version files at that moment
- Can be restored anytime with `mobilectl version restore`

---

## Semantic Versioning

Versions follow [Semantic Versioning](https://semver.org/):
```MAJOR.MINOR.PATCH
↓ ↓ ↓
1.5.0
```

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality, backwards compatible
- **PATCH**: Bug fixes, backwards compatible

**Examples:**
- `1.0.0` → `2.0.0` (major bump)
- `1.0.0` → `1.1.0` (minor bump)
- `1.0.0` → `1.0.1` (patch bump)

---

## Tips & Tricks

**Automate version bumping in CI/CD:**
In your CI/CD pipeline
mobilectl version bump patch
```git push origin --tags```

**Create release commits:**
```mobilectl version bump minor
git add -A
git commit -m "Release version X.X.X"
git push
```

**Always preview first:**
```
mobilectl version bump major --dry-run
```
Review output, then run without ```--dry-run```

---

## Troubleshooting

**Version not detected:**
Make sure your version file exists and is formatted correctly
```
mobilectl version show --verbose
```

**Files not updated:**
Check file permissions
Add files to mobileops.yaml:
version:
files_to_update:
- "path/to/my/version/file"


**Backup restore failed:**
Check backup exists
```mobilectl version restore```

Check backup is readable
```
ls -la .mobilectl/backups/
```
undefined







