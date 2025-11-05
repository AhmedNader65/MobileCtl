# Changelog Command Documentation

## Overview

The `mobilectl changelog` command manages your app's release notes and changelog automatically from git commits.

**Key Features:**

- 🤖 Auto-generates changelog from conventional commits
- 📝 Markdown output format
- 💾 Smart resumption (only processes new commits)
- 📊 Includes stats, contributors, and breaking changes
- 🔗 Links to commits and version comparisons
- 🏷️ Groups releases by git tags
- ⚙️ Highly configurable

***

## Commands

### Generate Changelog

Generate a new changelog from git commits.

```bash
mobilectl changelog generate [OPTIONS]
```

**Options:**

- `--from-tag TAG` - Generate commits since specific tag
- `--dry-run` - Preview without writing
- `--verbose, -v` - Verbose output
- `--append` - Append to existing changelog (default)
- `--overwrite` - Overwrite existing changelog
- `--fresh` - Ignore previous state, start from beginning

**Examples:**

Generate changelog (processes all commits on first run, only new commits after):

```bash
mobilectl changelog generate
```

Generate from specific tag:

```bash
mobilectl changelog generate --from-tag v1.0.0
```

Preview changes:

```bash
mobilectl changelog generate --dry-run --verbose
```

Force full regeneration:

```bash
mobilectl changelog generate --fresh
```


***

### Show Changelog

Display the current changelog file.

```bash
mobilectl changelog show [OPTIONS]
```

**Options:**

- `--verbose, -v` - Show file details and size

**Examples:**

View current changelog:

```bash
mobilectl changelog show
```

With details:

```bash
mobilectl changelog show --verbose
```


***

### Update Changelog

Regenerate and update the existing changelog with new commits.

```bash
mobilectl changelog update [OPTIONS]
```

**Options:**

- `--verbose, -v` - Verbose output

**Examples:**

Update changelog:

```bash
mobilectl changelog update
```

With verbose output:

```bash
mobilectl changelog update --verbose
```


***

## Configuration

Configure changelog behavior in `mobileops.yml`:

```yaml
changelog:
  enabled: true
  format: markdown
  output_file: CHANGELOG.md
  include_breaking_changes: true
  include_contributors: true
  include_stats: true
  include_compare_links: true
  group_by_version: true
  
  commit_types:
    - type: feat
      title: Features
      emoji: "✨"
    - type: fix
      title: Bug Fixes
      emoji: "🐛"
    - type: docs
      title: Documentation
      emoji: "📚"
    - type: perf
      title: Performance
      emoji: "⚡"
    - type: refactor
      title: Refactoring
      emoji: "♻️"
  
  releases:
    "1.5.0":
      highlights: "Major UI redesign and performance improvements"
      breaking_changes:
        - "API v1 endpoints removed"
        - "Configuration format changed"
      contributors:
        - "Ahmed Nader"
        - "Jane Doe"
```

**Configuration Options:**


| Option | Type | Default | Description |
| :-- | :-- | :-- | :-- |
| `enabled` | boolean | true | Enable/disable changelog feature |
| `format` | string | markdown | Default output format |
| `output_file` | string | CHANGELOG.md | Output file path |
| `include_breaking_changes` | boolean | true | Show breaking changes section |
| `include_contributors` | boolean | true | List contributors |
| `include_stats` | boolean | true | Show statistics |
| `include_compare_links` | boolean | true | Add version comparison links |
| `group_by_version` | boolean | true | Group commits by version |
| `commit_types` | array | defaults | Custom commit types |
| `releases` | map | empty | Release-specific notes |


***

## Commit Format

