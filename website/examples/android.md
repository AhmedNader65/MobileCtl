# Android App Example

Complete example for native Android app.

## Configuration

```yaml
app:
  name: AndroidApp
  identifier: com.example.android
  version: 1.0.0

build:
  android:
    enabled: true
    flavors: [production, staging, development]
    defaultType: release
    keyStore: release.keystore
    keyAlias: android-key
    keyPassword: ${MOBILECTL_KEY_PASSWORD}

deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase.json
      testGroups: [qa-team]
    playConsole:
      enabled: true
      serviceAccount: credentials/play.json
      packageName: com.example.android
```

## Usage

```bash
# Build
mobilectl build android production release

# Deploy to Firebase
mobilectl deploy firebase

# Deploy to Play Console
mobilectl deploy play-console
```

See [Examples Overview](/examples/) for more.
