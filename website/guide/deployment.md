# Deployment

Deploy your apps to Firebase, TestFlight, and app stores.

## Overview

MobileCtl supports multiple deployment destinations for both Android and iOS.

## Basic Deployment

```bash
mobilectl deploy firebase
mobilectl deploy testflight
mobilectl deploy --interactive
```

## Supported Destinations

### Android
- Firebase App Distribution
- Google Play Console
- Local filesystem

### iOS
- TestFlight
- App Store
- Local filesystem

## Configuration

```yaml
deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase.json
  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey.p8
```

See [Deploy Command Reference](/reference/deploy) for full details.
