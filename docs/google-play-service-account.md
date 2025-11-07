# Google Play Console Service Account

This guide explains how to create a Google Play Console service account for automated deployment with MobileCtl.

## Overview

A Google Play Console service account is required to:
- Upload APK/AAB files programmatically
- Manage releases on Play Console
- Update app metadata
- Deploy to different release tracks (internal, alpha, beta, production)

## Prerequisites

Before you begin, make sure you have:
- **Google Play Console access** with Admin or Account Owner permissions
- **Google Cloud Platform access** (automatically available with Play Console)
- A published app on Google Play Console (or at least one created)

## Step 1: Create Service Account

### 1.1 Open Google Cloud Platform

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. In the left menu, go to **Setup → API access**
4. Click **Choose a project to link** (if not already linked)

::: tip First Time Setup
If this is your first time using API access, you'll need to link your Play Console to a Google Cloud Project. Click **Create new project** or select an existing one.
:::

### 1.2 Create the Service Account

1. In the **API access** page, scroll down to **Service accounts**
2. Click **Create new service account**
3. Click **Google Cloud Platform** (this opens GCP Console in a new tab)

In Google Cloud Console:

4. Click **+ CREATE SERVICE ACCOUNT**
5. Fill in the details:
   - **Service account name**: `mobilectl-deploy` (or any name you prefer)
   - **Service account ID**: Auto-generated from name (e.g., `mobilectl-deploy@project-id.iam.gserviceaccount.com`)
   - **Service account description**: `Service account for automated app deployment with MobileCtl`

6. Click **CREATE AND CONTINUE**

### 1.3 Grant Permissions (Optional in GCP)

You can skip this step in GCP as you'll grant permissions in Play Console.

1. Click **CONTINUE** (skip role assignment)
2. Click **DONE**

## Step 2: Download JSON Key

### 2.1 Create Key

1. In the Service Accounts list, find your newly created service account
2. Click on the service account name
3. Go to the **KEYS** tab
4. Click **ADD KEY → Create new key**
5. Select **JSON** format
6. Click **CREATE**

The JSON key file will automatically download to your computer.

::: warning Keep It Secret!
This JSON file contains sensitive credentials. Never commit it to version control!
:::

### 2.2 Rename and Store Key

Rename the downloaded file for clarity:

```bash
# Original name (example)
project-name-abc123-1234567890ab.json

# Rename to
play-console-service-account.json
```

Move it to your credentials directory:

```bash
mkdir -p credentials
mv ~/Downloads/play-console-service-account.json credentials/
```

## Step 3: Grant Access in Play Console

Now you need to grant the service account access to your app in Play Console.

### 3.1 Return to Play Console

1. Go back to the **Play Console → Setup → API access** page
2. Refresh the page if needed
3. You should now see your service account in the **Service accounts** list

### 3.2 Grant Permissions

1. Click **Grant access** next to your service account
2. On the **App permissions** tab:
   - Select the apps you want to give access to
   - Or select **All applications** to grant access to all your apps

3. On the **Account permissions** tab:
   - **Recommended**: Select **Admin (all permissions)** for full access
   - **Or**: Select specific permissions:
     - ✅ **View app information and download bulk reports** (required)
     - ✅ **Manage production releases** (required for production)
     - ✅ **Manage testing track releases** (required for alpha/beta/internal)
     - ✅ **Create, edit, and delete draft apps** (optional)
     - ✅ **Release apps to testing tracks** (required)
     - ✅ **Release to production, exclude devices, and use app signing** (required for production)

::: tip Permission Levels
- **Admin (all permissions)**: Full access, simplest option
- **Release manager**: Can manage releases but not financial data
- **Custom**: Select only what you need for your workflow
:::

4. Click **Invite user** (the button may say **Apply** or **Save**)
5. Review the permissions
6. Click **Send invite** or **Apply**

The service account now has access to your app!

## Step 4: Configure MobileCtl

### 4.1 Update mobileops.yaml

Add the Play Console configuration to your `mobileops.yaml`:

```yaml
deploy:
  android:
    play_console:
      enabled: true
      service_account: credentials/play-console-service-account.json
      package_name: com.example.yourapp
      track: internal  # or: alpha, beta, production
```

### 4.2 Test Configuration

Verify the setup works:

```bash
# Build your app first
mobilectl build android

# Test upload to internal track
mobilectl deploy --platform android --destination play-console
```

::: tip Track Options
- **internal**: Internal testing (fastest approval, up to 100 testers)
- **alpha**: Alpha testing (closed testing)
- **beta**: Beta testing (open or closed testing)
- **production**: Production release (available to all users)
:::

## Step 5: Verify Setup

### 5.1 Check File Permissions

Ensure the JSON file has correct permissions:

```bash
# Check permissions
ls -l credentials/play-console-service-account.json

# Should show: -rw------- (600) - owner can read/write only
# If not, fix it:
chmod 600 credentials/play-console-service-account.json
```

### 5.2 Test JSON File

Verify the JSON file is valid:

```bash
# Check JSON format
cat credentials/play-console-service-account.json | jq .

# Should show formatted JSON with keys:
# - type: "service_account"
# - project_id: "your-project-id"
# - private_key_id: "..."
# - private_key: "-----BEGIN PRIVATE KEY-----..."
# - client_email: "mobilectl-deploy@..."
```

