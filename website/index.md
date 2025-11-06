---
layout: home

hero:
  name: MobileCtl
  text: Modern Mobile DevOps
  tagline: Build, version, and deploy iOS & Android apps with a single command. Production-ready automation for mobile development.
  image:
    src: /logo.svg
    alt: MobileCtl
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: View on GitHub
      link: https://github.com/AhmedNader65/MobileCtl

features:
  - icon: 🏗️
    title: Build Automation
    details: Compile Android & iOS apps with one command. Auto-detection, multiple flavors, and seamless configuration.

  - icon: 🔢
    title: Smart Versioning
    details: Automatic semantic versioning with multi-file support. Bump major, minor, or patch versions effortlessly.

  - icon: 📝
    title: Changelog Generation
    details: Auto-generated changelogs from conventional commits. Grouping by type, emoji support, and multi-version append mode.

  - icon: 📦
    title: Multi-Platform Deploy
    details: Deploy to Firebase, TestFlight, Play Console, and App Store. One command for all destinations.

  - icon: 🔒
    title: Production Ready
    details: Atomic writes, automatic backups, comprehensive validation, and error recovery built-in.

  - icon: ⚡
    title: Modern Stack
    details: Built with Kotlin Multiplatform, JGit, and SnakeYAML. No Ruby dependencies, just pure performance.

  - icon: 📧
    title: Smart Notifications
    details: Integrated Slack, email, and webhook support. Stay informed about every deployment.

  - icon: 🎯
    title: CI/CD Ready
    details: Perfect for GitHub Actions, GitLab CI, and Jenkins. Export reports in HTML or JSON.

  - icon: 🔧
    title: Flexible Configuration
    details: YAML-based config with environment variable support. One config file for all your mobile projects.
---

<style>
.vp-doc h2 {
  margin-top: 48px;
  border-top: 1px solid var(--vp-c-divider);
  padding-top: 24px;
}

.features-wrapper {
  margin-top: 48px;
}
</style>

## Why MobileCtl?

Mobile app deployment shouldn't be complicated. **MobileCtl** simplifies your entire DevOps pipeline:

```bash
# Bump version
mobilectl version bump patch

# Generate changelog
mobilectl changelog generate

# Build for all platforms
mobilectl build all

# Deploy to Firebase & TestFlight
mobilectl deploy --all-flavors
```

That's it. **Four commands** to go from code to production.

## Key Features

### 🎯 Zero Configuration

Auto-detects your project structure and provides smart defaults. Get started in seconds:

```bash
mobilectl build android
```

### 🔄 Backup & Restore

Every operation creates automatic backups. Made a mistake? Restore instantly:

```bash
mobilectl version restore
mobilectl changelog restore
```

### 🌍 Environment Variables

Keep secrets safe with environment variable support:

```yaml
build:
  android:
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}
```

### 📊 Comprehensive Reporting

Generate beautiful HTML or JSON reports for every build:

```yaml
report:
  enabled: true
  format: html
  output_path: ./build-reports
```

## Trusted by Developers

<div class="stats-grid">
  <div class="stat-card">
    <div class="stat-number">89%</div>
    <div class="stat-label">Test Coverage</div>
  </div>
  <div class="stat-card">
    <div class="stat-number">0</div>
    <div class="stat-label">Ruby Dependencies</div>
  </div>
  <div class="stat-card">
    <div class="stat-number">85+</div>
    <div class="stat-label">Unit Tests</div>
  </div>
  <div class="stat-card">
    <div class="stat-number">SOLID</div>
    <div class="stat-label">Architecture</div>
  </div>
</div>

<style>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  margin: 48px 0;
}

.stat-card {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  transition: all 0.3s ease;
}

.stat-card:hover {
  border-color: var(--vp-c-brand-1);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.stat-number {
  font-size: 48px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--vp-c-brand-1) 0%, var(--vp-c-accent-1) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--vp-c-text-2);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
</style>

## Quick Example

Deploy your Flutter app to Firebase and TestFlight in one command:

```yaml
# mobileops.yaml
app:
  name: MyAwesomeApp
  identifier: com.example.awesome
  version: 1.0.0

build:
  android:
    enabled: true
    defaultType: release
  ios:
    enabled: true
    scheme: Runner

deploy:
  android:
    firebase:
      enabled: true
      testGroups: [qa-team, beta-testers]
  ios:
    testflight:
      enabled: true
```

```bash
# One command to rule them all
mobilectl deploy --all-flavors --bump-version patch --changelog
```

This will:
1. Bump the version to `1.0.1`
2. Generate a changelog from commits
3. Build Android APK & iOS IPA
4. Sign both artifacts
5. Upload to Firebase & TestFlight
6. Send notifications to Slack
7. Generate deployment reports

## What's New in v0.2.0

::: tip Production Ready Changelog
- ✅ Generate changelog from conventional commits
- ✅ Group by commit type with emoji
- ✅ Multi-version append mode
- ✅ Automatic backups with restore
- ✅ Comprehensive validation
- ✅ 89% test coverage
:::

## Get Started in 60 Seconds

```bash
# Clone or download MobileCtl
git clone https://github.com/AhmedNader65/MobileCtl.git
cd MobileCtl

# Build the CLI
./gradlew build

# Create your config
cat > mobileops.yaml << EOF
app:
  name: MyApp
  version: 1.0.0

build:
  android:
    enabled: true
EOF

# Start building
./mobilectl.sh build android
```

[Read the full guide →](/guide/getting-started)
