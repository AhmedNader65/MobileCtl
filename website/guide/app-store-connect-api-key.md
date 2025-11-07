# App Store Connect API Key

This guide explains how to create an App Store Connect API key for automated iOS app deployment with MobileCtl.

## Overview

An App Store Connect API key is required to:
- Upload IPA files to TestFlight
- Submit apps to App Store review
- Manage app metadata programmatically
- Add testers and manage builds
- Automate iOS deployment

## Prerequisites

Before you begin, make sure you have:
- **Apple Developer Program** membership ($99/year)
- **App Store Connect** access
- **Account Holder** or **Admin** role (required to create API keys)
- An app created in App Store Connect

## Step 1: Access App Store Connect

### 1.1 Log In

1. Go to [App Store Connect](https://appstoreconnect.apple.com/)
2. Sign in with your Apple ID
3. Complete two-factor authentication if prompted

### 1.2 Verify Permissions

1. Click your name in the top right
2. Select **Users and Access**
3. Find your account
4. Verify you have **Admin** or **Account Holder** role

::: warning Permissions Required
Only **Account Holder** or **Admin** users can create API keys. If you don't have these permissions, ask your account administrator.
:::

## Step 2: Generate API Key

### 2.1 Navigate to API Keys

1. Click **Users and Access** in the top menu
2. Click the **Keys** tab
3. You'll see **App Store Connect API** section

### 2.2 Request Access (First Time Only)

If this is your first time accessing API keys:

1. Click **Request Access**
2. Review the terms and conditions
3. Click **Generate API Key**

### 2.3 Create New Key

1. Click the **+** button (or **Generate API Key**)
2. Fill in the details:
   - **Name**: `MobileCtl Deployment` (or any descriptive name)
   - **Access**: Select appropriate level:
     - **Admin**: Full access (recommended for automation)
     - **App Manager**: Can manage apps and submissions
     - **Developer**: Limited access
     - **Marketing**: Read-only for most features

3. Click **Generate**

::: tip Access Levels
- **Admin**: Full access to all features (recommended)
- **App Manager**: Can manage apps, TestFlight, and submissions
- **Developer**: Can't submit apps to review or manage testers
- Choose **Admin** or **App Manager** for MobileCtl
:::

## Step 3: Download API Key

### 3.1 Download the Key

After creating the key, you'll see key details:

1. **Issuer ID**: Your team identifier (e.g., `12345678-abcd-1234-abcd-123456789012`)
2. **Key ID**: The key identifier (e.g., `ABC123DEF`)
3. **Download API Key** button

::: danger Download Only Once!
You can only download the API key **once**. If you lose it, you must revoke and create a new one.
:::

4. Click **Download API Key**
5. Save the file (e.g., `AuthKey_ABC123DEF.p8`)

### 3.2 Copy Key Information

You'll need three pieces of information:

1. **Issuer ID**: Found at the top of the Keys page
2. **Key ID**: Shows next to your key name (e.g., `ABC123DEF`)
3. **Key File**: The downloaded `.p8` file

::: tip Save This Information
Write down or screenshot:
- Issuer ID: `12345678-abcd-1234-abcd-123456789012`
- Key ID: `ABC123DEF`
- Key File: `AuthKey_ABC123DEF.p8`

You'll need all three for MobileCtl configuration.
:::

## Step 4: Create JSON Configuration

MobileCtl expects a JSON file with your API key information.

### 4.1 Create JSON File

Create a file named `app-store-connect-api-key.json`:

```json
{
  "key_id": "ABC123DEF",
  "issuer_id": "12345678-abcd-1234-abcd-123456789012",
  "key": "-----BEGIN PRIVATE KEY-----\nYOUR_PRIVATE_KEY_HERE\n-----END PRIVATE KEY-----",
  "key_filepath": "AuthKey_ABC123DEF.p8"
}
```

### 4.2 Add Private Key Content

You need to add the content of the `.p8` file to the JSON:

**Option 1: Copy content directly**

```bash
# View the key content
cat AuthKey_ABC123DEF.p8

# Copy the entire output including:
# -----BEGIN PRIVATE KEY-----
# ... key content ...
# -----END PRIVATE KEY-----
```

Paste it into the `key` field with `\n` for line breaks:

```json
{
  "key_id": "ABC123DEF",
  "issuer_id": "12345678-abcd-1234-abcd-123456789012",
  "key": "-----BEGIN PRIVATE KEY-----\nMIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQg...\n-----END PRIVATE KEY-----"
}
```

**Option 2: Reference file path**

```json
{
  "key_id": "ABC123DEF",
  "issuer_id": "12345678-abcd-1234-abcd-123456789012",
  "key_filepath": "credentials/AuthKey_ABC123DEF.p8"
}
```

### 4.3 Store Files Securely

```bash
# Create credentials directory
mkdir -p credentials

# Move files
mv ~/Downloads/AuthKey_ABC123DEF.p8 credentials/
mv app-store-connect-api-key.json credentials/

# Set secure permissions
chmod 600 credentials/AuthKey_ABC123DEF.p8
chmod 600 credentials/app-store-connect-api-key.json
```

## Step 5: Find Your Team ID and Bundle ID

### 5.1 Get Team ID

1. Go to **App Store Connect**
2. Click your name → **View Membership**
3. Your **Team ID** is shown (10-character alphanumeric, e.g., `ABC123DEF`)

Or from Apple Developer:
1. Go to [Apple Developer](https://developer.apple.com/account)
2. Click **Membership**
3. **Team ID** is displayed

### 5.2 Get Bundle ID

1. In App Store Connect, select your app
2. Go to **App Information**
3. **Bundle ID** is shown (e.g., `com.example.myapp`)

Or:
1. In Xcode, open your project
2. Select your app target
3. Go to **General** tab
4. **Bundle Identifier** is shown

## Step 6: Configure MobileCtl

### 6.1 Update mobileops.yaml

Add the iOS deployment configuration:

```yaml
deploy:
  ios:
    testflight:
      enabled: true
      api_key_path: credentials/app-store-connect-api-key.json
      bundle_id: com.example.myapp
      team_id: ABC123DEF  # Your Team ID (NOT the numeric Issuer ID)

    app_store:
      enabled: false  # Enable when ready for App Store submission
      api_key_path: credentials/app-store-connect-api-key.json
      bundle_id: com.example.myapp
      team_id: ABC123DEF
```

::: warning Team ID vs Issuer ID
- **Team ID**: 10-character code (e.g., `ABC123DEF`) - use this in `mobileops.yaml`
- **Issuer ID**: UUID format (e.g., `12345678-abcd-...`) - use this in API key JSON
- They are different! Don't confuse them.
:::

### 6.2 Test Configuration

```bash
# Build your iOS app
mobilectl build ios

# Upload to TestFlight
mobilectl deploy --platform ios --destination testflight
```

## Step 7: Verify Setup

### 7.1 Check File Structure

```bash
credentials/
├── app-store-connect-api-key.json
└── AuthKey_ABC123DEF.p8
```

### 7.2 Verify JSON Format

```bash
# Check JSON is valid
cat credentials/app-store-connect-api-key.json | jq .

# Should show:
{
  "key_id": "ABC123DEF",
  "issuer_id": "12345678-abcd-...",
  "key_filepath": "credentials/AuthKey_ABC123DEF.p8"
}
```

### 7.3 Test Upload

```bash
# Dry run
mobilectl deploy --platform ios --destination testflight --dry-run

# Actual upload
mobilectl deploy --platform ios --destination testflight
```

Check App Store Connect → TestFlight to see your build!

## Troubleshooting

### Error: "Invalid API key"

**Solution:**
1. Verify Key ID matches the downloaded file
2. Check Issuer ID is correct (from Keys page)
3. Ensure the `.p8` file content is correctly formatted
4. Re-download the key if corrupted

### Error: "Not authorized"

**Solution:**
1. Verify the API key has **Admin** or **App Manager** access
2. Check that the key hasn't been revoked
3. Wait a few minutes for permissions to propagate
4. Ensure your Apple Developer Program membership is active

### Error: "Bundle ID mismatch"

**Solution:**
1. Verify Bundle ID in `mobileops.yaml` matches App Store Connect
2. Check Xcode project Bundle Identifier
3. Ensure the app is registered in App Store Connect
4. Verify provisioning profile matches Bundle ID

### Error: "Team ID not found"

**Solution:**
1. Use the 10-character **Team ID** (not Issuer ID)
2. Find it in App Store Connect → View Membership
3. Or Apple Developer → Membership
4. Format: `ABC123DEF` (10 chars, NOT UUID format)

### Error: "File not found" for .p8 file

**Solution:**
1. Check `key_filepath` path is correct
2. Use relative path from project root
3. Verify file exists: `ls -l credentials/AuthKey_*.p8`
4. Check file permissions: `chmod 600 credentials/AuthKey_*.p8`

### Can't create API key - "Request Access" button missing

**Solution:**
1. Ensure you have **Admin** or **Account Holder** role
2. Verify your Apple Developer Program membership is active
3. Check if your organization already has API access enabled
4. Contact your Account Holder to grant access

## Security Best Practices

### 1. Never Commit Keys to Version Control

Add to `.gitignore`:

```bash
# .gitignore
credentials/
*.p8
**/AuthKey_*.p8
```

### 2. Restrict Key Permissions

Use least privilege:

```
✓ App Manager: For TestFlight and app management
✗ Admin: Only if you need full access
✗ Developer: Can't upload to TestFlight
```

### 3. Rotate Keys Annually

```bash
# Every year or when compromised:
# 1. Revoke old key in App Store Connect
# 2. Generate new key
# 3. Update mobileops.yaml
# 4. Test deployment
# 5. Update CI/CD secrets
```

### 4. Secure Storage

```bash
# Set restrictive permissions
chmod 600 credentials/app-store-connect-api-key.json
chmod 600 credentials/AuthKey_*.p8

# Verify
ls -l credentials/
# Should show: -rw------- (owner read/write only)
```

### 5. Monitor Key Usage

Check activity regularly:
1. App Store Connect → Users and Access → Keys
2. View when keys were last used
3. Review for suspicious activity
4. Revoke unused keys

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy to TestFlight

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Xcode
        uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: 'latest-stable'

      - name: Build IPA
        run: mobilectl build ios

      - name: Deploy to TestFlight
        env:
          APP_STORE_CONNECT_KEY: ${{ secrets.APP_STORE_CONNECT_API_KEY }}
          APP_STORE_CONNECT_P8: ${{ secrets.APP_STORE_CONNECT_P8_KEY }}
        run: |
          echo "$APP_STORE_CONNECT_KEY" > /tmp/asc-api-key.json
          echo "$APP_STORE_CONNECT_P8" > /tmp/AuthKey.p8
          mobilectl deploy --platform ios --destination testflight -y
```

Add secrets in GitHub:
1. **Settings → Secrets → New repository secret**
2. `APP_STORE_CONNECT_API_KEY`: Paste JSON content
3. `APP_STORE_CONNECT_P8_KEY`: Paste `.p8` file content

### GitLab CI

```yaml
# .gitlab-ci.yml
deploy_testflight:
  stage: deploy
  tags:
    - macos
  script:
    - echo "$APP_STORE_CONNECT_KEY" > /tmp/asc-api-key.json
    - echo "$APP_STORE_CONNECT_P8" > /tmp/AuthKey.p8
    - mobilectl deploy --platform ios --destination testflight -y
  only:
    - tags
```

## Multiple Apps

### Same Team, Different Apps

Use one API key for all apps in your team:

```yaml
deploy:
  ios:
    # App 1
    app1:
      api_key_path: credentials/app-store-connect-api-key.json
      bundle_id: com.example.app1
      team_id: ABC123DEF

    # App 2
    app2:
      api_key_path: credentials/app-store-connect-api-key.json
      bundle_id: com.example.app2
      team_id: ABC123DEF
```

### Different Teams

Use separate API keys for different teams:

```yaml
deploy:
  ios:
    # Personal team
    personal:
      api_key_path: credentials/asc-personal.json
      team_id: ABC123DEF

    # Company team
    company:
      api_key_path: credentials/asc-company.json
      team_id: XYZ789GHI
```

## Related Documentation

- [Deploy Command](/reference/deploy) - Using TestFlight deployment
- [Setup Wizard](/guide/setup-wizard) - Automated credential configuration
- [Google Play Service Account](google-play-service-account) - For Android
- [Firebase Service Account](firebase-service-account) - For Firebase
- [CI/CD Integration](/guide/ci-cd) - Automating deployments
- [Configuration Reference](/reference/config-deploy) - All deployment options

## Useful Links

- [App Store Connect](https://appstoreconnect.apple.com/)
- [Apple Developer](https://developer.apple.com/)
- [App Store Connect API Docs](https://developer.apple.com/documentation/appstoreconnectapi)
- [Creating API Keys](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api)

---

::: tip Quick Reference
**TL;DR:**
1. App Store Connect → Users and Access → Keys → Generate API Key
2. Download `.p8` file (only chance!)
3. Note **Issuer ID**, **Key ID**, and **Team ID**
4. Create JSON with key info
5. Add to `mobileops.yaml`: `api_key_path: credentials/asc-api-key.json`
6. Test: `mobilectl deploy --platform ios --destination testflight`
:::
