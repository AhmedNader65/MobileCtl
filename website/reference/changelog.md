# changelog

Generate beautiful changelogs from your git commit history using conventional commits.

## Synopsis

```bash
mobilectl changelog <subcommand> [options]
```

## Subcommands

| Subcommand | Description |
|------------|-------------|
| `generate` | Generate changelog from git commits |
| `show` | Display current changelog |
| `update` | Update existing changelog |
| `restore [backup]` | Restore changelog from backup |

## changelog generate

Generate a changelog from git commit history.

### Usage

```bash
mobilectl changelog generate [options]
```

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--from-tag <tag>` | | Generate from specific git tag |
| `--verbose` | `-v` | Verbose output with detailed logs |
| `--dry-run` | | Preview changelog without saving |
| `--append` | | Append to existing changelog (default: true) |
| `--overwrite` | | Overwrite existing changelog |
| `--fresh` | | Ignore last state, start from beginning |

### Examples

```bash
# Generate changelog
mobilectl changelog generate

# Preview without saving
mobilectl changelog generate --dry-run

# Generate from specific tag
mobilectl changelog generate --from-tag v1.0.0

# Overwrite existing
mobilectl changelog generate --overwrite

# Start fresh (ignore last generated state)
mobilectl changelog generate --fresh
```

### What It Does

1. **Reads git history** since last generation (or specified tag)
2. **Parses commits** using conventional commit format
3. **Groups commits** by type (feat, fix, docs, etc.)
4. **Adds emoji** based on commit type
5. **Formats output** in markdown
6. **Creates backup** of existing changelog
7. **Writes/appends** to CHANGELOG.md

### Output Example

```markdown
# Changelog

## [1.0.1] - 2024-01-15

