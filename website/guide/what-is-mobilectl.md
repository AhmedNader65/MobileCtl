# What is MobileCtl?

**MobileCtl** is a modern, production-ready DevOps automation tool for mobile app development. It simplifies the entire mobile deployment pipeline with a single, unified CLI.

## The Problem

Mobile app deployment is unnecessarily complex:

- **Multiple platforms**: Different commands for Android and iOS
- **Version management**: Manually updating version numbers across files
- **Changelog creation**: Writing release notes by hand
- **Build complexity**: Different build types, flavors, and signing configurations
- **Deployment chaos**: Different upload processes for each app store
- **Notification fatigue**: Manually informing teams about releases

## The Solution

MobileCtl unifies your entire mobile DevOps workflow into simple, intuitive commands:

```bash
# Instead of this mess:
cd android && ./gradlew assembleRelease
cd ../ios && xcodebuild -scheme MyApp -configuration Release
# ... manually bump versions in 5 different files
# ... manually write changelog
# ... upload to Firebase manually
# ... upload to TestFlight manually
# ... send Slack notification manually

# Just do this:
mobilectl deploy --all-flavors --bump-version patch --changelog
```

## Core Features

### 🏗️ Build Automation

Compile Android and iOS apps with a single command:

```bash
mobilectl build android          # Build Android
mobilectl build ios              # Build iOS
mobilectl build all              # Build both
```

Auto-detection, multiple flavors, build types, and signing - all handled automatically.

### 🔢 Version Management

Semantic versioning across all your version files:

```bash
mobilectl version bump major     # 1.2.3 → 2.0.0
mobilectl version bump minor     # 1.2.3 → 1.3.0
mobilectl version bump patch     # 1.2.3 → 1.2.4
```

Updates `pubspec.yaml`, `package.json`, `build.gradle`, and any file you configure.

### 📝 Changelog Generation

Generate beautiful changelogs from your git commits:

```bash
mobilectl changelog generate
```

Supports conventional commits, custom commit types, emoji, breaking changes, and multi-version append mode.

### 📦 Multi-Platform Deployment

Deploy to multiple destinations with one command:

```bash
mobilectl deploy firebase        # Firebase App Distribution
mobilectl deploy testflight      # iOS TestFlight
mobilectl deploy play-console    # Google Play Console
mobilectl deploy --all-flavors   # All configured destinations
```

### 🔒 Production Ready

Built with production reliability in mind:

- **Atomic writes**: Safe file operations with temp files
- **Automatic backups**: Every write creates a backup
- **Error recovery**: Rollback on verification failure
- **Comprehensive validation**: Catch errors before they happen
- **Clear error messages**: Actionable feedback

## Technology Stack

MobileCtl is built with modern, reliable technologies:

- **Kotlin Multiplatform (KMM)**: Cross-platform, type-safe, and performant
- **Clikt**: Intuitive CLI framework with great UX
- **JGit**: Pure Java Git implementation (no shell commands)
- **SnakeYAML**: Robust YAML parsing
- **Kotlinx Serialization**: Type-safe configuration

**No Ruby dependencies** - everything is pure Kotlin/JVM.

## Philosophy

MobileCtl follows these principles:

1. **Simple by default, powerful when needed**: Works out of the box, but highly configurable
2. **Safety first**: Backups, validation, and atomic operations
3. **Developer experience**: Clear errors, helpful suggestions, beautiful output
4. **CI/CD friendly**: Designed for automation
5. **No magic**: Transparent operations, clear logs

## Use Cases

### Solo Developers

Simplify your deployment workflow without complex scripts:

```bash
mobilectl deploy --bump-version patch --changelog
```

### Teams

Standardize deployments across team members:

```yaml
# mobileops.yaml - committed to git
deploy:
  android:
    firebase:
      testGroups: [qa-team, beta-testers]
```

### CI/CD Pipelines

Perfect for GitHub Actions, GitLab CI, Jenkins:

```yaml
- name: Deploy to Firebase
  run: mobilectl deploy firebase --confirm
```

### Multi-App Organizations

One tool for all your mobile projects:

```bash
# Project A
cd project-a && mobilectl deploy production

# Project B
cd ../project-b && mobilectl deploy staging
```

## What Makes It Different?

| Feature | MobileCtl | Traditional Tools |
|---------|-----------|-------------------|
| **Languages** | Kotlin (JVM) | Ruby, Shell scripts |
| **Platforms** | Android + iOS | Usually separate |
| **Configuration** | Single YAML file | Multiple config files |
| **Git Operations** | JGit (reliable) | Shell commands (fragile) |
| **Backups** | Automatic | Manual or none |
| **Error Handling** | Comprehensive | Often silent failures |
| **Testing** | 89% coverage | Variable |
| **Architecture** | SOLID principles | Often ad-hoc |

## Current Status

### ✅ Production Ready (v0.3.2)

- Version bumping with multi-file support
- Changelog generation with backups
- Build automation for Android & iOS
- Deploy to Firebase, TestFlight, Play Console
- Comprehensive test coverage (89%)
- SOLID architecture

### 🔄 Coming Soon

- HTML/JSON changelog output
- Custom templates
- Pre/post hooks
- Advanced filtering
- Cloud/SaaS mode

## Next Steps

Ready to get started?

- [Installation Guide](/guide/installation)
- [Quick Start Tutorial](/guide/quick-start)
- [Configuration Reference](/reference/configuration)

Have questions?

- [GitHub Issues](https://github.com/AhmedNader65/MobileCtl/issues)
- [View Examples](/examples/)
