# Firebase App Distribution Service Account

This guide explains how to create a Firebase service account for automated app distribution with MobileCtl.

::: tip One Service Account for Firebase & Play Console
**Good news!** You can use the **same service account** for both Firebase App Distribution and Google Play Console. No need to create separate credentials - just set up proper permissions for both services.

See also: [Google Play Service Account](google-play-service-account)
:::

## Overview

A Firebase service account enables:
- Upload APK/AAB files to Firebase App Distribution
- Distribute builds to testers
- Manage release notes
- Add testers to groups programmatically
- **Bonus:** Can also be used for Google Play Console deployment!

## Prerequisites

Before you begin, make sure you have:
- **Firebase project** created
- **Firebase App Distribution** enabled
- **Firebase Console access** with Owner or Editor permissions
- Your app registered in Firebase

## Step 1: Enable Firebase App Distribution

### 1.1 Create Firebase Project (if needed)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **Add project** (or select existing project)
3. Enter project name and follow the setup wizard
4. Click **Create project**

### 1.2 Add Your App

1. In Firebase Console, select your project
2. Click the **Android icon** (or iOS) to add your app
3. Enter your **Android package name** (e.g., `com.example.myapp`)
4. Enter **App nickname** (optional)
5. Click **Register app**
6. Download `google-services.json`
7. Click **Continue** and **Finish**

### 1.3 Enable App Distribution

1. In the left menu, click **Release & Monitor → App Distribution**
2. Click **Get started** (if first time)
3. Accept the terms of service

## Step 2: Create Service Account

### 2.1 Open IAM Settings

1. In Firebase Console, click the **gear icon** (⚙️) next to Project Overview
2. Click **Project settings**
3. Go to the **Service accounts** tab
4. Click **Manage service account permissions** (opens Google Cloud Console)

### 2.2 Create Service Account in GCP

In Google Cloud Console:

1. Click **+ CREATE SERVICE ACCOUNT**
2. Fill in the details:
   - **Service account name**: `firebase-mobilectl` (or any name)
   - **Service account ID**: Auto-generated (e.g., `firebase-mobilectl@your-project.iam.gserviceaccount.com`)
   - **Service account description**: `Service account for Firebase App Distribution with MobileCtl`

3. Click **CREATE AND CONTINUE**

### 2.3 Assign Roles

On the **Grant this service account access to project** step:

1. Click **Select a role**
2. Search for and add the required role:
   - **Firebase App Distribution Admin** (required for Firebase)

3. **Optional but recommended:** If you want to use this service account for **Google Play Console** too:
   - You don't need additional GCP roles
   - Just grant Play Console permissions later (see note below)

4. Click **CONTINUE**
5. Click **DONE**

::: tip Role Options
**For Firebase only:**
- **Firebase App Distribution Admin**: Can upload builds and manage testers (minimum required)
- **Firebase Admin**: Full Firebase access (includes all features)

**For Firebase + Play Console (same service account):**
- **Firebase App Distribution Admin**: Required for Firebase
- No additional GCP roles needed - Play Console permissions are granted separately in Play Console itself
:::