### ✨ Features
- Add dark mode support (#42)
- Implement user profile page (#45)

### 🐛 Bug Fixes
- Fix login redirect issue (#43)
- Resolve memory leak in image loader (#46)

### 📚 Documentation
- Update installation guide
- Add API documentation

### 🔧 Chores
- Update dependencies
- Bump version to 1.0.1

---

## [1.0.0] - 2024-01-01

### ✨ Features
- Initial release
- User authentication
- Product catalog
```

## changelog show

Display the current changelog.

### Usage

```bash
mobilectl changelog show [options]
```

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--verbose` | `-v` | Show detailed information |

### Example

```bash
mobilectl changelog show
```

## changelog update

Update existing changelog with new commits.

### Usage

```bash
mobilectl changelog update [options]
```

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--verbose` | `-v` | Verbose output |

### Example

```bash
mobilectl changelog update
```

## changelog restore

Restore changelog from automatic backup.

### Usage

```bash
mobilectl changelog restore [backup-id] [options]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `backup-id` | No | Backup ID to restore (omit to list backups) |

### Options

| Option | Short | Description |
|--------|-------|-------------|
| `--verbose` | `-v` | Verbose output |

### Examples

```bash
# List available backups
mobilectl changelog restore

# Restore specific backup
mobilectl changelog restore changelog-backup-2024-01-15-14-30-00
```

### Listing Backups

```
Available changelog backups:

1. changelog-backup-2024-01-15-14-30-00
   Version: 1.0.1
   Date: 2024-01-15 14:30:00
   Size: 4.2 KB

2. changelog-backup-2024-01-14-10-15-00
   Version: 1.0.0
   Date: 2024-01-14 10:15:00
   Size: 2.8 KB

To restore, run:
  mobilectl changelog restore <backup-id>
```

## Conventional Commits

MobileCtl uses the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Commit Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Supported Types

| Type | Emoji | Description | Changelog Section |
|------|-------|-------------|-------------------|
| `feat` | ✨ | New feature | Features |
| `fix` | 🐛 | Bug fix | Bug Fixes |
| `docs` | 📚 | Documentation | Documentation |
| `style` | 💎 | Code style/formatting | Styles |
| `refactor` | ♻️ | Code refactoring | Refactoring |
| `perf` | ⚡ | Performance improvement | Performance |
| `test` | ✅ | Tests | Tests |
| `build` | 🏗️ | Build system | Build |
| `ci` | 👷 | CI/CD | CI/CD |
| `chore` | 🔧 | Maintenance | Chores |

### Examples

```bash
# Feature
git commit -m "feat: add dark mode toggle"

# Bug fix
git commit -m "fix: resolve login redirect issue"

# Breaking change
git commit -m "feat!: redesign authentication flow

BREAKING CHANGE: Old authentication tokens are no longer valid"

# With scope
git commit -m "feat(auth): add OAuth2 support"

# Multiple types
git commit -m "feat: add user profile

Also includes:
- fix: profile image upload
- docs: update user guide"
```

## Configuration

Configure changelog generation in `mobileops.yaml`:

```yaml
changelog:
  enabled: true
  format: markdown
  outputFile: CHANGELOG.md
  fromTag: null
  append: true
  useLastState: true
  dryRun: false
  includeBreakingChanges: true
  includeContributors: true
  includeStats: true
  includeCompareLinks: true
  groupByVersion: true

  commitTypes:
    - type: feat
      title: Features
      emoji: ✨
    - type: fix
      title: Bug Fixes
      emoji: 🐛
    - type: docs
      title: Documentation
      emoji: 📚
    - type: style
      title: Styles
      emoji: 💎
    - type: refactor
      title: Refactoring
      emoji: ♻️
    - type: perf
      title: Performance
      emoji: ⚡
    - type: test
      title: Tests
      emoji: ✅
    - type: build
      title: Build
      emoji: 🏗️
    - type: ci
      title: CI/CD
      emoji: 👷
    - type: chore
      title: Chores
      emoji: 🔧

  releases:
    1.0.0:
      highlights:
        - "Initial production release"
        - "Complete authentication system"
      breakingChanges:
        - "API endpoints restructured"
      contributors:
        - "John Doe <john@example.com>"
        - "Jane Smith <jane@example.com>"
```

[See full configuration reference →](/reference/config-changelog)

## Advanced Features

### Release Notes

Add custom release notes for specific versions:

```yaml
changelog:
  releases:
    1.0.0:
      highlights:
        - "🎉 First production release!"
        - "Complete rewrite with new architecture"
      breakingChanges:
        - "Old API endpoints removed"
        - "Database schema changed"
      contributors:
        - "Alice <alice@example.com>"
        - "Bob <bob@example.com>"
```

Output:
```markdown
## [1.0.0] - 2024-01-15

**Highlights:**
- 🎉 First production release!
- Complete rewrite with new architecture

**⚠️ Breaking Changes:**
- Old API endpoints removed
- Database schema changed

**Contributors:**
- Alice <alice@example.com>
- Bob <bob@example.com>

### ✨ Features
...
```

### Breaking Changes

Detected automatically from commits:

```bash
git commit -m "feat!: redesign API

BREAKING CHANGE: All endpoints now require authentication"
```

Output:
```markdown
## [2.0.0] - 2024-01-20

**⚠️ Breaking Changes:**
- All endpoints now require authentication (feat)

### ✨ Features
- Redesign API
```

### Contributors

Automatically includes commit authors:

```markdown
**Contributors:**
- John Doe <john@example.com> (12 commits)
- Jane Smith <jane@example.com> (8 commits)
```

### Statistics

```markdown
**Statistics:**
- Total commits: 20
- Features: 8
- Bug fixes: 10
- Documentation: 2
```

### Compare Links

Links to git compare view:

```markdown
## [1.0.1] - 2024-01-15

[Compare v1.0.0...v1.0.1](https://github.com/user/repo/compare/v1.0.0...v1.0.1)
```

## Multi-Version Support

### Append Mode (Default)

Adds new version to top of existing changelog:

```bash
mobilectl changelog generate --append
```

Before:
```markdown
# Changelog

## [1.0.0] - 2024-01-01
...
```

After:
```markdown
# Changelog

## [1.0.1] - 2024-01-15
...

## [1.0.0] - 2024-01-01
...
```

### Overwrite Mode

Replaces entire changelog:

```bash
mobilectl changelog generate --overwrite
```

### Fresh Start

Ignore previously generated state:

```bash
mobilectl changelog generate --fresh
```

## Backup System

### Automatic Backups

Every changelog generation creates a backup:

```
.mobilectl/backups/changelog/
├── changelog-backup-2024-01-15-14-30-00.md
├── changelog-backup-2024-01-14-10-15-00.md
└── changelog-backup-2024-01-13-09-00-00.md
```

### Restore from Backup

```bash
# List backups
mobilectl changelog restore

# Restore specific
mobilectl changelog restore changelog-backup-2024-01-15-14-30-00
```

## Integration with Other Commands

### With Version Bump

```bash
# Bump version and generate changelog
mobilectl version bump minor
mobilectl changelog generate
```

### With Deploy

```bash
# Generate changelog before deploy
mobilectl changelog generate
mobilectl deploy --all-flavors

# Or automatic
mobilectl deploy --changelog
```

### Complete Workflow

```bash
# All-in-one release
mobilectl deploy \
  --bump-version minor \
  --changelog \
  --all-flavors
```

## Filtering Commits

### By Tag Range

```bash
# From specific tag to HEAD
mobilectl changelog generate --from-tag v1.0.0

# Between tags (coming soon)
mobilectl changelog generate --from v1.0.0 --to v1.1.0
```

### By Date (Coming Soon)

```bash
mobilectl changelog generate --since "2024-01-01"
```

### By Type (Coming Soon)

```bash
mobilectl changelog generate --types feat,fix
```

## Output Formats

### Markdown (Default)

```yaml
changelog:
  format: markdown
  outputFile: CHANGELOG.md
```

### HTML (Coming Soon)

```yaml
changelog:
  format: html
  outputFile: changelog.html
```

### JSON (Coming Soon)

```yaml
changelog:
  format: json
  outputFile: changelog.json
```

## Custom Templates (Coming Soon)

```yaml
changelog:
  template: .mobilectl/changelog-template.md
```

## Best Practices

### 1. Use Conventional Commits

```bash
# Good
git commit -m "feat: add user profile"
git commit -m "fix: resolve login bug"

# Bad
git commit -m "updates"
git commit -m "misc changes"
```

### 2. Write Descriptive Commit Messages

```bash
# Good
git commit -m "feat: implement OAuth2 authentication

- Add Google OAuth provider
- Add GitHub OAuth provider
- Store tokens securely
- Add logout functionality"

# Bad
git commit -m "feat: add auth"
```

### 3. Use Breaking Change Notation

```bash
git commit -m "feat!: redesign API

BREAKING CHANGE: All endpoints now use /api/v2 prefix"
```

### 4. Generate Regularly

```bash
# Before each release
mobilectl version bump minor
mobilectl changelog generate
git add CHANGELOG.md
git commit -m "docs: update changelog for v1.1.0"
git tag v1.1.0
```

### 5. Review Before Committing

```bash
# Preview first
mobilectl changelog generate --dry-run

# Then generate
mobilectl changelog generate
```

## Troubleshooting

### No Commits Found

```
Warning: No new commits found since last generation

Suggestions:
  1. Check if you have new commits
  2. Use --fresh to regenerate from beginning
  3. Specify tag with --from-tag
```

### Invalid Commit Format

Non-conventional commits are included under "Other Changes":

```markdown
### Other Changes
- WIP: work in progress
- fixed stuff
```

### Empty Changelog

```
Error: No commits match conventional format

Suggestions:
  1. Ensure commits follow conventional commit format
  2. Example: feat: add new feature
  3. See: https://www.conventionalcommits.org/
```

## See Also

- [Configuration: Changelog](/reference/config-changelog)
- [Version Command](/reference/version)
- [Deploy Command](/reference/deploy)
- [Changelog Guide](/guide/changelog)
- [Conventional Commits](https://www.conventionalcommits.org/)
