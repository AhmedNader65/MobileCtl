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
  - icon: 🚀
    title: Setup Wizard
    details: Interactive wizard generates complete configuration. Auto-detects project, creates CI/CD workflows, and gets you deploying in 5 minutes.

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

  - icon: 🤖
    title: CI/CD Integration
    details: Auto-generate GitHub Actions and GitLab CI workflows. Perfect for automated deployments with proper secret handling.

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

Mobile app deployment shouldn't be complicated. **MobileCtl** simplifies your entire DevOps pipeline into **one setup wizard** and **one deployment command**.

### 5-Minute Setup

Start with the interactive setup wizard:

```bash
mobilectl setup
```

The wizard auto-detects your project and generates everything you need:
- Complete `mobileops.yaml` configuration
- GitHub Actions or GitLab CI workflows
- Deployment groups and strategies
- Version management settings

### One-Command Deployment

After setup, deploy with a single command:

```bash
# Complete release: version bump + changelog + build + deploy
mobilectl deploy --bump-version patch --changelog --all-variants
```

That's it. **One command** to go from code to production.

The `deploy` command intelligently:
- 🔢 Bumps version if you specify `--bump-version`
- 📝 Generates changelog if you add `--changelog` (or `-C`)
- 🏗️ Builds your apps automatically (unless you add `--skip-build`)
- 📦 Deploys to all configured destinations

Or use commands separately for more control:

```bash
mobilectl version bump patch     # Manual version control
mobilectl changelog generate     # Separate changelog step
mobilectl build all             # Build only
mobilectl deploy --all-variants  # Deploy existing builds
```

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

### Generate Configuration with Setup Wizard

Run the interactive wizard:

```bash
mobilectl setup
```

It generates your complete `mobileops.yaml`:

```yaml
# Generated by mobilectl setup wizard
app:
  name: MyAwesomeApp
  identifier: com.example.awesome
  version: 1.0.0

build:
  android:
    enabled: true
    flavors: [free, paid, premium]
    defaultFlavor: free
    defaultType: release
  ios:
    enabled: true
    project_path: ios/Runner.xcworkspace
    scheme: Runner

deploy:
  enabled: true

  android:
    firebase:
      enabled: true
      service_account: credentials/firebase-adminsdk.json
      testGroups: [qa-team, beta-testers]

    play_console:
      enabled: true
      service_account: credentials/play-console.json

  ios:
    testflight:
      enabled: true
      api_key_path: credentials/app-store-connect-api-key.json
      team_id: ABC123DEF

  flavor_groups:
    production:
      flavors: [free, paid, premium]

version:
  enabled: true
  auto_increment: true
  bump_strategy: patch

changelog:
  enabled: true
  format: markdown
  output_file: CHANGELOG.md
```

### Deploy Everything with One Command

```bash
# One command to rule them all
mobilectl deploy --all-variants --bump-version patch --changelog
```

This will:
1. Bump the version to `1.0.1`
2. Generate a changelog from commits
3. Build all flavors (free, paid, premium)
4. Sign all artifacts
5. Upload to Firebase & TestFlight
6. Update Play Console
7. Send notifications (if configured)
8. Generate deployment reports

## What's New in v0.2.0

::: tip Production Ready Changelog
- ✅ Generate changelog from conventional commits
- ✅ Group by commit type with emoji
- ✅ Multi-version append mode
- ✅ Automatic backups with restore
- ✅ Comprehensive validation
- ✅ 89% test coverage
:::

## Get Started in 5 Minutes

### Option 1: Setup Wizard (Recommended)

The easiest way to get started:

```bash
# Clone or download MobileCtl
git clone https://github.com/AhmedNader65/MobileCtl.git
cd MobileCtl && ./gradlew build

# Run the setup wizard
cd /path/to/your/mobile/project
mobilectl setup
```

The wizard will:
1. Auto-detect your project type (Android, iOS, Flutter, React Native)
2. Find existing credentials and configuration
3. Guide you through 8 setup phases
4. Generate complete `mobileops.yaml`
5. Optionally create CI/CD workflows

[Learn about the Setup Wizard →](/guide/setup-wizard)

### Option 2: Manual Configuration

```bash
# Create your config manually
cat > mobileops.yaml << EOF
app:
  name: MyApp
  identifier: com.example.myapp
  version: 1.0.0

build:
  android:
    enabled: true
    defaultType: release

deploy:
  android:
    firebase:
      enabled: true
      testGroups: [qa-team]
EOF

# Start building and deploying
./mobilectl.sh build
./mobilectl.sh deploy --all-variants
```

[Read the full guide →](/guide/getting-started)