### 5.3 Test Deployment

Try a dry-run deployment:

```bash
mobilectl deploy --platform android --destination play-console --dry-run
```

## Troubleshooting

### Error: "The service account doesn't have the required permissions"

**Solution:**
1. Go back to Play Console → API access
2. Click on your service account
3. Verify permissions are granted
4. Ensure you selected the correct app
5. Wait a few minutes for permissions to propagate

### Error: "Project ID not found"

**Solution:**
1. Verify the JSON file downloaded correctly
2. Check that `project_id` exists in the JSON
3. Ensure the Play Console is linked to a GCP project
4. Re-download the JSON key if needed

### Error: "Invalid JSON file"

**Solution:**
1. Open the JSON file and verify it's valid JSON
2. Ensure the file wasn't corrupted during download
3. Check file encoding (should be UTF-8)
4. Re-download the key from GCP Console

### Error: "Authentication failed"

**Solution:**
1. Verify the service account email in JSON matches the one in Play Console
2. Check that the service account hasn't been deleted
3. Ensure the JSON key hasn't been revoked
4. Generate a new key if the old one is compromised

### Can't find "API access" in Play Console

**Solution:**
1. Ensure you have Admin or Account Owner permissions
2. Verify you have at least one app created (even if not published)
3. Try a different browser or clear cache
4. Contact your Play Console administrator

## Security Best Practices

### 1. Never Commit to Version Control

Add to `.gitignore`:

```bash
# .gitignore
credentials/
*.json
!package.json
!tsconfig.json
```

### 2. Restrict Permissions

Only grant permissions needed for your workflow:

```yaml
# Minimal permissions for deployment:
- View app information
- Manage testing track releases
- Release apps to testing tracks
```

### 3. Rotate Keys Regularly

Rotate service account keys periodically:

```bash
# In GCP Console:
# 1. Create new key
# 2. Update mobilectl config
# 3. Test new key
# 4. Delete old key
```

### 4. Use Separate Accounts

Use different service accounts for different environments:

```yaml
# Production
service_account: credentials/play-console-production.json

# Staging
service_account: credentials/play-console-staging.json
```

### 5. Monitor Usage

Check service account activity regularly:

1. Go to GCP Console → IAM & Admin → Service accounts
2. Click on your service account
3. View **Activity** logs
4. Monitor for suspicious activity

## CI/CD Integration

### GitHub Actions

Store the JSON as a secret:

```yaml
# .github/workflows/deploy.yml
- name: Deploy to Play Console
  env:
    PLAY_CONSOLE_JSON: ${{ secrets.PLAY_CONSOLE_SERVICE_ACCOUNT }}
  run: |
    echo "$PLAY_CONSOLE_JSON" > /tmp/play-console.json
    mobilectl deploy --platform android --destination play-console
```

Add secret to GitHub:
1. Go to repository **Settings → Secrets**
2. Click **New repository secret**
3. Name: `PLAY_CONSOLE_SERVICE_ACCOUNT`
4. Value: Paste entire JSON content
5. Click **Add secret**

### GitLab CI

```yaml
# .gitlab-ci.yml
deploy:
  script:
    - echo "$PLAY_CONSOLE_JSON" > /tmp/play-console.json
    - mobilectl deploy --platform android --destination play-console
  only:
    - tags
```

Add variable in GitLab:
1. Go to **Settings → CI/CD → Variables**
2. Click **Add variable**
3. Key: `PLAY_CONSOLE_JSON`
4. Value: Paste JSON content
5. Check **Protect variable** and **Mask variable**

## Multiple Apps

If you manage multiple apps, you can:

### Option 1: One Service Account for All Apps

Grant the service account access to multiple apps in Play Console.

```yaml
# Same service account for all apps
deploy:
  android:
    play_console:
      service_account: credentials/play-console-service-account.json
```

### Option 2: Separate Service Accounts

Create different service accounts for different apps:

```yaml
# App 1
deploy:
  android:
    play_console:
      service_account: credentials/play-console-app1.json
      package_name: com.example.app1

# App 2
deploy:
  android:
    play_console:
      service_account: credentials/play-console-app2.json
      package_name: com.example.app2
```

## Related Documentation

- [Deploy Command](/reference/deploy) - Using Play Console deployment
- [Setup Wizard](/guide/setup-wizard) - Configuring credentials automatically
- [CI/CD Integration](/guide/ci-cd) - Automating deployments
- [Firebase Service Account](firebase-service-account) - For Firebase App Distribution
- [Configuration Reference](/reference/config-deploy) - All deployment options

## Useful Links

- [Google Play Console](https://play.google.com/console)
- [Google Cloud Console](https://console.cloud.google.com)
- [Google Play Developer API](https://developers.google.com/android-publisher)
- [API Access Documentation](https://support.google.com/googleplay/android-developer/answer/6112435)

---

::: tip Quick Reference
**TL;DR:**
1. Play Console → API access → Create service account
2. Download JSON key from GCP
3. Grant permissions in Play Console
4. Add to `mobileops.yaml`: `service_account: credentials/play-console.json`
5. Test: `mobilectl deploy --platform android --destination play-console`
:::
