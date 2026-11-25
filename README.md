# DSK CHROME

**Developer:** DSK OFFICIAL  
**Application Name:** DSK CHROME  
**Android Application ID:** `com.aistudio.dskchrome.vqmrkz`  
**Stack:** Native Android (Kotlin, Jetpack Compose, Material 3, Android WebView, Room, DataStore, Firebase AI Logic, Gradle 9.3.1 / AGP 9.1.1)

> **Architecture Note:** This repository is a native Android Kotlin & Jetpack Compose application under `app/` (not a React/Vite web project), so Capacitor is not used and the native Compose/WebView implementation is preserved directly.

---

## Features

- **Real WebView Browsing:** Live web rendering with reload/stop controls, progress indicator, SSL/TLS security sheet, Safe Browsing warnings, Desktop Site toggle, Find in Page, and external URI handling.
- **Multi-Tab & Incognito Browsing:** `ViewModel` + `StateFlow` tab management supporting tab creation, switching, duplication, closing, reopening closed tabs, and private Incognito sessions.
- **Chrome-Style Adaptive Toolbar:** Switchable **Top**, **Bottom**, or **Split** navigation & address bar placement in **Settings → Appearance**, plus a Chrome-style 3-dot quick-action menu.
- **Bookmarks, History & Downloads:** Room-backed local persistence for bookmarks, browsing history, shortcuts, site permissions, saved tabs, and file downloads (`DownloadEngine`).
- **Media, Uploads & Permissions:** File upload (`OpenMultipleDocuments` + Camera capture via `FileProvider`), HTML5 fullscreen video support, runtime permission prompts, voice search, and adaptive edge-to-edge UI.
- **DSK AI Search & Page Translation:** Integrated AI assistant and webpage translation powered by Firebase AI Logic.
- **Custom Branding & Launchers:** Custom adaptive launcher icon, animated startup splash overlay, and homepage wallpaper presets or user-selected gallery backgrounds.

---

## Repository Structure

```text
.
├── .github/
│   └── workflows/
│       └── build-android.yml        # Automated CI build, test, APK validation & artifact upload
├── app/
│   ├── build.gradle.kts             # App module Gradle configuration (compileSdk 36.1, minSdk 24)
│   ├── google-services.json         # Firebase configuration (matched to com.aistudio.dskchrome.vqmrkz)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml  # Permissions, MainActivity, FileProvider
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ai/              # Firebase AI Logic & App Check
│       │   │   ├── browser/         # ViewModel, TabStateManager, WebView pool & clients
│       │   │   ├── data/            # Room entities, DAOs, repository, DataStore
│       │   │   ├── download/        # Download manager engine
│       │   │   └── ui/              # Jetpack Compose screens, toolbars, menus, theme
│       │   └── res/                 # Custom DSK CHROME icons, drawables, strings, XML configs
│       └── test/                    # JVM unit tests (ExampleUnitTest)
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts                 # Root Gradle build script
├── settings.gradle.kts              # Project modules & repositories
├── debug.keystore.base64            # Base64-encoded debug keystore for debug builds
└── README.md
```

---

## Building the Android APK Locally

### Prerequisites
- JDK 21 (Temurin / OpenJDK 21)
- Android SDK (`platform-tools`, `platforms;android-36.1`, `build-tools;36.0.0`)
- Gradle 9.3.1

### Commands

1. Decode the debug keystore if `debug.keystore` is not yet present in the root directory:
   ```bash
   base64 --decode debug.keystore.base64 > debug.keystore
   ```
2. Run unit tests:
   ```bash
   gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"
   ```
3. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
4. Locate and verify the generated APK:
   ```bash
   ls -lh app/build/outputs/apk/debug/app-debug.apk
   "$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump badging app/build/outputs/apk/debug/app-debug.apk
   ```

---

## GitHub Actions Instructions

The workflow `.github/workflows/build-android.yml` builds and validates the APK automatically on:
- `push` to `main` or `master`
- `pull_request` targeting `main` or `master`
- Manual trigger (`workflow_dispatch`)

### How to Run Manually
1. Open `sachinkumar90276/DSK-chrome` on GitHub.
2. Open the **Actions** tab.
3. Click **Build DSK CHROME Android APK** in the left sidebar.
4. Click **Run workflow** and select the target branch (`main`).

### Where to Download the APK
1. Open the finished workflow run in the **Actions** tab.
2. Scroll down to the **Artifacts** section.
3. Click **`DSK-CHROME-APK`** to download `app-debug.apk`.

---

## Troubleshooting

- **`Warning: Failed to find package 'tools'` in `Set up Android SDK`:** Fixed by removing `android-actions/setup-android` (which attempts to install the deprecated `tools` SDK package) and directly invoking the runner's `sdkmanager` with `platform-tools`, `platforms;android-36.1`, `platforms;android-36`, and `build-tools;36.0.0`.
- **Missing `debug.keystore` during CI build:** `debug.keystore` is gitignored, so the workflow automatically decodes the committed `debug.keystore.base64` before `gradle :app:assembleDebug`.
- **Firebase / `google-services.json` mismatch:** `app/google-services.json` is registered to `com.aistudio.dskchrome.vqmrkz`; changing `applicationId` without updating `google-services.json` will cause `processDebugGoogleServices` to fail.
