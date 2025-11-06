# Multi-Platform Example

Flutter/React Native app example.

## Configuration

```yaml
app:
  name: MultiPlatformApp
  identifier: com.example.multi
  version: 1.0.0

build:
  android:
    enabled: true
    flavors: [production, staging]
  ios:
    enabled: true
    scheme: Runner

deploy:
  android:
    firebase:
      enabled: true
  ios:
    testflight:
      enabled: true
```

## Usage

```bash
# Build all platforms
mobilectl build all

# Deploy all
mobilectl deploy --all-flavors
```

See [Examples Overview](/examples/) for more.
