# Changelog Generation

Generate beautiful changelogs from git commits.

## Overview

MobileCtl generates changelogs from conventional commits automatically.

## Basic Usage

```bash
mobilectl changelog generate
mobilectl changelog generate --dry-run
mobilectl changelog generate --append
```

## Conventional Commits

Use conventional commit format:

```bash
git commit -m "feat: add new feature"
git commit -m "fix: resolve bug"
git commit -m "docs: update readme"
```

## Configuration

```yaml
changelog:
  enabled: true
  outputFile: CHANGELOG.md
  commitTypes:
    - type: feat
      title: Features
      emoji: ✨
```

See [Changelog Command Reference](/reference/changelog) for full details.
