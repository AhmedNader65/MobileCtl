# Google Play Console Service Account

This guide explains how to create a Google Play Console service account for automated deployment with MobileCtl.

::: tip One Service Account for Firebase & Play Console
**Good news!** You can use the **same service account** for both Firebase App Distribution and Google Play Console. No need to create separate credentials - just set up proper permissions for both services.
:::

## Overview

A Google Play Console service account enables:
- Upload APK/AAB files programmatically
- Manage releases on Play Console
- Update app metadata
- Deploy to different release tracks (internal, alpha, beta, production)

## Prerequisites

Before you begin, make sure you have:
- **Google Play Console access** with Admin or Account Owner permissions
- **Google Cloud Platform access** (automatically available with Play Console)
- An app registered in Google Play Console (can be in draft state)

## Step 1: Access Google Play Console API Settings

### 1.1 Navigate to API Access

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app (or any app if setting up for all apps)
3. In the left sidebar, navigate to **Setup → API access**

::: tip First Time Setup
If this is your first time accessing the API section, you may need to accept the API Terms of Service.
:::

### 1.2 Link to Google Cloud Project

If you haven't linked to a Google Cloud project yet:

1. Click **Link to a Google Cloud project**
2. Choose an option:
   - **Create a new Google Cloud project** (recommended for new apps)
   - **Use an existing Google Cloud project** (if you have one, e.g., from Firebase)
3. Click **Link**

::: warning Use Existing Firebase Project
If you're already using Firebase for your app, **select the same Google Cloud project** that Firebase uses. This allows you to use one service account for both Firebase and Play Console!
:::

## Step 2: Create or Use Existing Service Account

### Option A: Use Existing Service Account (Recommended if you have Firebase)

If you already have a Firebase service account, you can use it for Play Console too!

1. In **Setup → API access**, scroll to **Service accounts**
2. You should see your existing service account listed
3. Skip to **Step 3: Grant Play Console Permissions** below

### Option B: Create New Service Account

1. In the **Service accounts** section, click **Create new service account**
2. You'll see instructions to create it in Google Cloud Console
3. Click **Google Cloud Platform** link (opens in new tab)

In the Google Cloud Console:

4. Click **+ CREATE SERVICE ACCOUNT** at the top
5. Fill in service account details:
   - **Name**: `mobilectl-deploy` (or `android-deployment`, `app-deployment`)
   - **ID**: Auto-generated (e.g., `mobilectl-deploy@your-project.iam.gserviceaccount.com`)
   - **Description**: `Service account for automated Android app deployment`

6. Click **CREATE AND CONTINUE**

7. **Grant roles** (important for Firebase compatibility):
   - If you want to use this for **both Play Console and Firebase**:
     - Add role: **Firebase App Distribution Admin**
     - Add role: **Firebase Admin SDK Administrator Service Agent**
   - If only for Play Console, you can skip this step

8. Click **CONTINUE** then **DONE**

## Step 3: Download Service Account Key

### 3.1 Generate JSON Key

Still in Google Cloud Console:

1. Find your service account in the list
2. Click on the service account email address
3. Go to the **KEYS** tab
4. Click **ADD KEY** → **Create new key**
5. Choose **JSON** format
6. Click **CREATE**

The JSON key file will download automatically.

::: danger Download Only Once
You can only download this key file once! Store it securely. If lost, you'll need to create a new key.
:::

### 3.2 Store the Key Securely

```bash
# Create credentials directory
mkdir -p credentials

# Rename for clarity (use a name that indicates it works for both!)
mv ~/Downloads/your-project-*.json credentials/google-service-account.json

# Set restrictive permissions
chmod 600 credentials/google-service-account.json
```

::: tip Naming Recommendation
If using for both Firebase and Play Console, name it something generic like:
- `google-service-account.json`
- `android-deployment-sa.json`
- `firebase-play-console-sa.json`

This makes it clear it's used for multiple Google services.
:::

## Step 4: Grant Play Console Permissions

Now grant this service account access to manage your apps in Play Console.

### 4.1 Invite Service Account in Play Console

1. Return to **Google Play Console → Setup → API access**
2. In the **Service accounts** section, find your service account
3. Click **Grant access** next to the service account name

::: tip Service Account Not Appearing?
If you just created the service account and it's not showing:
- Refresh the page
- Wait 1-2 minutes and try again
- Make sure you created it in the same Google Cloud project that's linked to Play Console
:::

### 4.2 Configure App Permissions

On the **App permissions** tab:

1. Select apps to grant access:
   - **All current and future apps** (recommended for automation)
   - Or select specific apps from the list

2. Click **Apply**

### 4.3 Configure Account Permissions

On the **Account permissions** tab, choose permissions:

**Option 1: Admin (Recommended for full automation)**
- Select **Admin (all permissions)**
- This grants all necessary permissions for release management

