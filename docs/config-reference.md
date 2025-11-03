# mobileops.yml Configuration Reference

## Zero-Config Philosophy

mobilectl works **without a config file**. Just run:

mobilectl build # Auto-detects Android/iOS, uses smart defaults
mobilectl deploy # Auto-uploads to ./builds folder


Create `mobileops.yml` only to **customize** default behavior.

---

## Auto-Detection: What mobilectl Does Automatically

### Platform Detection

| Platform | Detection Method | Auto Build Command |
|----------|-----------------|-------------------|
| **Android** | Finds `build.gradle` or `build.gradle.kts` | `./gradlew assembleRelease` |
| **iOS** | Finds `*.xcodeproj` or `*.xcworkspace` | `xcodebuild -scheme [first-scheme] -configuration Release` |

### Version Detection

| Source | Priority | Example |
|--------|----------|---------|
| `build.gradle.kts` | 1st | `version = "1.0.0"` |
| `Info.plist` | 2nd | `CFBundleShortVersionString: 1.0.0` |
| `package.json` | 3rd | `"version": "1.0.0"` |
| Fallback | Default | `1.0.0` |

### App Identifier

| Platform | Detection Method |
|----------|-----------------|
| **Android** | `applicationId` from `build.gradle.kts` |
| **iOS** | `CFBundleIdentifier` from `Info.plist` |

### App Name

| Platform | Detection Method |
|----------|-----------------|
| **Android** | `app:name` from `AndroidManifest.xml` |
| **iOS** | `CFBundleName` from `Info.plist` |

---

## Minimal Config Examples

### Example 1: Zero Config (Just Run It!)

**No `mobileops.yml` needed!**

$ mobilectl info
✅ Project Information
├─ Android: detected ✅
│ ├─ Identifier: com.example.myapp
│ ├─ Version: 1.0.0
│ └─ Gradle Task: assembleRelease
├─ iOS: detected ✅
│ ├─ Identifier: com.example.myapp
│ ├─ Scheme: MyApp
│ └─ Version: 1.0.0
└─ Config: No config file (using auto-detection)

$ mobilectl build
✅ Building Android...
✅ Building iOS...
✅ All builds complete!

$ mobilectl deploy
✅ Uploading artifacts to ./builds
✅ Deploy complete!

### Example 2: Minimal Config (Custom Gradle Task)
Only override what you need
build:
android:
gradle_task: "bundleRelease" # Use App Bundle instead of APK

Then:

mobilectl build # Uses bundleRelease, iOS auto-detects


### Example 3: Minimal Config + Slack

Only configure what's different from defaults
notify:
slack:
enabled: true
webhook_url: "${SLACK_WEBHOOK_URL}"
notify_on: ["success", "failure"]


### Example 4: Multi-Platform Build Config

Override iOS scheme if you have multiple
build:
ios:
scheme: "MyAppProduction" # If you have MyAppStaging, MyAppDev, etc.

Everything else auto-detects


---

## Default Behavior (No Config File)

If no `mobileops.yml` exists, mobilectl uses these defaults:

Build:
Android: gradle_task = "assembleRelease"
iOS: first available scheme, configuration = "Release"

Deploy:
Destinations: ["local: ./builds"]

Notify:
All disabled

Report:
Disabled


---

## When to Add Config

| Scenario | What to Add |
|----------|------------|
| Single platform (Android OR iOS) | Skip iOS/Android section in config |
| Custom Gradle task | `build.android.gradle_task` |
| Multiple iOS schemes | `build.ios.scheme` |
| Signing/Keystore | `build.android.keystore` |
| Upload to Firebase | `deploy.destinations` |
| Slack notifications | `notify.slack` |
| Custom app name/version | `app.*` |

---

## Command Reference

### Build

Auto-detect and build all platforms
mobilectl build

Explicit platform
mobilectl build android
mobilectl build ios

Show what would happen (dry-run)
mobilectl build --dry-run

Verbose output
mobilectl build --verbose

### Deploy

Deploy to default (./builds)

mobilectl deploy

Deploy to specific destination
mobilectl deploy --destination firebase
mobilectl deploy --destination app-center

Deploy to multiple destinations
mobilectl deploy --destinations local,firebase

Show what would happen
mobilectl deploy --dry-run

### Info

Show detected project configuration
mobilectl info

Show detailed config
mobilectl info --detailed

### Version

Auto-increment version
mobilectl version bump patch # 1.0.0 → 1.0.1
mobilectl version bump minor # 1.0.0 → 1.1.0
mobilectl version bump major # 1.0.0 → 2.0.0

Show current version
mobilectl version

### Changelog

Generate changelog
mobilectl changelog generate

Generate from tag
mobilectl changelog generate --from v1.0.0


---

## Best Practices

1. **Start with zero config** — Just run `mobilectl build`
2. **Add config gradually** — Only customize what you need
3. **Commit to git** — Include `mobileops.yml` in version control
4. **Use env vars for secrets** — Never hardcode passwords/keys
5. **Use `mobilectl info`** — Verify auto-detected values before first build

---

