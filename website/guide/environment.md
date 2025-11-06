# Environment Variables

Use environment variables for secure configuration.

## Overview

MobileCtl supports environment variable substitution in config files.

## Usage

```yaml
build:
  android:
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}

notify:
  slack:
    webhookUrl: ${SLACK_WEBHOOK_URL}
```

## Setting Variables

```bash
export MOBILECTL_KEY_PASSWORD="your_password"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/..."
```

## Best Practices

- Never commit secrets to git
- Use .env files (in .gitignore)
- Use CI/CD secrets for automation

See [Configuration Reference](/reference/configuration) for details.
