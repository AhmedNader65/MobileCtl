# Notification Configuration

Configure deployment notifications.

## Overview

```yaml
notify:
  slack:
    enabled: true
    webhookUrl: ${SLACK_WEBHOOK_URL}
  email:
    enabled: true
    recipients: [team@example.com]
```

## Slack

```yaml
notify:
  slack:
    enabled: true
    webhookUrl: ${SLACK_WEBHOOK_URL}
```

## Email

```yaml
notify:
  email:
    enabled: true
    recipients:
      - dev@example.com
      - qa@example.com
```

## Webhook

```yaml
notify:
  webhook:
    enabled: true
    url: https://api.example.com/webhook
```

See [Deploy Command](/reference/deploy) for details.
