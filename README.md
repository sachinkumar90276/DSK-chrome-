# DSK CHROME

**Developer:** DSK OFFICIAL  
**Application Name:** DSK CHROME  
**Android Application ID:** `com.aistudio.dskchrome.vqmrkz`  
**Stack:** Native Android (Kotlin, Jetpack Compose, Material 3, Android WebView, Room, DataStore, Firebase AI Logic, Gradle)

> **Architecture Note:** This repository is already a native Android Kotlin & Jetpack Compose application (not a React/Vite web project), so Capacitor is not used and the native Compose/WebView implementation is preserved directly without adding a redundant wrapper.

---

## Features

- **Real WebView Browsing:** Live web rendering with pull/reload controls, progress indicator, SSL/TLS security inspection, Safe Browsing checks, Desktop Site toggle, Find in Page, and external URI scheme handling.
- **Multi-Tab & Incognito Browsing:** `ViewModel` + `StateFlow` tab management supporting tab creation, switching, duplication, closing, restoring recently closed tabs, and private Incognito sessions.
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
│       └── build-android.yml        # Automated CI build & APK artifact upload
├── app/
│   ├── build.gradle.kts             # App module Gradle configuration
│   ├── google-services.json         # Firebase configuration
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
- JDK 21
- Android SDK (API 36)
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
4. Locate the generated APK at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## GitHub Actions Automated APK Build

The workflow file at `.github/workflows/build-android.yml` builds the Android debug APK automatically on:
- Any `push` to `main` or `master`
- Any `pull_request` targeting `main` or `master`
- Manual trigger (`workflow_dispatch`)

### How to Run the Workflow Manually
1. Open the repository on GitHub (`sachinkumar90276/DSK-chrome`).
2. Go to the **Actions** tab.
3. Select **Build DSK CHROME Android APK** from the left sidebar.
4. Click **Run workflow**, choose your branch (e.g. `main`), and click **Run workflow**.

### Where to Download the APK
1. Open the completed workflow run under the **Actions** tab.
2. Scroll down to the **Artifacts** section at the bottom of the run summary page.
3. Click **`DSK-CHROME-ANDROID-APK`** to download the zip containing `app-debug.apk`.
