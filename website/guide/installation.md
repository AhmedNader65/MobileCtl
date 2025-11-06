# Installation

Learn how to install MobileCtl on your system.

## System Requirements

### Minimum Requirements

- **Java**: JDK 11 or higher
- **Git**: 2.0 or higher
- **Operating System**: Linux, macOS, or Windows

### Platform-Specific Requirements

#### For Android Builds
- Android SDK
- Gradle 7.0 or higher
- Android Studio (recommended)

#### For iOS Builds
- macOS only
- Xcode 13 or higher
- CocoaPods (if using)

## Installation Methods

### Method 1: Build from Source (Recommended)

This is the current installation method while package managers are being set up.

#### Step 1: Clone the Repository

```bash
git clone https://github.com/AhmedNader65/MobileCtl.git
cd MobileCtl
```

#### Step 2: Build the Project

```bash
./gradlew build
```

This will:
- Download all dependencies
- Compile the Kotlin source code
- Run tests
- Create the CLI executable

#### Step 3: Test the Installation

```bash
./mobilectl.sh --help
```

You should see the MobileCtl help output.

#### Step 4: Add to PATH (Optional)

**On Linux/macOS:**

```bash
# Add to ~/.bashrc or ~/.zshrc
export PATH="$PATH:/path/to/MobileCtl"

# Or create a symlink
sudo ln -s /path/to/MobileCtl/mobilectl.sh /usr/local/bin/mobilectl
```

**On Windows:**

```bash
# Use mobilectl.bat
# Add the MobileCtl directory to your PATH environment variable
```

### Method 2: Homebrew (Coming Soon)

```bash
brew tap AhmedNader65/mobilectl
brew install mobilectl
```

### Method 3: Direct Binary Download (Coming Soon)

