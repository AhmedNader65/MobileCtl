# Configuration

Learn how to configure MobileCtl for your project.

## Configuration File

MobileCtl uses `mobileops.yaml` for configuration. This file should be in your project root.

## Basic Configuration

```yaml
app:
  name: MyApp
  identifier: com.example.myapp
  version: 1.0.0

build:
  android:
    enabled: true
  ios:
    enabled: true
```

## Configuration Sections

- **app**: App metadata
- **build**: Build configuration
- **version**: Version management
- **changelog**: Changelog generation
- **deploy**: Deployment settings
- **notify**: Notifications

See [Configuration Reference](/reference/configuration) for complete details.

## Environment Variables

Use `${VAR_NAME}` to reference environment variables:

```yaml
build:
  android:
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
```

## Next Steps

- [Full Configuration Reference](/reference/configuration)
- [Build Configuration](/reference/config-build)
- [Deploy Configuration](/reference/config-deploy)
