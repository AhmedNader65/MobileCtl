# iOS App Example

Complete example for native iOS app.

## Configuration

```yaml
app:
  name: iOSApp
  identifier: com.example.ios
  version: 1.0.0

build:
  ios:
    enabled: true
    scheme: MyApp
    configuration: Release
    codeSignIdentity: "iPhone Distribution"

deploy:
  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey.p8
      bundleId: com.example.ios
      teamId: ABC123
```

## Usage

```bash
# Build
mobilectl build ios

# Deploy to TestFlight
mobilectl deploy testflight
```

See [Examples Overview](/examples/) for more.
