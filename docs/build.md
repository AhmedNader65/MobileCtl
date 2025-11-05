# Build Command

## Overview

`mobilectl build` compiles your app for one or more platforms with a single command.

**Key Features:**
- 🏗️ Build Android, iOS, or both
- ⚙️ Automatic platform detection
- 🎯 Flavor and type support
- 📊 Build duration reporting
- 🔍 Verbose output for debugging
- 📋 Dry-run mode to preview builds

---

## Basic Usage

```

mobilectl build [PLATFORM] [FLAVOR] [TYPE] [OPTIONS]

```

### Build All Platforms (Auto-Detect)

```

mobilectl build

```

Automatically detects available platforms and builds them.

### Build Specific Platform

```


# Build Android only

mobilectl build android

# Build iOS only

mobilectl build ios

# Build both explicitly

mobilectl build all

```

### Build with Flavor

```


# Android with staging flavor

mobilectl build android staging

# iOS with production flavor

mobilectl build ios production

```

### Build with Type

```


# Android release build

mobilectl build android production release

# Android debug build

mobilectl build android staging debug

```

---

## Commands

### Basic Build

```

mobilectl build

```

**Output:**
```

🏗️  Building: auto-detect
🏗️  Starting build...

✅ ANDROID: Build succeeded
✅ IOS: Build succeeded

✅ All builds succeeded
📊 Total time: 45.23s

```

### Build Specific Platform

```

mobilectl build android

```

**Output:**
```

🏗️  Building: ANDROID
🏗️  Starting build...

✅ ANDROID: Build succeeded
Output: /path/to/app-release.apk
Duration: 32.45s

✅ ANDROID build succeeded
📊 Total time: 32.45s

```

---

## Options

### Verbose Mode

Show detailed build output and file paths.

```

mobilectl build android --verbose

# or

mobilectl build android -v

```

**Output:**
```

🏗️  Building: ANDROID
🔍 Verbose mode enabled
Flavor: production
Type: release
🏗️  Starting build...

✅ ANDROID: Build succeeded
Output: /path/to/app-release.apk
Duration: 32.45s

```

### Dry-Run Mode

Preview what would be built without actually building.

```

mobilectl build --dry-run

```

**Output:**
```

🏗️  Building: auto-detect
📋 DRY-RUN mode - nothing will actually be built

```

Useful for:
- Verifying build configuration
- Testing in CI/CD pipelines
- Debugging configuration issues

---

## Configuration

Configure builds in `mobileops.yml`:

```

build:
android:
enabled: true
project_path: "."
default_flavor: "production"
default_type: "release"
gradle_properties:
MY_PROP: "value"
ios:
enabled: true
project_path: "."
scheme: "MyApp"
configuration: "Release"
destination: "generic/platform=iOS"

```

**Android Options:**
- `enabled` - Enable/disable Android builds
- `project_path` - Path to Android project
- `default_flavor` - Default flavor to build
- `default_type` - Default type (debug/release)
- `gradle_properties` - Custom gradle properties

**iOS Options:**
- `enabled` - Enable/disable iOS builds
- `project_path` - Path to iOS project
- `scheme` - Xcode scheme name
- `configuration` - Build configuration (Debug/Release)
- `destination` - Build destination

---

## Examples

### Development Build

```


# Build for testing

mobilectl build android staging debug --verbose

```

### Release Build

```


# Production release

mobilectl build all production release

```

### CI/CD Integration

```

\#!/bin/bash

# In your CI/CD pipeline

# Preview

mobilectl build --dry-run

# Actually build

mobilectl build all production release --verbose

# Check result

if [ \$? -eq 0 ]; then
echo "Build succeeded!"

# Deploy...

else
echo "Build failed!"
exit 1
fi

```

### Multi-Platform Build

```


# Build all platforms with same config

mobilectl build all

```

---

## Troubleshooting

**Platform not detected:**
```


# Make sure Android/iOS files exist

mobilectl build android --verbose

```

**Build fails:**
```


# Get detailed output

mobilectl build android --verbose

# Check configuration

cat mobileops.yml

```

**Flavor/Type not applied:**
```


# Specify explicitly

mobilectl build android my-flavor release

# Or add to config

# mobileops.yml:

# build:

# android:

# default_flavor: "my-flavor"

# default_type: "release"

```