**Option 2: Custom Permissions (More restrictive)**
- ✅ **View app information and download bulk reports (read only)**
- ✅ **Release to production, exclude devices, and use app signing**
- ✅ **Release apps to testing tracks**
- ✅ **Manage testing track releases**
- ✅ **Manage production releases**
- ✅ **Manage app content**

3. Click **Invite user** (Note: despite the name, this works for service accounts)
4. Review the summary and click **Send invite** or **Apply**

::: warning Permission Propagation
It may take a few minutes for permissions to fully propagate. If deployment fails immediately after setup, wait 2-3 minutes and try again.
:::

## Step 5: Configure MobileCtl

### 5.1 For Play Console Only

```yaml
# mobileops.yaml
deploy:
  android:
    play_console:
      enabled: true
      service_account: credentials/google-service-account.json
      package_name: com.example.yourapp
      track: internal  # Options: internal, alpha, beta, production
```

### 5.2 For Both Firebase and Play Console (Recommended!)

```yaml
# mobileops.yaml
deploy:
  android:
    # Use same service account for both!
    firebase:
      enabled: true
      service_account: credentials/google-service-account.json
      test_groups:
        - qa-team
        - internal

    play_console:
      enabled: true
      service_account: credentials/google-service-account.json  # Same file!
      package_name: com.example.yourapp
      track: internal
```

::: tip One Credential, Multiple Destinations
As long as you granted the service account both Firebase and Play Console permissions, you can use the same JSON file for both deployment destinations. This simplifies credential management significantly!
:::

### 5.3 Test the Setup

```bash
# Build your app
mobilectl build android

# Test Play Console upload
mobilectl deploy --platform android --destination play-console

# If you configured Firebase too:
mobilectl deploy --platform android --destination firebase

# Deploy to both at once:
mobilectl deploy --platform android --destination play-console,firebase
```

## Track Options Explained

Google Play supports different release tracks:

| Track | Description | Review Time | Tester Limit |
|-------|-------------|-------------|--------------|
| **internal** | Internal testing, no review | Instant | 100 testers |
| **alpha** | Closed testing | Hours | Unlimited |
| **beta** | Open or closed testing | Hours | Unlimited |
| **production** | Public release | 1-7 days | Everyone |

::: tip Start with Internal
Use `internal` track for initial testing - it's instant and perfect for CI/CD pipelines.
:::

## Verification

### Check File Format

```bash
# Verify JSON is valid
cat credentials/google-service-account.json | jq .

# Should contain:
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...",
  "client_email": "mobilectl-deploy@your-project.iam.gserviceaccount.com",
  ...
}
```

### Test Upload

```bash
# Dry run (doesn't actually upload)
mobilectl deploy --platform android --destination play-console --dry-run

# Real upload
mobilectl deploy --platform android --destination play-console
```

Check Google Play Console → Release → Testing → Internal testing to see your build!

## Troubleshooting

### Error: "The current user has insufficient permissions"

**Cause**: Service account doesn't have required permissions in Play Console

**Solution**:
1. Go to Play Console → Setup → API access
2. Find your service account
3. Click "Manage permissions" or "Grant access"
4. Ensure it has at least:
   - View app information
   - Manage testing track releases
   - Release apps to testing tracks
5. Or simply grant "Admin" permissions
6. Wait 2-3 minutes for changes to propagate

### Error: "The resource could not be found" or "Package not found"

**Cause**: Package name mismatch or app not properly registered

**Solution**:
1. Verify `package_name` in `mobileops.yaml` matches exactly:
   - The package name in Google Play Console
   - Your app's `applicationId` in `build.gradle`
2. Ensure the app is created in Play Console (even if not published)
3. Check that you granted the service account access to this specific app

### Error: "Invalid JSON key" or "Could not load credentials"

**Cause**: JSON file corrupted or malformed

**Solution**:
1. Validate JSON: `cat credentials/google-service-account.json | jq .`
2. Check file permissions: `ls -l credentials/google-service-account.json` (should be `-rw-------`)
3. Verify the file isn't empty: `wc -l credentials/google-service-account.json`
4. If corrupted, re-download from Google Cloud Console

### Service Account Not Appearing in Play Console

**Cause**: Not created in linked Google Cloud project

