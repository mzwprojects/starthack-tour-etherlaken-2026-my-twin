# Getting Started

This page covers everything you need to build, configure, and run MyTwin locally.

## Requirements

| Requirement | Version |
|---|---|
| Android Studio | Meerkat or newer |
| Android SDK | API 36 |
| Java | 11+ |
| Kotlin | 2.3.21 |
| Gradle | 9.2 (via wrapper) |
| Device | Android 13+ (API 33 minimum) |

!!! note "Samsung Health"
    Full wearable integration requires a **Samsung device with Samsung Health** installed. On non-Samsung devices or emulators the app falls back to manual data entry.

---

## 1. Clone the Repository

```bash
git clone git@github.com:mzwprojects/starthack-tour-etherlaken-2026-my-twin.git
cd starthack-tour-etherlaken-2026-my-twin/apps/mytwin-mobile
```

---

## 2. Configure `local.properties`

The app reads the Claude API key directly from `local.properties` at build time. This file is gitignored and never committed.

Create or open `apps/mytwin-mobile/local.properties` and add:

```properties
sdk.dir=/path/to/your/android/sdk
claudeApiKey=YOUR_ANTHROPIC_KEY
claudeModel=claude-sonnet-4-20250514
```

!!! tip
    `claudeModel` is optional. If omitted, the app defaults to a bundled fallback model constant. For the best twin experience, use a recent Claude Sonnet model.

---

## 3. Build

=== "Debug APK"

    ```bash
    ./gradlew assembleDebug
    ```

=== "Release APK"

    ```bash
    ./gradlew assembleRelease
    ```

---

## 4. Install on Device

```bash
./gradlew installDebug
```

Or drag the APK from `app/build/outputs/apk/debug/` onto a connected device.

---

## 5. Run Tests

=== "Unit tests"

    ```bash
    ./gradlew testDebugUnitTest
    ```

=== "Single test class"

    ```bash
    ./gradlew test --tests "com.mzwprojects.mytwin.simulation.SimulationEngineTest"
    ```

=== "Instrumentation tests (device required)"

    ```bash
    ./gradlew connectedAndroidTest
    ```

---

## Samsung Health Notes

- MyTwin uses the **Samsung Health Data SDK** (bundled as a local `.aar` in `app/libs/`), not Health Connect.
- Local testing may require Samsung Health developer-mode or policy configuration depending on your device setup.
- If Samsung Health is unavailable the onboarding permissions step shows a fallback card and lets you skip to manual data entry.

---

## What Happens on First Launch

1. **Splash screen** — the `SplashScreen` API holds on a teal background while `RootViewModel` reads the onboarding flag from DataStore.
2. If onboarding is not complete → **Welcome Screen → Onboarding Wizard**
3. If already onboarded → directly to **Home Dashboard**

The onboarding flag is set once at the end of the wizard and never repeats.
