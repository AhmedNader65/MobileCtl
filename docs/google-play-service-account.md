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
- **Google Cloud Platform access** (free to create)
- An app registered in Google Play Console (can be in draft state)

## Step 1: Create Google Cloud Project

### 1.1 Navigate to Google Cloud Platform

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Sign in with your Google account

### 1.2 Create New Project (or Use Existing)

**Option A: Create New Project**
1. Click the project dropdown at the top
2. Click **New Project**
3. Enter project name (e.g., `my-app-deployment`)
4. Click **Create**

**Option B: Use Existing Firebase Project (Recommended)**

If you already have a Firebase project:
1. Click the project dropdown at the top
2. Select your existing Firebase project
3. This allows you to use one service account for both Firebase and Play Console!

::: tip Use Existing Firebase Project
If you're already using Firebase, **select the same Google Cloud project**. This way, you can use one service account for both Firebase App Distribution and Play Console deployment!
:::

## Step 2: Enable Google Play Android Developer API

::: danger Critical Step - Don't Skip!
You **must** enable the Google Play Android Developer API. Skipping this step will result in your JSON key being rejected because it won't have access to the Play Console API.
:::

1. With your project selected, go to **APIs & Services → Library**
   - Or visit: https://console.cloud.google.com/apis/library
2. In the search box, type: `Google Play Android Developer API`
3. Click on **Google Play Android Developer API**
4. Click **ENABLE**
5. Wait for it to enable (takes a few seconds)

You should see "API enabled" confirmation.

## Step 3: Create Service Account

### 3.1 Navigate to Credentials

1. Go to **APIs & Services → Credentials**
   - Or visit: https://console.cloud.google.com/apis/credentials
2. Click **Create Credentials** at the top
3. Select **Service account**

### 3.2 Create Service Account

On the **Create service account** page:

1. Fill in the details:
   - **Service account name**: `mobilectl-deploy` (or `android-deployment`)
   - **Service account ID**: Auto-generated (e.g., `mobilectl-deploy@your-project.iam.gserviceaccount.com`)
   - **Description**: `Service account for automated Android app deployment`

2. Click **CREATE AND CONTINUE**

### 3.3 Grant Editor Role

On the **Grant this service account access to project** step:

1. Click **Select a role** dropdown
2. Search for `Editor`
3. Select **Editor** role
4. Click **CONTINUE**

::: tip Why Editor Role?
The Editor role provides the necessary permissions for both Play Console API access and Firebase (if you're using it). This is the recommended role for deployment automation.
:::

5. Skip the optional step "Grant users access to this service account"
6. Click **DONE**

## Step 4: Download JSON Key

### 4.1 Find Your Service Account

1. You'll be returned to the Credentials page
2. Scroll down to **Service Accounts** section
3. Find the service account you just created

### 4.2 Manage Keys

1. Click on the **Actions** menu (three vertical dots) for your service account
2. Click **Manage keys**

### 4.3 Create and Download Key

1. Click **ADD KEY**
2. Select **Create new key**
3. Choose **JSON** format
4. Click **CREATE**

The JSON key file will automatically download to your computer.

::: danger Download Only Once!
You can only download this key file once! Store it securely. If lost, you'll need to create a new key.
:::

### 4.4 Store the Key Securely

```bash
# Create credentials directory
mkdir -p credentials

# Rename for clarity
# If using for BOTH Firebase and Play Console:
mv ~/Downloads/your-project-*.json credentials/google-service-account.json

# Or if using for Play Console only:
mv ~/Downloads/your-project-*.json credentials/play-console-service-account.json

# Set restrictive permissions
chmod 600 credentials/google-service-account.json
```

::: tip Naming Recommendation
If using for both Firebase and Play Console, name it generically:
- `google-service-account.json` (recommended)
- `android-deployment-sa.json`
- `firebase-play-console-sa.json`
:::

## Step 5: Grant Play Console Permissions

Now you need to invite this service account in Google Play Console.

### 5.1 Navigate to Users and Permissions

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app (or any app if setting up for all apps)
3. In the left sidebar, click **Users and permissions**
4. Click **Invite new users** button

::: info Alternative Path
Some accounts may have this at the account level:
- Click the settings gear icon
- Select **Users and permissions**
- Click **Invite new users**
:::

### 5.2 Add Service Account Email

1. In the **Email address** field, enter the service account email
   - Find this in the JSON file: look for `client_email`
   - Format: `mobilectl-deploy@your-project.iam.gserviceaccount.com`

2. Don't send a notification email (it's a service account, not a real person)

### 5.3 Configure Permissions

