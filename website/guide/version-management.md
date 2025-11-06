# Version Management

Manage app versions with semantic versioning.

## Overview

MobileCtl handles version bumping across multiple files automatically.

## Basic Usage

```bash
mobilectl version show
mobilectl version bump patch
mobilectl version bump minor
mobilectl version bump major
```

## Configuration

```yaml
version:
  enabled: true
  current: 1.0.0
  filesToUpdate:
    - pubspec.yaml
    - package.json
```

## Version Strategies

- **patch**: Bug fixes (1.0.0 → 1.0.1)
- **minor**: New features (1.0.0 → 1.1.0)
- **major**: Breaking changes (1.0.0 → 2.0.0)

See [Version Command Reference](/reference/version) for full details.
