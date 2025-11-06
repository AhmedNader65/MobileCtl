# CI/CD Integration

Integrate MobileCtl with your CI/CD pipeline.

## Overview

MobileCtl is designed for automation in CI/CD environments.

## GitHub Actions

```yaml
name: Build and Deploy

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy
        env:
          MOBILECTL_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          mobilectl deploy --all-flavors --confirm
```

## GitLab CI

```yaml
deploy:
  stage: deploy
  script:
    - mobilectl deploy firebase --confirm
  only:
    - tags
```

## Best Practices

- Use `--confirm` to skip prompts
- Use `--verbose` for detailed logs
- Store credentials in CI secrets
- Use dry-run in PR checks

See [Examples](/examples/ci-cd) for more examples.