**Solution**:
1. In Play Console → API access, check which GCP project is linked
2. Go to [Google Cloud Console](https://console.cloud.google.com)
3. Switch to the correct project (top left dropdown)
4. Navigate to IAM & Admin → Service Accounts
5. Verify your service account exists in THIS project
6. If not, create it in the correct project

### Error: "Permission denied" when deploying

**Cause**: Recent permission changes haven't propagated

**Solution**:
1. Wait 2-5 minutes after granting permissions
2. Verify permissions are correctly set in Play Console
3. Try re-downloading the JSON key
4. Check the service account hasn't been disabled

## Security Best Practices

### 1. Never Commit Credentials to Git

```bash
# .gitignore
credentials/
*.json
!package.json
!tsconfig.json
**/google-services.json
```

### 2. Use Environment-Specific Service Accounts

```yaml
# Production
deploy:
  android:
    play_console:
      service_account: credentials/google-sa-production.json

# Staging
deploy:
  android:
    play_console:
      service_account: credentials/google-sa-staging.json
```

### 3. Rotate Keys Regularly

```bash
# Every 90-180 days:
# 1. Create new key in GCP Console
# 2. Update mobilectl config
# 3. Test deployment
# 4. Delete old key in GCP Console
```

### 4. Monitor Service Account Activity

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Navigate to **IAM & Admin → Service Accounts**
3. Click your service account
4. Review activity logs
5. Monitor for unexpected usage patterns

### 5. Principle of Least Privilege

Only grant permissions actually needed:

```
✓ For internal testing: Release apps to testing tracks
✓ For production: Release to production + Manage production releases
✗ Don't grant Admin if you only need testing track access
```

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/deploy-android.yml
name: Deploy Android to Play Console

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Build Android App
        run: ./gradlew assembleRelease

      - name: Deploy to Play Console
        env:
          GOOGLE_SERVICE_ACCOUNT: ${{ secrets.GOOGLE_SERVICE_ACCOUNT_JSON }}
        run: |
          echo "$GOOGLE_SERVICE_ACCOUNT" > /tmp/google-sa.json
          mobilectl deploy --platform android --destination play-console
```

**Setup GitHub Secrets:**
1. Go to repository **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `GOOGLE_SERVICE_ACCOUNT_JSON`
4. Value: Paste the **entire contents** of your `google-service-account.json`
5. Click **Add secret**

### GitLab CI

```yaml
# .gitlab-ci.yml
deploy_android:
  stage: deploy
  image: openjdk:17
  script:
    - echo "$GOOGLE_SERVICE_ACCOUNT_JSON" > /tmp/google-sa.json
    - mobilectl deploy --platform android --destination play-console
  only:
    - tags
  variables:
    GIT_STRATEGY: clone
```

**Setup GitLab Variables:**
1. Go to **Settings → CI/CD → Variables**
2. Click **Add variable**
3. Key: `GOOGLE_SERVICE_ACCOUNT_JSON`
4. Value: Paste JSON file contents
5. Type: **File** (recommended) or **Variable**
6. ✅ Protect variable
7. ✅ Mask variable
8. Click **Add variable**

## Using Same Service Account for Multiple Apps

### Same Team, Multiple Apps

One service account can manage all your apps:

```yaml
# App 1 config
deploy:
  android:
    play_console:
      service_account: credentials/google-service-account.json
      package_name: com.example.app1

# App 2 config
deploy:
  android:
    play_console:
      service_account: credentials/google-service-account.json  # Same file!
      package_name: com.example.app2
```

Just make sure to grant the service account access to all apps in Play Console.

### Different Teams / Organizations

Use separate service accounts:

```yaml
# Personal projects
deploy:
  android:
    play_console:
      service_account: credentials/google-sa-personal.json

# Company projects
deploy:
  android:
    play_console:
      service_account: credentials/google-sa-company.json
```

## Related Documentation

- [Firebase Service Account](firebase-service-account) - Set up for Firebase App Distribution (can use same SA!)
- [Deploy Command](/reference/deploy) - Deployment options and strategies
- [Setup Wizard](/guide/setup-wizard) - Automated credential configuration
- [CI/CD Integration](/guide/ci-cd) - Complete automation examples
- [Configuration Reference](/reference/config-deploy) - All deployment config options

## Useful Links

- [Google Play Console](https://play.google.com/console)
- [Google Cloud Console](https://console.cloud.google.com)
- [Google Play Developer API](https://developers.google.com/android-publisher)
- [Service Account Documentation](https://cloud.google.com/iam/docs/service-accounts)
- [Play Console API Access](https://support.google.com/googleplay/android-developer/answer/6112435)

---

::: tip Quick Reference
**TL;DR for Play Console + Firebase:**

1. **Setup** → **API access** in Play Console
2. Link Google Cloud project (use same one as Firebase if applicable)
3. Create service account in GCP Console (or use existing Firebase one)
4. Add roles: **Firebase App Distribution Admin** (if using for both)
5. Download JSON key
6. Grant access in Play Console with Admin permissions
7. Use **same JSON file** for both Firebase and Play Console in `mobileops.yaml`
8. Test: `mobilectl deploy --platform android --destination play-console,firebase`

**One service account. Two destinations. Zero hassle.** ✨
:::
