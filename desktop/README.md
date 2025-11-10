# MobileCtl Desktop GUI

A modern desktop application for MobileCtl built with Compose Multiplatform Desktop.

## Features

### Main Dashboard
- **Quick Deploy Form**: Select platform, flavor, and track to start a deployment
- **Live Stats Cards**: View total deploys, success rate, and average deployment time
- **Recent Deployments**: Browse deployment history with timestamps and status

### Deploy Progress Screen
- **Real-time Progress**: Visual progress bars for each deployment step
- **Live Logs**: Color-coded log output with auto-scrolling
- **Cancel Support**: Abort deployments in progress
- **Smooth Animations**: Animated progress indicators with Material 3 design

### Configuration Editor
- **Firebase Settings**: Configure service accounts, release notes, and test groups
- **Play Console Settings**: Set up package name, track, and service account
- **Signing Configuration**: Manage keystore paths and credentials
- **File Pickers**: Native file dialogs for selecting JSON and JKS files
- **Validation**: Inline validation with helpful error messages

### UI Features
- **Dark/Light Themes**: Toggle between dark and light mode
- **Native Menu Bar**: File, View, and Help menus with keyboard shortcuts
- **Keyboard Navigation**: Full keyboard support with shortcuts
  - `Cmd/Ctrl + D`: Quick deploy
  - `Cmd/Ctrl + ,`: Open settings
  - `Cmd/Ctrl + Q`: Quit application
- **Responsive Design**: Adapts to different window sizes
- **Material 3 Design**: Modern, clean interface with smooth animations

## Building the Desktop App

### Prerequisites
- JDK 17 or higher
- Gradle 8.0 or higher

### Build Commands

```bash
# Run the desktop app
./gradlew :desktop:run

# Build distributable packages
./gradlew :desktop:packageDmg      # macOS
./gradlew :desktop:packageMsi      # Windows
./gradlew :desktop:packageDeb      # Linux

# Build all packages
./gradlew :desktop:packageDistributionForCurrentOS
```

### Development

```bash
# Run with hot reload (if configured)
./gradlew :desktop:runDistributable

# Run tests
./gradlew :desktop:test

# Clean build
./gradlew :desktop:clean :desktop:build
```

## Architecture

### Code Reuse
The desktop app reuses all business logic from `shared/commonMain`:
- `DeployOrchestrator` for deployment logic
- `ConfigLoader` for configuration management
- `VersionOrchestrator` for version bumping
- `ChangelogOrchestrator` for changelog generation
- All model classes (DeployConfig, DeployResult, etc.)

### UI Structure
```
desktop/
├── src/desktopMain/kotlin/com/mobilectl/desktop/
│   ├── Main.kt                    # Entry point with window setup
│   ├── ui/
│   │   ├── App.kt                 # Main app with navigation
│   │   ├── screens/
│   │   │   ├── DashboardScreen.kt # Main dashboard
│   │   │   ├── DeployProgressScreen.kt
│   │   │   └── ConfigScreen.kt
│   │   └── theme/
│   │       ├── Theme.kt           # Material 3 theme
│   │       └── Typography.kt      # Typography definitions
│   └── viewmodel/
│       ├── DashboardViewModel.kt  # Dashboard state management
│       ├── DeployProgressViewModel.kt
│       └── ConfigViewModel.kt
└── build.gradle.kts               # Desktop module config
```

## Distribution

The packaged apps include:
- Bundled JRE (no Java installation required)
- Native installers for each platform
- Auto-updates support (if configured)
- Platform-specific icons and metadata

### Package Sizes
- macOS DMG: ~100-150 MB
- Windows MSI: ~100-150 MB
- Linux DEB: ~100-150 MB

## Configuration

The desktop app reads the standard `mobileops.yaml` configuration file from:
1. Current working directory
2. `.mobilectl/mobileops.yaml`

## Known Issues

- File pickers use AWT FileDialog (not native Compose)
- Some animations may stutter on slower machines
- Window state persistence not yet implemented

## Future Enhancements

- [ ] Persistent window size/position
- [ ] Deployment history persistence
- [ ] Real-time deployment streaming from CLI
- [ ] Build artifact preview
- [ ] Changelog editor
- [ ] Multi-deployment support
- [ ] Custom themes
- [ ] Plugin system

## Contributing

The desktop GUI is part of the MobileCtl project. See the main README for contribution guidelines.

## License

Same as MobileCtl project license.