In the permissions section:

**App Permissions:**
- Select the apps this service account can access:
  - **All current and future apps** (recommended)
  - Or select specific apps

**Account Permissions:**

::: tip Recommended Permissions
Select these permissions for full deployment automation:
:::

**Required for Releases:**
- ✅ **View app information (read only)**
- ✅ **Manage production releases**
- ✅ **Manage testing track releases**

**Store Presence (Recommended):**
- ✅ **Edit store listing, pricing & distribution**
- ✅ **Manage store presence**

**App Access (Read-only):**
- ✅ **View app information (read only)**

**Financial Data (Optional):**
- Only if you need to access financial reports

::: warning Minimum Required Permissions
At minimum, you need:
- **View app information (read only)**
- **Manage testing track releases** (for internal/alpha/beta)
- **Manage production releases** (for production track)
:::

### 5.4 Invite User

1. Review the permissions
2. Click **Invite user** button
3. Click **Send invite** in the confirmation dialog

::: tip Permission Propagation
It may take a few minutes for permissions to fully propagate. If deployment fails immediately after setup, wait 2-3 minutes and try again.
:::

## Step 6: Configure MobileCtl

### 6.1 For Play Console Only

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

### 6.2 For Both Firebase and Play Console (Recommended!)

Use the same service account for both destinations:

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
If you created the service account in a Firebase project and granted it Editor role, you can use the same JSON file for both Firebase App Distribution and Play Console deployment!
:::

### 6.3 Test the Setup

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

Check **Google Play Console → Release → Testing → Internal testing** to see your build!

## Troubleshooting

### Error: "The current user has insufficient permissions"

**Cause**: Service account doesn't have required permissions in Play Console

**Solution**:
1. Go to Play Console → Users and permissions
2. Find your service account email
3. Click on it to edit permissions
4. Ensure it has at least:
   - View app information (read only)
   - Manage testing track releases
   - Manage production releases
5. Save changes
6. Wait 2-3 minutes for changes to propagate

### Error: "403 Forbidden" or "The API is not enabled"

**Cause**: Google Play Android Developer API is not enabled

**Solution**:
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Go to **APIs & Services → Library**
4. Search for `Google Play Android Developer API`
5. Click **ENABLE**
6. Wait a few minutes and try again

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

### Service Account Not Found in Play Console

**Cause**: Email address typo or service account not created

**Solution**:
1. Check the `client_email` in your JSON file
2. Copy it exactly (don't type it manually)
3. Ensure the service account exists in GCP Console
4. Verify you're in the correct Google Cloud project

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
✓ For internal testing: Manage testing track releases
✓ For production: Manage production releases
✗ Don't grant more permissions than necessary
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

## Adding Firebase to Existing Service Account

If you created this service account for Play Console and want to use it for Firebase too:

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Go to **IAM & Admin → IAM**
4. Find your service account
5. Click the edit icon (pencil)
6. Click **ADD ANOTHER ROLE**
7. Add: **Firebase App Distribution Admin**
8. Click **Save**
9. Now you can use the same JSON file for both!

## Related Documentation

- [Firebase Service Account](firebase-service-account) - Use same service account for Firebase!
- [Deploy Command](/reference/deploy) - Deployment options and strategies
- [Setup Wizard](/guide/setup-wizard) - Automated credential configuration
- [CI/CD Integration](/guide/ci-cd) - Complete automation examples
- [Configuration Reference](/reference/config-deploy) - All deployment config options

## Useful Links

- [Google Play Console](https://play.google.com/console)
- [Google Cloud Console](https://console.cloud.google.com)
- [Google Play Developer API](https://developers.google.com/android-publisher)
- [Google Play Android Developer API](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com)
- [Service Account Documentation](https://cloud.google.com/iam/docs/service-accounts)

---

::: tip Quick Reference
**TL;DR:**

1. **Create Google Cloud project** (or use existing Firebase project)
2. **Enable Google Play Android Developer API** ⚠️ (critical!)
3. Go to **Credentials → Create Credentials → Service account**
4. Name it and assign **Editor** role
5. **Download JSON key**
6. Go to **Play Console → Users and permissions → Invite new users**
7. Add service account email with appropriate permissions
8. Add to `mobileops.yaml`: `service_account: credentials/google-sa.json`
9. Test: `mobilectl deploy --platform android --destination play-console`

**For Firebase + Play Console (same service account):**
- Use existing Firebase project in step 1
- Same service account JSON works for both!
- Deploy to both: `mobilectl deploy --platform android --destination firebase,play-console`

**One service account. Two destinations. Zero hassle.** ✨
:::
