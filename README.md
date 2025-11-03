# mobilectl

Modern DevOps automation for mobile apps. Build, version, and deploy iOS & Android with a single command.

## ✨ Features

- 🏗️ **Build Automation** — Compile Android & iOS apps with one command
- 🔢 **Version Bumping** — Automatic semantic versioning
- 📝 **Changelog Generation** — Auto-generated from git commits
- 📦 **Artifact Upload** — Push builds to Firebase, local storage, etc.
- 📧 **Notifications** — Slack, email, and webhook support
- 📊 **Beautiful Reports** — HTML/Markdown build summaries
- ⚡ **Modern Stack** — Kotlin Multiplatform, fast startup, no Ruby!

## 🚀 Quick Start

### Installation
Coming soon — instructions for Homebrew, direct download, etc.

### Basic Usage
Create mobileops.yml in your project root
mobilectl build android # Build Android app
mobilectl version bump # Bump version (major/minor/patch)
mobilectl changelog generate # Generate changelog
mobilectl upload --destination firebase # Upload artifact

## 📋 Configuration

Create `mobileops.yml` in your project root:

build:
android:
gradle_task: assembleRelease
ios:
scheme: MyApp
configuration: Release

deploy:
firebase: true
destinations: [local]

notify:
slack:
webhook_url: ${SLACK_WEBHOOK_URL}
email: true


## 🛠️ Tech Stack

- **Language:** Kotlin Multiplatform (KMM)
- **CLI Framework:** Clikt
- **Config:** SnakeYAML
- **Build:** Gradle (Kotlin DSL)
- **Testing:** Kotest + JUnit
- **CI/CD:** GitHub Actions

## 📚 Documentation

- [Getting Started Guide](docs/getting-started.md)
- [Configuration Reference](docs/config-reference.md)
- [CLI Commands](docs/cli-commands.md)
- [Contributing](CONTRIBUTING.md)

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

MIT License — see [LICENSE](LICENSE) file.

## 🗺️ Roadmap

- [ ] MVP v0.1.0 — Core features (build, version, changelog, upload, notify)
- [ ] v0.2.0 — Advanced integrations (AppCenter, TestFlight)
- [ ] v0.3.0 — GUI (Compose Desktop)
- [ ] v1.0.0 — Cloud/SaaS mode

## 💬 Support

Have questions? Open an issue on GitHub or reach out to the team.

---

**Made with ❤️ for mobile developers**
