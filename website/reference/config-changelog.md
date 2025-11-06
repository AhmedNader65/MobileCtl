# Changelog Configuration

Configure changelog generation.

## Overview

```yaml
changelog:
  enabled: true
  outputFile: CHANGELOG.md
  commitTypes:
    - type: feat
      title: Features
      emoji: ✨
```

## Options

| Option | Type | Description |
|--------|------|-------------|
| `enabled` | Boolean | Enable changelog generation |
| `format` | String | Output format (markdown) |
| `outputFile` | String | Output file path |
| `append` | Boolean | Append to existing |
| `commitTypes` | Array | Commit type definitions |

See [Changelog Command](/reference/changelog) for details.
