# Build Automation

Automate your mobile app builds with MobileCtl.

## Overview

MobileCtl simplifies building Android and iOS apps with automatic detection, multiple flavors, and build types.

## Basic Build

```bash
mobilectl build android
mobilectl build ios
mobilectl build all
```

## Flavors and Types

```bash
mobilectl build android production release
mobilectl build android staging debug
```

## Configuration

```yaml
build:
  android:
    enabled: true
    flavors: [production, staging, development]
    defaultType: release
```

See [Build Command Reference](/reference/build) for full details.
