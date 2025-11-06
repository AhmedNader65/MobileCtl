# CI/CD Examples

Integrate MobileCtl with CI/CD pipelines.

## GitHub Actions

```yaml
name: Deploy
on:
  push:
    tags: ['v*']

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy
        run: mobilectl deploy --confirm
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

## Jenkins

```groovy
stage('Deploy') {
    steps {
        sh 'mobilectl deploy --all-flavors --confirm'
    }
}
```

See [CI/CD Guide](/guide/ci-cd) for more.
