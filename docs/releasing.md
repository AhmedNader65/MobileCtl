# Release Process

This document describes how to create and publish releases for mobilectl.

## Automated Release Process

We use GitHub Actions to automatically build and release mobilectl for all platforms when you push a version tag.

### Quick Release Steps

1. **Update the version** in your code (if needed)
2. **Commit your changes**
3. **Create and push a tag**
4. **GitHub Actions does the rest!**

### Detailed Steps

#### 1. Prepare the Release

```bash
# Make sure you're on the main/master branch
git checkout master
git pull origin master

# Update version in build files if needed
# (Update version in build.gradle.kts or version.properties)

# Update CHANGELOG.md
# Add a new section for this version

# Commit changes
git add .
git commit -m "chore: Prepare release v1.0.0"
git push origin master
```

#### 2. Create and Push Tag

```bash
# Create a new tag (use semantic versioning: v1.0.0, v2.1.3, etc.)
git tag -a v1.0.0 -m "Release v1.0.0"

# Push the tag to GitHub
git push origin v1.0.0
```

#### 3. GitHub Actions Takes Over

Once you push the tag, GitHub Actions will automatically:

1. ✅ Build mobilectl on Linux, macOS, and Windows
2. ✅ Create platform-specific packages:
   - `mobilectl-linux.tar.gz`
   - `mobilectl-macos.tar.gz`
   - `mobilectl-windows.zip`
3. ✅ Create a GitHub Release
4. ✅ Upload all artifacts to the release
5. ✅ Include installation instructions in the release notes

#### 4. Verify the Release

1. Go to https://github.com/AhmedNader65/MobileCtl/releases
2. Find your new release
3. Download and test one of the artifacts
4. Verify the installation works:
   ```bash
   # Test the installer
   curl -sSL https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.sh | bash
   mobilectl --version
   ```

## Release Checklist

Before creating a release, ensure:

- [ ] All tests pass (`./gradlew test`)
- [ ] Build succeeds (`./gradlew build`)
- [ ] CHANGELOG.md is updated
- [ ] Version number is correct
- [ ] Documentation is up to date
- [ ] No uncommitted changes

## Version Numbering

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (v1.0.0 → v2.0.0): Breaking changes
- **MINOR** (v1.0.0 → v1.1.0): New features, backward compatible
- **PATCH** (v1.0.0 → v1.0.1): Bug fixes, backward compatible

### Examples:

- `v1.0.0` - First stable release
- `v1.1.0` - Added new deployment destination
- `v1.1.1` - Fixed bug in version bumping
- `v2.0.0` - Changed configuration format (breaking change)

## Manual Release (If Needed)

If you need to create a release manually:

### 1. Build the artifacts

```bash
# Build the JAR
./gradlew :cli:jar

# The JAR will be at: cli/build/libs/mobilectl.jar
```

### 2. Package for each platform

**Linux/macOS:**
```bash
mkdir -p dist/mobilectl/bin
mkdir -p dist/mobilectl/lib

cp cli/build/libs/mobilectl.jar dist/mobilectl/lib/

# Create launcher script
cat > dist/mobilectl/bin/mobilectl << 'EOF'
#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MOBILECTL_HOME="$( cd "$SCRIPT_DIR/.." && pwd )"
exec java -jar "$MOBILECTL_HOME/lib/mobilectl.jar" "$@"
EOF

chmod +x dist/mobilectl/bin/mobilectl

# Package
tar -czf mobilectl-linux.tar.gz -C dist mobilectl/
```

**Windows:**
```powershell
# Create launcher script
@"
@echo off
set SCRIPT_DIR=%~dp0
set MOBILECTL_HOME=%SCRIPT_DIR%..
java -jar "%MOBILECTL_HOME%\lib\mobilectl.jar" %*
"@ | Out-File -FilePath dist/mobilectl/bin/mobilectl.bat -Encoding ASCII

# Package
Compress-Archive -Path dist/mobilectl/* -DestinationPath mobilectl-windows.zip
```

### 3. Create GitHub Release

1. Go to https://github.com/AhmedNader65/MobileCtl/releases/new
2. Choose the tag (or create a new one)
3. Fill in the release title and description
4. Upload the three artifacts:
   - mobilectl-linux.tar.gz
   - mobilectl-macos.tar.gz
   - mobilectl-windows.zip
5. Publish the release

## Testing Releases

### Test Installation Scripts

After creating a release, test the installation:

**Linux/macOS:**
```bash
curl -sSL https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.sh | bash
mobilectl --version
mobilectl --help
```

**Windows:**
```powershell
irm https://raw.githubusercontent.com/AhmedNader65/MobileCtl/main/install.ps1 | iex
mobilectl --version
mobilectl --help
```

### Test Basic Functionality

```bash
# Test setup
cd /path/to/test/project
mobilectl setup

# Test info
mobilectl info

# Test version
mobilectl version show

# Test help
mobilectl --help
```

## Troubleshooting

### Build Fails on GitHub Actions

1. Check the Actions tab: https://github.com/AhmedNader65/MobileCtl/actions
2. Look at the failed job logs
3. Common issues:
   - Gradle build failure: Check dependencies
   - Permission issues: Ensure gradlew is executable
   - Test failures: Fix failing tests before release

### Release Not Created

- Ensure you pushed a tag starting with `v` (e.g., `v1.0.0`)
- Check that the workflow file is in `.github/workflows/release.yml`
- Verify GitHub Actions is enabled for your repository

### Artifacts Not Uploaded

- Check the release job logs
- Ensure artifacts were built successfully in the build job
- Verify GITHUB_TOKEN has correct permissions

## Quick Reference

### Create a New Release

```bash
# 1. Update and commit
git add .
git commit -m "chore: Prepare release v1.0.0"
git push

# 2. Tag and push
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# 3. Wait for GitHub Actions to complete
# 4. Verify at https://github.com/AhmedNader65/MobileCtl/releases
```

### Delete a Tag (if needed)

```bash
# Delete local tag
git tag -d v1.0.0

# Delete remote tag
git push origin :refs/tags/v1.0.0
```

### Create a Prerelease

```bash
# Use a prerelease version format
git tag -a v1.0.0-beta.1 -m "Beta release v1.0.0-beta.1"
git push origin v1.0.0-beta.1

# Mark as prerelease in GitHub UI
```

## CI/CD Pipeline Overview

```
Tag Push (v*.*.*)
    ↓
GitHub Actions Triggered
    ↓
Build Job (Matrix: Linux, macOS, Windows)
    ├─ Checkout code
    ├─ Set up JDK 17
    ├─ Build JAR with Gradle
    ├─ Create launcher scripts
    ├─ Package artifacts
    └─ Upload artifacts
    ↓
Release Job
    ├─ Download all artifacts
    ├─ Create GitHub Release
    ├─ Upload artifacts to release
    └─ Publish release notes
    ↓
✅ Release Published
```

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Semantic Versioning](https://semver.org/)
- [GitHub Releases Documentation](https://docs.github.com/en/repositories/releasing-projects-on-github)
