# MobileCtl - Technical Overview

## Project Description

MobileCtl is a modern DevOps automation tool for mobile application development, designed to streamline the build, versioning, and deployment workflows for iOS and Android applications. Built with Kotlin Multiplatform, it provides a unified CLI interface for managing the entire mobile app release pipeline through a single command-line tool.

## Architecture

### Technology Stack

- **Language**: Kotlin Multiplatform (JVM target)
- **Build System**: Gradle with Kotlin DSL
- **CLI Framework**: kotlinx-cli for command parsing
- **Git Integration**: JGit (pure Java Git implementation)
- **HTTP Client**: OkHttp3 for API communication
- **YAML Parsing**: SnakeYAML for configuration files
- **Serialization**: kotlinx-serialization
- **Testing**: Kotlin Test framework

### Design Principles

- **SOLID Architecture**: Single Responsibility, Dependency Injection
- **Kotlin Multiplatform**: Shared business logic between JVM targets
- **Type Safety**: Leveraging Kotlin's type system for compile-time guarantees
- **Atomic Operations**: File operations with automatic backups and rollback
- **Zero Dependencies**: No Ruby or platform-specific runtime requirements

## Module Structure

### 1. CLI Module (`/cli`)

Entry point and command-line interface implementation.

**Package Structure:**
```
cli/src/main/kotlin/com/mobilectl/
├── commands/
│   ├── buildCommand/        # Build orchestration
│   ├── changelog/           # Changelog management (generate, show, update, restore)
│   ├── deploy/              # Deployment workflow orchestration
│   │   ├── ConfigurationService.kt      # Config loading & merging
│   │   ├── DeployHandler.kt             # Main deployment coordinator
│   │   ├── DeploymentWorkflow.kt        # Business logic orchestration
│   │   ├── DeploymentPresenter.kt       # UI/output formatting
│   │   ├── InteractiveDeploymentWizard.kt
│   │   └── SmartDefaultsProvider.kt     # Auto-detection logic
│   ├── setup/               # Interactive setup wizard
│   │   ├── ProjectDetector.kt           # Platform & config detection
│   │   ├── SetupHandler.kt
│   │   ├── SetupWizard.kt              # 8-phase interactive wizard
│   │   └── WorkflowGenerator.kt         # CI/CD config generation
│   ├── version/             # Version management (bump, show, restore)
│   └── *.Command.kt         # Command definitions
├── main.kt                  # CLI entry point
└── MobileCtl.kt            # Main application class
```

**Key Components:**
- Command handlers follow Single Responsibility Principle
- Each command delegates to specialized services
- Separation of concerns: Handler → Workflow → Service → Presenter

### 2. Shared Module (`/shared`)

Core business logic shared across platforms, organized using Kotlin Multiplatform conventions.

**Module Layout:**
```
shared/src/
├── commonMain/kotlin/       # Platform-agnostic code
│   └── com/mobilectl/
│       ├── builder/         # Build orchestration interfaces
│       ├── changelog/       # Changelog generation logic
│       ├── config/          # Configuration models & parsing
│       ├── deploy/          # Deployment strategies
│       ├── detector/        # Project & platform detection
│       ├── model/           # Data models & DTOs
│       ├── util/            # Utilities (expect declarations)
│       ├── validation/      # Config & file validation
│       └── version/         # Version management
│
├── jvmMain/kotlin/          # JVM-specific implementations
│   └── com/mobilectl/
│       ├── builder/         # Android/iOS builders (JVM)
│       │   ├── AndroidBuilder.kt        # Gradle-based Android builds
│       │   ├── IosBuilder.kt            # xcodebuild wrapper
│       │   ├── BuildCache.kt            # Smart rebuild detection
│       │   └── ArtifactCache.kt         # APK/IPA path caching
│       ├── changelog/
│       │   └── JvmGitCommitParser.kt    # JGit integration
│       ├── config/
│       │   └── SnakeYamlParser.kt       # YAML serialization
│       ├── deploy/
│       │   ├── firebase/                # Firebase App Distribution
│       │   │   ├── FirebaseHttpClient.kt
│       │   │   ├── GoogleServicesParser.kt
│       │   │   └── ServiceAccountAuth.kt
│       │   ├── FirebaseAndroidUploader.kt
│       │   └── BaseUploadStrategy.kt
│       ├── detector/
│       │   └── JvmProjectDetector.kt    # File system detection
│       ├── util/
│       │   ├── ApkAnalyzer.kt           # APK metadata extraction (aapt/aapt2)
│       │   ├── ArtifactDetector.kt      # Smart artifact finding
│       │   ├── JvmLogger.kt
│       │   └── JvmProcessExecutor.kt
│       └── version/
│           └── JvmVersionBumper.kt      # File-based version updates
│
└── commonTest/kotlin/       # Shared tests
    └── com/mobilectl/
        ├── config/
        │   └── ConfigParserTest.kt
        └── deploy/
            └── FirebaseAndroidUploaderManualTest.kt
```

