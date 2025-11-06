# Advanced Examples

Complex scenarios and enterprise setups.

## Multi-Environment

```yaml
deploy:
  flavorGroups:
    production:
      flavors: [productionRelease]
    testing:
      flavors: [staging, development]
```

## Custom Workflows

```bash
# Release workflow
mobilectl deploy \
  --bump-version minor \
  --changelog \
  --all-flavors \
  --confirm
```

## Enterprise Setup

```yaml
notify:
  slack:
    enabled: true
  email:
    enabled: true
    recipients: [team@company.com]
  webhook:
    enabled: true
    url: https://api.company.com/deploys
```

See [Examples Overview](/examples/) for more.
