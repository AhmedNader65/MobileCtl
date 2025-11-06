# Version Configuration

Configure version management.

## Overview

```yaml
version:
  enabled: true
  current: 1.0.0
  filesToUpdate:
    - pubspec.yaml
    - package.json
```

## Options

| Option | Type | Description |
|--------|------|-------------|
| `enabled` | Boolean | Enable version management |
| `current` | String | Current version number |
| `autoIncrement` | Boolean | Auto-increment version |
| `bumpStrategy` | String | Default bump strategy |
| `filesToUpdate` | String[] | Files to update |

See [Version Command](/reference/version) for details.