## Core Components

### Build System

- **AndroidBuilder**: Gradle wrapper integration with smart module detection
  - Auto-detects `app`, `android`, or custom module names
  - Finds build tools in `ANDROID_HOME`, `local.properties`, or common paths
  - Handles APK signing with keystore management
  - Smart caching to avoid unnecessary rebuilds

- **IosBuilder**: xcodebuild wrapper for iOS compilation
  - Supports scheme-based builds
  - Code signing with provisioning profiles
  - IPA generation with multiple export formats

- **BuildCache**: Intelligent rebuild detection
  - Tracks source file modification times
  - Cache invalidation on config changes
  - Hash-based validation

### Configuration Management

- **Config Models** (`/model`): Strongly-typed configuration
  - `Config`: Root configuration object
  - `BuildConfig`: Android/iOS build settings
  - `DeployConfig`: Deployment destinations
  - `VersionConfig`: Version management rules
  - `ChangelogConfig`: Changelog generation settings
  - `NotifyConfig`: Notification channels

- **SnakeYamlParser**: YAML serialization with proper formatting
  - Block-style list formatting (not JSON flow style)
  - Empty list/array filtering
  - Custom DumperOptions for clean output
  - Bidirectional parse/serialize support

### Deployment Pipeline

- **DeployOrchestrator**: Multi-destination deployment coordinator
  - Parallel deployment to multiple destinations
  - Individual result tracking per destination
  - Graceful failure handling

- **Upload Strategies**:
  - `FirebaseAndroidUploader`: Firebase App Distribution
  - `PlayConsoleUploader`: Google Play Console (planned)
  - `TestFlightUploader`: Apple TestFlight (planned)

- **Firebase Integration**:
  - Service account authentication (OAuth2 JWT)
  - APK package ID extraction for config matching
  - google-services.json auto-detection
  - Distribution to tester groups
  - Clean error messages (no HTML dumps)

### Version Management

- **VersionBumper**: Semantic versioning automation
  - Strategies: patch, minor, major, auto
  - Multi-file updates (build.gradle.kts, Info.plist, package.json)
  - Atomic writes with automatic backups
  - Rollback capability

### Changelog Generation

- **ChangelogOrchestrator**: Git commit analysis
  - Conventional commit parsing
  - Commit type grouping (feat, fix, docs, etc.)
  - Breaking changes detection
  - Contributor attribution
  - Markdown/JSON/plain text output

### Smart Detection

- **ProjectDetector**: Auto-detect project configuration
  - Platform detection (Android/iOS/Flutter/React Native)
  - Build tool identification (Gradle/Xcode)
  - Package ID extraction
  - Default configuration suggestions

- **ApkAnalyzer**: APK metadata extraction
  - Package ID via aapt/aapt2
  - Version code/name extraction
  - Multi-strategy tool detection
  - Zero manual configuration required

### Artifact Management

- **ArtifactCache**: Build-to-deploy path caching
  - Per-flavor/type cache keys
  - Thread-safe concurrent access
  - Avoids re-searching filesystem
  - Handles multiple flavors without conflicts

- **ArtifactDetector**: Smart artifact finding
  - Android module detection (`app/`, `android/`, custom)
  - Common path scanning
  - Most recent file selection
  - AAB/APK/IPA support

## File Organization

### Configuration Files

