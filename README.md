Modern DevOps automation for mobile apps. Build, version, and deploy iOS & Android with a single command.

## ✨ Features

- 🏗️ **Build Automation** — Compile Android & iOS apps with one command
- 🔢 **Version Bumping** — Automatic semantic versioning with multi-file support
- 📝 **Changelog Generation** — Auto-generated from conventional commits with backup/restore
- 📦 **Artifact Upload** — Push builds to Firebase, local storage, etc.
- 📧 **Notifications** — Slack, email, and webhook support
- 🔒 **Production Ready** — Atomic writes, automatic backups, comprehensive validation
- ⚡ **Modern Stack** — Kotlin Multiplatform, JGit, no Ruby!

## 🚀 Quick Start

### Installation

Coming soon — Homebrew, direct download, etc.

### Basic Usage

```


# Create config file

echo "changelog:
enabled: true
format: markdown
output_file: CHANGELOG.md
commit_types:
- type: feat
title: Features
emoji: ✨
- type: fix
title: Bug Fixes
emoji: 🐛" > mobileops.yml

# Generate changelog

mobilectl changelog generate

# Bump version

mobilectl version bump patch

# Build

mobilectl build android

```

## 📋 Configuration

Create `mobileops.yml` in your project root:

```

version:
enabled: true
current: "1.0.0"
bumpStrategy: semver
filesToUpdate:
- pubspec.yaml
- package.json

changelog:
enabled: true
format: markdown
output_file: CHANGELOG.md
commit_types:
- type: feat
title: Features
emoji: ✨
- type: fix
title: Bug Fixes
emoji: 🐛
- type: docs
title: Documentation
emoji: 📚

build:
android:
enabled: true
default_type: release
ios:
enabled: true
scheme: Runner

deploy:
firebase: true
destinations:
- local

```

## 🛠️ Tech Stack

- **Language:** Kotlin Multiplatform (KMM)
- **CLI Framework:** Clikt
- **Config:** SnakeYAML + Kotlinx Serialization
- **Git:** JGit (no shell commands)
- **Build:** Gradle (Kotlin DSL)
- **Testing:** Kotlin Test + JUnit
- **CI/CD:** GitHub Actions

## 📈 Current Status

### v0.2.0 ✅ Production Ready

**Changelog Feature:**
- ✅ Generate changelog from conventional commits
- ✅ Group by commit type with emoji
- ✅ Multi-version append mode
- ✅ Automatic backups with restore
- ✅ Comprehensive validation
- ✅ 89% test coverage (85+ tests)
- ✅ SOLID architecture

**Version Feature:**
- ✅ Semantic versioning
- ✅ Multi-file version bumping
- ✅ Auto-detection of version files
- ✅ Validation and error recovery

**Next (v0.3.0):**
- 🔄 Deploy/Upload feature (Android/iOS/Web)
- 🔄 HTML changelog output
- 🔄 Pre/post hooks

## 🏃 Quick Commands

```


# Changelog

mobilectl changelog generate           \# Generate changelog
mobilectl changelog generate --dry-run \# Preview
mobilectl changelog generate --append  \# Append to existing
mobilectl changelog backups            \# List backups
mobilectl changelog restore BACKUP_ID  \# Restore backup

# Version

mobilectl version bump major   \# Bump major version
mobilectl version bump minor   \# Bump minor version
mobilectl version bump patch   \# Bump patch version

# Build

mobilectl build android        \# Build Android APK/AAB
mobilectl build ios            \# Build iOS app

# Deploy (Coming in v0.3.0)

mobilectl deploy firebase      \# Deploy to Firebase
mobilectl deploy testflight    \# Deploy to TestFlight

```

## 🔍 Key Improvements in v0.2.0

| Feature | Before | After |
|---------|--------|-------|
| Git Ops | Shell commands (unreliable) | JGit (reliable) |
| Data Safety | No backups | Automatic backups |
| Validation | Silent failures | Clear error messages |
| Performance | N+1 queries | Single query |
| Testing | Basic tests | 89% coverage |
| Architecture | Scattered logic | SOLID principles |

## 🔒 Reliability Features

- **Atomic Writes:** Safe file operations with temp files and verification
- **Automatic Backups:** Every write creates a backup automatically
- **Error Recovery:** Rollback on verification failure
- **Validation:** Config and input validation with suggestions
- **Clear Errors:** Actionable error messages with fixes

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

MIT License — see [LICENSE](LICENSE) file.

## 🗺️ Roadmap

### Current
- ✅ v0.1.0 — Version bumping
- ✅ v0.2.0 — Changelog with backup/restore (Production Ready!)

### Upcoming
- [ ] v0.3.0 — Deploy/Upload (Android/iOS/Web)
- [ ] v0.4.0 — HTML/JSON output, custom templates
- [ ] v0.5.0 — Pre/post hooks, advanced filtering
- [ ] v1.0.0 — Cloud/SaaS mode

## 💬 Support

Have questions?
- Open an [issue on GitHub](https://github.com/AhmedNader65/MobileCtl/issues)
- Check the [documentation](docs/)
- Read the [configuration guide](docs/config-reference.md)

## ⭐ Show Your Support

If mobilectl helps you, please give it a star on GitHub!

---

**Made with ❤️ for mobile developers**

v0.2.0 - Production Changelog Feature 🚀
```