The changelog uses [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Examples:**

```bash
git commit -m "feat(auth): add two-factor authentication"
git commit -m "fix(api): resolve null pointer in user endpoint"
git commit -m "docs(readme): update installation instructions"
git commit -m "perf(cache): optimize memory usage by 50%"
```

**Breaking Changes:**

Indicate breaking changes in commit body:

```bash
git commit -m "feat(api): redesign user endpoint

BREAKING CHANGE: /v1/users endpoint removed, use /v2/users instead"
```


***

## Output Examples

### Markdown Format

```markdown
# Changelog

## [1.5.0] - 2025-11-05

### 📢 Highlights

Major UI redesign with improved performance and better mobile support.

### 🚨 BREAKING CHANGES

- API v1 endpoints removed ([abc123](https://github.com/user/repo/commit/abc123))
- Configuration format changed from YAML to JSON ([def456](https://github.com/user/repo/commit/def456))

### ✨ Features

- Add dark mode support ([1a2b3c](https://github.com/user/repo/commit/1a2b3c))
- Implement real-time notifications ([2b3c4d](https://github.com/user/repo/commit/2b3c4d))

### 🐛 Bug Fixes

- Fix crash on login ([3c4d5e](https://github.com/user/repo/commit/3c4d5e))
- Fix memory leak in cache ([4d5e6f](https://github.com/user/repo/commit/4d5e6f))

### 👥 Contributors

- Ahmed Nader (8 commits)
- Jane Doe (5 commits)
- John Smith (2 commits)

### 📊 Stats

- Total commits: 15
- Contributors: 3
- Breaking changes: 2

[View all changes](https://github.com/user/repo/compare/v1.4.9...v1.5.0)
```


***

## Workflow Examples

### First-Time Setup

```bash
# 1. Create initial changelog from all commits
mobilectl changelog generate

# 2. View the generated changelog
mobilectl changelog show

# 3. Commit to git
git add CHANGELOG.md
git commit -m "docs: add changelog"
```


### Regular Release Process

```bash
# 1. Check current version
mobilectl version show

# 2. Bump version
mobilectl version bump minor

# 3. Generate updated changelog (only new commits)
mobilectl changelog generate

# 4. Review changes
mobilectl changelog show --verbose

# 5. Create release commit
git add -A
git commit -m "chore: release v1.5.0"
git tag v1.5.0
```

### Append vs Overwrite

```bash
# Append new commits to existing changelog (default)
mobilectl changelog generate

# Completely regenerate from scratch
mobilectl changelog generate --fresh

# Overwrite with new content
mobilectl changelog generate --overwrite
```


### Preview Before Committing

```bash
# Preview changes
mobilectl changelog generate --dry-run

```


***

## State Management

The changelog tracks its progress in `.mobilectl/changelog-state.json`:

```json
{
  "lastGeneratedCommit": "89065e4b048deac6ce34fc1593e1ce0218af4952",
  "lastGeneratedDate": "2025-11-05T14:02:50.794096800",
  "lastGeneratedVersion": "v1.5.0",
  "lastGeneratedRange": "89065e4b048deac6ce34fc1593e1ce0218af4952..v1.5.0"
}
```

This allows the tool to resume from where it left off and only process new commits.

**Reset state:**

```bash
rm .mobilectl/changelog-state.json
mobilectl changelog generate --fresh
```


***

## Tips \& Best Practices

### Use Conventional Commits

Always follow conventional commit format for better changelog organization:

```bash
git commit -m "feat: add new feature"
git commit -m "fix: resolve bug"
git commit -m "docs: update docs"
```


### Tag Releases

Create git tags for each release version:

```bash
git tag v1.5.0
git tag v1.4.9
git tag v1.4.8
```


### Add Release Notes

Add highlights and breaking changes in config:

```yaml
releases:
  "1.5.0":
    highlights: "Major update with performance improvements"
    breaking_changes:
      - "Old API removed"
```


### Automate in CI/CD

Add to your CI/CD pipeline:

```bash
#!/bin/bash
mobilectl version bump minor
mobilectl changelog generate
git add -A
git commit -m "chore: release $(mobilectl version show)"
git tag v$(mobilectl version show | grep Detected | awk '{print $3}')
git push origin --tags
```


***

## Troubleshooting

**No commits found:**

```bash
# Check git history
git log --oneline

# Verify config
cat mobileops.yml | grep -A5 changelog
```

**Changelog not updating:**

```bash
# Reset state and regenerate
rm .mobilectl/changelog-state.json
mobilectl changelog generate --fresh
```

**Wrong commit format:**

```bash
# Follow conventional commits
git commit -m "feat(scope): description"
```

**File not found:**

```bash
# Check output file path in config
mobilectl changelog show

# Create directory if needed
mkdir -p $(dirname CHANGELOG.md)
```


***

## Integration Examples

### GitHub Actions

```yaml
- name: Generate Changelog
  run: |
    mobilectl changelog generate
    git add CHANGELOG.md
    git commit -m "docs: update changelog" || true
```


### Pre-push Hook

```bash
#!/bin/bash
mobilectl changelog generate --dry-run
echo "Changelog preview above. Continue? (y/n)"
read -r response
[[ "$response" == "y" ]] && mobilectl changelog generate
```


### Manual Release Script

```bash
#!/bin/bash
set -e

VERSION=$1
mobilectl version bump major
mobilectl changelog generate
git add -A
git commit -m "release: v$VERSION"
git tag "v$VERSION"
echo "✅ Release v$VERSION created"
```