- **mobileops.yaml**: Main configuration file
  - App metadata (name, identifier, version)
  - Build settings (flavors, signing)
  - Deployment destinations (Firebase, Play Console)
  - Version management rules
  - Changelog settings
  - Notification configuration

- **local.properties**: Android SDK path (standard Android project file)
  - Used for SDK auto-detection
  - Not committed to git

- **.mobilectl/**: Local state directory
  - Backup files (`.bak`)
  - Changelog cache
  - Local configuration overrides

### Build Outputs

- **Android**: `app/build/outputs/apk/{flavor}/{type}/`
- **iOS**: `build/Release-iphoneos/`

### Generated Files

- **CI/CD Workflows**:
  - `.github/workflows/mobilectl-deploy.yml`
  - `.gitlab-ci.yml`

- **Documentation**:
  - `docs/SETUP.md`
  - `CHANGELOG.md`

## Build System

### Gradle Configuration

- **Kotlin Multiplatform**: Shared code between JVM targets
- **Compose Multiplatform**: UI framework support (desktop/web planned)
- **Version Catalogs**: `libs.versions.toml` for dependency management

### Build Process

1. **Compilation**: `gradlew compileKotlin`
2. **Testing**: `gradlew test`
3. **Assembly**: `gradlew bundle`
4. **Distribution**: Fat JAR with all dependencies

### Entry Points

- **CLI**: `cli/src/main/kotlin/com/mobilectl/main.kt`
- **Wrapper Scripts**:
  - `mobilectl.sh` (Unix/macOS)
  - `mobilectl.bat` (Windows)

## Testing Strategy

- **Unit Tests**: Business logic validation
- **Integration Tests**: End-to-end workflow testing
- **Manual Tests**: Firebase/Play Console upload validation

**Test Organization:**
- `cli/src/test/`: CLI command tests
- `shared/src/commonTest/`: Shared logic tests
- Platform-specific tests in `jvmTest/`

## Development Workflow

1. **Local Development**:
   ```bash
   gradlew compileKotlin    # Compile
   gradlew test             # Run tests
   gradlew bundle         # Build JAR
   ```

2. **CLI Testing**:
   ```bash
   ./mobilectl.sh setup     # Test setup wizard
   ./mobilectl.sh deploy    # Test deployment
   ```

3. **Release**:
   - Version bump
   - Changelog generation
   - Build distribution artifacts
   - Tag release in git

## Key Design Decisions

1. **Kotlin Multiplatform**: Enables code sharing while maintaining platform-specific implementations
2. **JGit over CLI**: Pure Java Git implementation removes Ruby/shell dependencies
3. **SOLID Principles**: Each component has a single, well-defined responsibility
4. **Type Safety**: Leverages Kotlin's type system for configuration validation
5. **Atomic Operations**: All file operations are atomic with automatic rollback
6. **Smart Detection**: Minimizes manual configuration through intelligent auto-detection
7. **Caching Strategy**: Multi-level caching (build cache, artifact cache) for performance
8. **Error Handling**: Clean error messages with actionable suggestions

## Extension Points

- **Deploy Strategies**: Implement `BaseUploadStrategy` for new destinations
- **Build Platforms**: Extend `PlatformBuilder` for new build systems
- **Config Formats**: Implement `ConfigParser` for TOML/JSON support
- **Notification Channels**: Add new notification providers
- **Version Strategies**: Custom version bump algorithms

## Performance Optimizations

- **Build Cache**: Source file hashing to skip unnecessary rebuilds
- **Artifact Cache**: In-memory path caching between build/deploy phases
- **Parallel Deployment**: Concurrent uploads to multiple destinations
- **Smart SDK Detection**: Caches Android SDK location across invocations
- **File Operation Batching**: Minimizes disk I/O with batch writes

## Security Considerations

- **Credential Management**: Environment variable support for sensitive data
- **Keystore Security**: Never logs or exposes keystore passwords
- **Service Accounts**: JWT-based authentication (no password storage)
- **Atomic Writes**: Prevents partial file corruption
- **Backup Strategy**: Automatic `.bak` files before modifications
- **Input Validation**: Comprehensive validation before file operations

---

**Last Updated**: November 2024
**Kotlin Version**: 1.9+
**Target Platform**: JVM 11+