Download pre-built binaries from [GitHub Releases](https://github.com/AhmedNader65/MobileCtl/releases).

**Linux:**
```bash
curl -L https://github.com/AhmedNader65/MobileCtl/releases/latest/download/mobilectl-linux -o mobilectl
chmod +x mobilectl
sudo mv mobilectl /usr/local/bin/
```

**macOS:**
```bash
curl -L https://github.com/AhmedNader65/MobileCtl/releases/latest/download/mobilectl-macos -o mobilectl
chmod +x mobilectl
sudo mv mobilectl /usr/local/bin/
```

**Windows:**
```powershell
# Download mobilectl.exe from releases
# Add to PATH
```

### Method 4: NPM (Coming Soon)

```bash
npm install -g mobilectl
```

### Method 5: Docker (Coming Soon)

```bash
docker pull ahmednader65/mobilectl:latest

# Run with your project mounted
docker run -v $(pwd):/app ahmednader65/mobilectl build android
```

## Verify Installation

Check that MobileCtl is properly installed:

```bash
mobilectl --version
```

Expected output:
```
MobileCtl v0.2.0
Kotlin Multiplatform CLI for Mobile DevOps
```

Check available commands:

```bash
mobilectl --help
```

## Platform-Specific Setup

### Android Setup

#### 1. Install Android SDK

Download from [Android Studio](https://developer.android.com/studio) or use sdkmanager:

```bash
sdkmanager "platform-tools" "platforms;android-33"
```

#### 2. Set Environment Variables

```bash
export ANDROID_HOME=/path/to/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

#### 3. Create Keystore (for Release Builds)

```bash
keytool -genkey -v -keystore release.keystore -alias my-app-key \
  -keyalg RSA -keysize 2048 -validity 10000
```

Configure in `mobileops.yaml`:

```yaml
build:
  android:
    keyStore: release.keystore
    keyAlias: my-app-key
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}
```

### iOS Setup

#### 1. Install Xcode

Download from the [Mac App Store](https://apps.apple.com/us/app/xcode/id497799835).

#### 2. Install Command Line Tools

```bash
xcode-select --install
```

#### 3. Accept Xcode License

```bash
sudo xcodebuild -license accept
```

#### 4. Configure Code Signing

In `mobileops.yaml`:

```yaml
build:
  ios:
    scheme: MyApp
    codeSignIdentity: "iPhone Distribution"
    provisioningProfile: "path/to/profile.mobileprovision"
```

## Credential Setup

### Firebase Setup

#### 1. Download Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings > Service Accounts
4. Click "Generate New Private Key"
5. Save as `firebase-service-account.json`

#### 2. Configure in MobileCtl

```yaml
deploy:
  android:
    firebase:
      enabled: true
      serviceAccount: credentials/firebase-service-account.json
```

### TestFlight Setup

#### 1. Create App Store Connect API Key

1. Go to [App Store Connect](https://appstoreconnect.apple.com/)
2. Users and Access > Keys
3. Create new API key
4. Download the `.p8` file

#### 2. Configure in MobileCtl

```yaml
deploy:
  ios:
    testflight:
      enabled: true
      apiKeyPath: credentials/AuthKey_XXXXXXX.p8
      bundleId: com.example.myapp
      teamId: XXXXXXXXXX
```

### Google Play Console Setup

#### 1. Create Service Account

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create Service Account
3. Download JSON key

#### 2. Configure in MobileCtl

```yaml
deploy:
  android:
    playConsole:
      enabled: true
      serviceAccount: credentials/play-console-key.json
      packageName: com.example.myapp
```

## Environment Variables

Store sensitive credentials as environment variables:

```bash
# .env or in your shell profile
export MOBILECTL_KEY_PASSWORD="your_keystore_password"
export MOBILECTL_STORE_PASSWORD="your_store_password"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
```

Then reference in config:

```yaml
build:
  android:
    keyPassword: ${MOBILECTL_KEY_PASSWORD}
    storePassword: ${MOBILECTL_STORE_PASSWORD}

notify:
  slack:
    webhookUrl: ${SLACK_WEBHOOK_URL}
```

## Updating MobileCtl

### Update from Source

```bash
cd MobileCtl
git pull origin main
./gradlew build
```

### Update from Homebrew (Coming Soon)

```bash
brew upgrade mobilectl
```

### Update from NPM (Coming Soon)

```bash
npm update -g mobilectl
```

## Uninstallation

### Remove Binary

```bash
# If installed via symlink
sudo rm /usr/local/bin/mobilectl

# If cloned
rm -rf /path/to/MobileCtl
```

### Remove Homebrew (Coming Soon)

```bash
brew uninstall mobilectl
```

### Remove NPM (Coming Soon)

```bash
npm uninstall -g mobilectl
```

## Troubleshooting

### Java Version Issues

**Error**: "Unsupported class file major version"

**Solution**: Upgrade to JDK 11 or higher:

```bash
# macOS with Homebrew
brew install openjdk@17

# Linux with apt
sudo apt install openjdk-17-jdk

# Verify
java -version
```

### Permission Denied

**Error**: "Permission denied" when running `./mobilectl.sh`

**Solution**: Make the script executable:

```bash
chmod +x mobilectl.sh
```

### Build Failures

**Error**: Gradle build fails

**Solution**: Clean and rebuild:

```bash
./gradlew clean build
```

### Android SDK Not Found

**Error**: "ANDROID_HOME not set"

**Solution**: Set the environment variable:

```bash
export ANDROID_HOME=/path/to/Android/Sdk
```

## Next Steps

Now that MobileCtl is installed:

1. [Create your first config file](/guide/getting-started#create-configuration-file)
2. [Run your first build](/guide/getting-started#your-first-build)
3. [Learn about configuration](/guide/configuration)
4. [Explore command reference](/reference/commands)

Need help? Check out:
- [Getting Started Guide](/guide/getting-started)
- [GitHub Issues](https://github.com/AhmedNader65/MobileCtl/issues)
- [Examples](/examples/)
