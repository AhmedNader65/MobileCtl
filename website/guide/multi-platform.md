# Multi-Platform Builds

Build for Android and iOS simultaneously.

## Overview

MobileCtl makes it easy to build for multiple platforms with a unified workflow.

## Build All Platforms

```bash
mobilectl build all
```

## Configuration

```yaml
build:
  android:
    enabled: true
    defaultType: release
  ios:
    enabled: true
    scheme: Runner
```

## Platform Detection

MobileCtl automatically detects:
- Flutter projects
- React Native projects
- Native Android/iOS

See [Build Command Reference](/reference/build) for details.
