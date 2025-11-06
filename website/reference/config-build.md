# Build Configuration

Configure Android and iOS builds.

## Overview

```yaml
build:
  android:
    enabled: true
    flavors: [production, staging]
  ios:
    enabled: true
    scheme: Runner
```

## Android Options

```yaml
build:
  android:
    enabled: true
    defaultFlavor: production
    defaultType: release
    keyStore: release.keystore
    keyPassword: ${KEY_PASSWORD}
```

## iOS Options

```yaml
build:
  ios:
    enabled: true
    scheme: Runner
    configuration: Release
    codeSignIdentity: "iPhone Distribution"
```

See [Build Command](/reference/build) for details.