::: info Using for Both Firebase and Play Console?
If you plan to use this service account for both Firebase and Play Console:
1. Create it here with **Firebase App Distribution Admin** role
2. Download the JSON key (next step)
3. Then follow [Google Play Service Account guide](google-play-service-account#step-4-grant-play-console-permissions) Step 4 to grant Play Console permissions
4. Use the **same JSON file** for both destinations in your config!
:::

## Step 3: Generate JSON Key

### 3.1 Create Key

1. In the Service Accounts list, find your newly created service account
2. Click on the service account email
3. Go to the **KEYS** tab
4. Click **ADD KEY → Create new key**
5. Select **JSON** format
6. Click **CREATE**

The JSON key file will automatically download.

::: warning Keep It Secret!
This JSON file grants admin access to your Firebase project. Never commit it to version control!
:::

### 3.2 Rename and Store Key

Rename the file for clarity:

```bash
# Move to credentials directory
mkdir -p credentials

# Rename downloaded file
# If using for BOTH Firebase and Play Console, use a generic name:
mv ~/Downloads/your-project-abc123-1234567890ab.json credentials/google-service-account.json

# Or if using for Firebase only:
mv ~/Downloads/your-project-abc123-1234567890ab.json credentials/firebase-service-account.json

# Set restrictive permissions
chmod 600 credentials/google-service-account.json
```

::: tip Naming Recommendation
If using for both Firebase and Play Console, name it generically:
- `google-service-account.json` (recommended)
- `android-deployment-sa.json`
- `firebase-play-console-sa.json`

This makes it clear it works for multiple Google services.
:::

### 3.3 Also Download google-services.json

You'll also need your app's `google-services.json`:

1. In Firebase Console, go to **Project settings**
2. Scroll down to **Your apps**
3. Find your Android app
4. Click the **google-services.json** download button
5. Save it to your app directory:

```bash
# Android project
cp ~/Downloads/google-services.json app/
```

## Step 4: Set Up Test Groups

Create tester groups for organized distribution:

### 4.1 Create Groups

1. In Firebase Console, go to **App Distribution**
2. Click the **Testers & Groups** tab
3. Click **Add group**
4. Enter group name: `qa-team`, `beta-testers`, `internal`, etc.
5. Click **Create group**

### 4.2 Add Testers

1. Click on a group name
2. Click **Add testers**
3. Enter email addresses (one per line or comma-separated)
4. Click **Add testers**

Common groups:
- `qa-team` - Internal QA testers
- `beta-testers` - External beta testers
- `internal` - Company employees
- `partners` - Partner organizations

## Step 5: Configure MobileCtl

### 5.1 Firebase Only Configuration

```yaml
# mobileops.yaml
deploy:
  android:
    firebase:
      enabled: true
      service_account: credentials/firebase-service-account.json
      google_services: app/google-services.json
      release_notes: "Automated build from MobileCtl"
      test_groups:
        - qa-team
        - beta-testers
```

### 5.2 Firebase + Play Console (Recommended!)

Use the same service account for both destinations:

```yaml
# mobileops.yaml
deploy:
  android:
    firebase:
      enabled: true
      service_account: credentials/google-service-account.json  # Same file!
      google_services: app/google-services.json
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
If you granted both Firebase and Play Console permissions to your service account, you can use the same JSON file for both deployment destinations. This simplifies credential management!

To add Play Console permissions: Follow [Step 4 of the Play Console guide](google-play-service-account#step-4-grant-play-console-permissions)
:::

### 5.3 Test Configuration

```bash
# Build your app
mobilectl build android

# Deploy to Firebase only
mobilectl deploy --platform android --destination firebase

# Or deploy to both Firebase and Play Console at once:
mobilectl deploy --platform android --destination firebase,play-console
```

## Step 6: Verify Setup

### 6.1 Check File Permissions

```bash
# Check permissions
ls -l credentials/firebase-service-account.json

# Should be: -rw------- (600)
chmod 600 credentials/firebase-service-account.json
```

### 6.2 Validate JSON

```bash
# Verify JSON is valid
cat credentials/firebase-service-account.json | jq .

# Should contain:
# - type: "service_account"
# - project_id: "your-project-id"
# - private_key: "-----BEGIN PRIVATE KEY-----..."
# - client_email: "firebase-mobilectl@..."
```

### 6.3 Test Upload

```bash
# Dry run
mobilectl deploy --platform android --destination firebase --dry-run

# Actual upload
mobilectl deploy --platform android --destination firebase
```

Check Firebase Console → App Distribution → Releases to see your build!

## Troubleshooting

### Error: "Permission denied" or "403 Forbidden"

**Solution:**
1. Verify service account has **Firebase App Distribution Admin** role
2. Check that the role was applied correctly in GCP Console
3. Wait a few minutes for permissions to propagate
4. Re-download JSON key if needed

### Error: "App not found in Firebase project"

**Solution:**
1. Verify your app is registered in Firebase Console
2. Check that package name matches in:
   - `mobileops.yaml`
   - `google-services.json`
   - Firebase Console app registration
3. Ensure App Distribution is enabled

### Error: "Invalid google-services.json"

**Solution:**
1. Re-download `google-services.json` from Firebase Console
2. Verify it's placed in correct location (usually `app/`)
3. Check JSON is valid: `cat app/google-services.json | jq .`
4. Ensure it matches your app's package name

### Error: "Test group not found"

**Solution:**
1. Create the test group in Firebase Console → App Distribution
2. Verify group name spelling matches exactly
3. Check that group has at least one tester
4. Use comma-separated list in YAML: `test_groups: [group1, group2]`

### Error: "Service account key expired"

**Solution:**
1. Service account keys don't expire by default
2. Check if key was manually revoked in GCP Console
3. Generate a new key
4. Update `mobileops.yaml` with new key path

## Advanced Configuration

### Custom Release Notes

Use dynamic release notes:

```yaml
deploy:
  android:
    firebase:
      release_notes: "Build ${version} - ${date}"
      # Or use file:
      release_notes_file: RELEASE_NOTES.txt
```

### Multiple Test Groups

Deploy to different groups:

```yaml
deploy:
  android:
    firebase:
      test_groups:
        - qa-team        # Internal QA
        - beta-testers   # External beta
        - partners       # Partner companies
        - executives     # Company leadership
```

### Conditional Deployment

Use flavor-specific Firebase apps:

```yaml
deploy:
  android:
    firebase:
      # Free flavor to one Firebase app
      free:
        service_account: credentials/firebase-free.json
        google_services: app/src/free/google-services.json

      # Paid flavor to different Firebase app
      paid:
        service_account: credentials/firebase-paid.json
        google_services: app/src/paid/google-services.json
```

## Security Best Practices

### 1. Never Commit Credentials

Add to `.gitignore`:

```bash
# .gitignore
credentials/
**/google-services.json
*.json
!package.json
!tsconfig.json
```

### 2. Restrict Service Account Permissions

Use principle of least privilege:

```
✓ Firebase App Distribution Admin (minimum required)
✗ Don't use "Owner" role (too broad)
✗ Don't use "Editor" role (unnecessary)
```

### 3. Rotate Keys Periodically

```bash
# Every 90 days:
# 1. Create new key in GCP Console
# 2. Update mobileops.yaml
# 3. Test deployment
# 4. Delete old key
```

### 4. Use Environment Variables in CI/CD

Don't store JSON in repo:

```yaml
# .github/workflows/deploy.yml
- name: Deploy to Firebase
  env:
    FIREBASE_SA: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
  run: |
    echo "$FIREBASE_SA" > /tmp/firebase-sa.json
    mobilectl deploy --platform android --destination firebase
```

### 5. Monitor Service Account Activity

Check usage regularly:
1. GCP Console → IAM & Admin → Service Accounts
2. Click service account
3. View activity logs
4. Review for suspicious activity

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy to Firebase

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

      - name: Build APK
        run: ./gradlew assembleRelease

      - name: Deploy to Firebase
        env:
          FIREBASE_SERVICE_ACCOUNT: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
          GOOGLE_SERVICES: ${{ secrets.GOOGLE_SERVICES_JSON }}
        run: |
          echo "$FIREBASE_SERVICE_ACCOUNT" > /tmp/firebase-sa.json
          echo "$GOOGLE_SERVICES" > app/google-services.json
          mobilectl deploy --platform android --destination firebase -y
```

Add secrets in GitHub:
1. **Settings → Secrets → New repository secret**
2. Name: `FIREBASE_SERVICE_ACCOUNT`
3. Value: Paste entire JSON content
4. Repeat for `GOOGLE_SERVICES_JSON`

### GitLab CI

```yaml
# .gitlab-ci.yml
deploy_firebase:
  stage: deploy
  image: openjdk:17
  script:
    - echo "$FIREBASE_SERVICE_ACCOUNT" > /tmp/firebase-sa.json
    - echo "$GOOGLE_SERVICES_JSON" > app/google-services.json
    - mobilectl deploy --platform android --destination firebase -y
  only:
    - tags
```

Add variables in GitLab:
- **Settings → CI/CD → Variables**
- Add `FIREBASE_SERVICE_ACCOUNT` and `GOOGLE_SERVICES_JSON`
- Check **Protect variable** and **Mask variable**

## Multiple Apps / Flavors

### Separate Firebase Projects

```yaml
deploy:
  android:
    firebase:
      # Production app
      production:
        service_account: credentials/firebase-prod.json
        google_services: app/src/production/google-services.json
        test_groups: [internal, executives]

      # Staging app
      staging:
        service_account: credentials/firebase-staging.json
        google_services: app/src/staging/google-services.json
        test_groups: [qa-team, developers]
```

### Same Project, Different Apps

```yaml
deploy:
  android:
    firebase:
      # Android app
      android:
        service_account: credentials/firebase-sa.json
        google_services: app/google-services.json

      # Wear OS app (separate Firebase app)
      wear:
        service_account: credentials/firebase-sa.json
        google_services: wear/google-services.json
```

## Related Documentation

- [Google Play Service Account](google-play-service-account) - Use same service account for Play Console!
- [Deploy Command](/reference/deploy) - Using Firebase deployment
- [Setup Wizard](/guide/setup-wizard) - Automated credential configuration
- [CI/CD Integration](/guide/ci-cd) - Automating deployments
- [Configuration Reference](/reference/config-deploy) - All deployment options

## Useful Links

- [Firebase Console](https://console.firebase.google.com/)
- [Firebase App Distribution](https://firebase.google.com/docs/app-distribution)
- [Google Cloud Console](https://console.cloud.google.com/)
- [Firebase CLI](https://firebase.google.com/docs/cli)

---

::: tip Quick Reference
**TL;DR for Firebase only:**
1. Firebase Console → Project settings → Service accounts
2. Create service account in GCP with **Firebase App Distribution Admin** role
3. Download JSON key
4. Create test groups in Firebase Console
5. Add to `mobileops.yaml`: `service_account: credentials/firebase-sa.json`
6. Test: `mobilectl deploy --platform android --destination firebase`

**TL;DR for Firebase + Play Console (same service account):**
1. Create service account with **Firebase App Distribution Admin** role
2. Download JSON key → save as `credentials/google-service-account.json`
3. Grant Play Console permissions via Play Console → API access
4. Use **same JSON file** for both in `mobileops.yaml`
5. Test: `mobilectl deploy --platform android --destination firebase,play-console`

**One service account. Two destinations. Zero hassle.** ✨
:::
