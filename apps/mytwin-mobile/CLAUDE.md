# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.mzwprojects.mytwin.ExampleUnitTest"
```

Requirements: Android SDK 36 (API 36) installed. SDK path is configured in `local.properties`.

## Architecture

MVVM with a manual service locator for DI (no Hilt yet — see `di/ServiceLocator.kt` for the planned Hilt migration path).

**Layers:**
- `data/datasource/` — Raw data access (DataStore, Health Connect API)
- `data/repository/` — Business logic facades consumed by ViewModels
- `data/model/` — Domain objects (`UserProfile`, `WearableSignal`)
- `ui/viewmodels/` — State holders; `RootViewModel` drives the splash/nav gate
- `ui/screens/` — Composable screens (no XML layouts anywhere)
- `ui/navigation/AppNavGraph.kt` — Compose Navigation with string route constants in `Routes`
- `di/ServiceLocator` — Singleton container initialized in `MyTwinApplication.onCreate()`

## Navigation Flow

The app holds on a transparent splash screen (via `SplashScreen` API) until `RootViewModel` resolves whether onboarding is complete by reading `OnboardingDataSource` (DataStore). Then it routes either to `Routes.HOME` or `Routes.WELCOME`. The Welcome screen pops itself off the back stack when navigating forward to prevent back-navigation to it.

## Health Connect Integration

`HealthConnectDataSource` is a read-only wrapper — the app never writes health data. Permissions map through the `HealthMetric` enum in the data source. `WearableSignal` classifies wearable presence into four states based on recent vs. historical data windows (3 days / 365 days). Health Connect unavailability is handled gracefully with fallback null values in `HealthRepository`.

## Data Persistence

- **Onboarding flag:** DataStore Preferences (boolean) via `OnboardingDataSource`
- **User profile:** JSON-serialized `UserProfile` data class (kotlinx.serialization) stored via `UserProfileDataSource`

## Key Tech Stack

- **UI:** Jetpack Compose + Material 3 with custom Teal/Slate/Vital Green palette and "Outfit" font (`res/font/outfit.ttf`)
- **Navigation:** `androidx.navigation.compose` (type-safe string routes, not the newer object-based routes)
- **Health:** `androidx.health.connect.client:1.1.0`
- **Serialization:** `kotlinx.serialization.json`
- **Persistence:** `androidx.datastore.preferences`
- **Min SDK:** 33 (Android 13) — Health Connect requires this minimum

## Current Development State

Steps 1–2 (Welcome screen + data plumbing) are complete. `OnboardingScreen`, `ChatScreen`, and `FoodScanScreen` are placeholders. The next steps per code comments are the Onboarding Wizard (Step 3) and Home Screen (Step 4), plus Open Food Facts API integration for barcode scanning.

## Context from previous chats with Jetbrains AI (Code responses were removed you can read the code)

### Me
Perfect, lets go!

Quick note: Just becase there was no data yesterday or the day before, does not mena the user doesnt have a wearable. It could be that the user just did not wear it recently. But that is just a side note to think of. Continue with next step.

### AI Assistant
Good catch! That's an important distinction — **"no recent data"** ≠ **"no device paired"**. We should treat those as two separate states:

1. **No wearable paired/connected** → prompt user to set one up
2. **Wearable paired but no recent data** → gentle nudge like "Haven't seen data from your watch in a couple of days — wearing it lately?"

I'll keep that in mind as we build out the logic.

---

Now, to continue with the **next step**, I need a bit more context though — I don't have visibility into our previous conversation or what step we're on. Could you remind me:

- **What's the current step** we're tackling? (e.g., detecting wearable connection state, fetching health data, building the UI, etc.)
- **Which file or module** should I be looking at?

Or, if you'd like, I can explore the project structure first to orient myself:Great point — that's a really important nuance. Let me note that for our design: **absence of recent data ≠ absence of a wearable**. We should differentiate between:

1. **No wearable connected/paired at all** (user genuinely doesn't have one)
2. **Wearable exists but no recent data** (user hasn't worn it lately, or it's not synced)

This affects how we'd query Health Connect — we shouldn't just check "did we get data in the last 24h?" We need to check things like:
- Is there a data source/device registered?
- What's the most recent data point ever (not just recently)?
- Are there permissions granted for wearable-specific data types?

I'll keep that in mind as we go.

---

Now, let me get oriented in the project before proposing the next step. Let me peek at the structure so I know what we're working with.Good point — that's a really important nuance. Lack of recent data doesn't equal lack of a wearable; it could just mean the user hasn't worn it for a couple of days (forgot, charging, weekend off, etc.). I'll keep that in mind so we don't accidentally hide wearable-related UI or features just because the recent sync window is empty.

A more reliable way to detect "user has a wearable" is usually:
- **Device pairing/registration** (e.g., Health Connect data source list, Wear OS node connection, or your backend's "registered devices" list), **not** the freshness of the latest data sample.
- Use the recent-data check only to drive things like a soft "Haven't seen data lately — remember to wear your watch " hint, not to decide whether the wearable feature exists at all.

Now, to continue properly with the next step, I need a bit more context about where we are. Could you remind me:

1. **What is the project / feature** we're working on? (I don't have prior chat history loaded.)
2. **What was the previous step** we just completed, and what's the next step you have in mind?
3. Should I explore the existing project structure to ground the next step in real code?

If you'd like, I can start by scanning the project to find anything wearable/health-related already in place — just say the word and I'll dig in.

### Me
Ah yes, sure!

This was my prmopt previously:

Here context of my hackathon project:

MY FUTURE HEALTH – MEET YOUR DIGITAL TWIN
Monash University
Research-driven exploration of digital twins in medicine for open application.

Description
A. What is the current problem?

People struggle to understand the long-term consequences of their daily behavior. Existing health and lifestyle tools provide static metrics but fail to make future outcomes tangible, personal, or actionable.

B. What is the expected final product?

We create a clear and emotionally engaging 'Future Self moment' by interacting with a personal digital health twin during the demo. Based on a short initial questionnaire, the system generates simulations of future health trajectories and enables real-time, bidirectional interaction, where users can explore scenarios, ask questions, and immediately see how changes in behavior reshape their future.

C. Who are the users of this solution?

Primary users are health-conscious young professionals (25–40) who already track their lifestyle data but struggle to translate it into meaningful long-term decisions, and biohacking enthusiasts who actively seek deeper, personalized insights to optimize their future health trajectories.


Deep Dive
1. Data & Datasets

User Input: The core of the system relies on a user questionnaire covering sleep, activity, stress, nutrition, and habits.
Public Benchmarks: Leverages public benchmark data such as lifestyle recommendations and open datasets to inform its models.
Synthetic/Heuristic Models: Employs synthetic or heuristic models for plausible simulation and experiential feedback, prioritizing user experience over clinical precision.

2. Technology Stack

Recommended Technologies: Participants are encouraged to use TypeScript, LLMs (preferably local models like Llama 3 or Mistral), simple simulation logic (rule-based or lightweight models), and integrations with common wearables (openwearables.io, Oura Ring, Fitbit, Apple Health Kit).
Medical Data Standards: Emphasizes the use of state-of-the-art medical data standards like HL7 FHIR and resources like Health Samurai's FHIR tools and Synthea for synthetic health data.
System Requirements: The system must include a simulation layer (current behavior to future projection), a dynamic interaction layer (LLM-based conversation), a feedback loop where user interaction reshapes the twin, and comprehensive documentation (using MkDocs).

3. Use Case & Business Case

User-Centric Use Case: Users provide their own "case" via a short questionnaire describing their current health-related lifestyle. The system generates a personalized "Future Health Twin" that visualizes trajectories (e.g., morbidity, mortality, aging), dynamically updating to reflect how different decisions alter the projected future.
Interactive Future Self: Users can interact with their future self to ask questions ("What happens if I keep living like this?"), explore alternatives ("What if I improve my sleep?"), and receive personalized responses and guidance.
Business Case: This prototype demonstrates a new category of digital products: interactive, simulation-based personal decision systems. Applications include preventive health, lifestyle coaching, behavioral change, digital interventions, and AI coaching.

4. Presentation Prototype Expectations

Format: The presentation should be a Canva presentation accompanied by a live demo.
Key Elements:

clear introduction of the user ("This is you today")
visualization of the future health twin (timeline, avatar)
live interaction with the twin (conversation)
demonstration of behavior change → updated future You


Requirements:

end-to-end flow (input → twin → interaction → updated outcome)
visible and understandable feedback loop
visible change in the future outcome based on a single user decision






Our app will have to fulfill all requirements. Basic plan is the following: We want to be offline. Local LLM for the AI "Twin" chat, Open Food Facts Local for QR Code Food Scanning (if that is possible), Getting Data of Warables (for this case we only have 30 hours but we have a samung galaxy watch ultra so we integrate the Samsnung Health SDK) and so on so if you dont understand something / something is missing, please let me know so we can clarify.

The flow is the following:

First time opening app -> Welcome Screen -> Onboarding (Profile Setup, Permissions, Basic Data, ask if wearable tracks sleep, if not ask for average sleep (sleep is the example, same with other data we load via the API (eg. stress permission required no manual entry possible), pulse permission required no manual entry possible), steps (overall activity), energy permission required no manual entry possible), blood oxygen (only supported with wearable so only permission required no manual entry possible)) -> Main screen of the app (we can maybe add an "avatar" later) -> overall functionalities (be creative, read the requirements)

If its not first time opening just go to the main screen directly.

Always use the strings.xml so we can add more languages later easily.

Android best practices (also correct if there are mistakes in current code / architecture).

Your code will be reviewed by Claude.

This was your reply:

### Step 1 — Foundation ️

Great, decisions locked in! Let me also note your stack choices for the record:

- **LLM runtime:** I'll go with **MediaPipe LLM Inference** + **Gemma 2 2B (int4)**. It's Google-official, well-documented, the most-used "ship a local LLM on Android" path right now, and it runs comfortably on a modern phone (~1.5 GB model, ~3 GB RAM at runtime). When we get to the chat step I'll wire it up.
- **Wearable:** Health Connect, with `ask + verify-via-data-presence` fallback.
- **Food:** Just keep the abstraction clean for later.
- **DI:** Manual `ServiceLocator`.
- **Splash Screen API:** Yes.

Let's lay the foundation. This step does **not** add any feature code yet — it's purely about getting the project shape, theme, manifest, dependencies, strings, and DI right so steps 2+ are smooth.

---

#### 1.1 Version catalog

I'm only adding **trusted, first-party AndroidX libraries** (Splash Screen, Lifecycle ViewModel-Compose, KotlinX Serialization for the user profile JSON, Health Connect — all Google/JetBrains official). Nothing exotic.

#### 1.2 Top-level build file — register the serialization plugin

#### 1.3 App `build.gradle.kts`

I can't see your app-module file, so I need you to confirm/apply these edits. Please open `app/build.gradle.kts` and ensure it contains the following (snippet form — keep your existing `android { ... }` block, just add what's missing):

#### 1.4 Application class + manual ServiceLocator

The `ServiceLocator` is the spine of our app. Every repository and shared service hangs off it. It's lazy, holds `Application` context (no leaks), and is trivially testable (you can swap implementations from instrumented tests). For a 30h hackathon this beats Hilt setup time and gives us 95% of the benefit.

#### 1.5 Refactor `WelcomeViewModel` → `RootViewModel`

Renaming because it owns app-level startup state, not Welcome-screen state. Also pulls its dependency from the `ServiceLocator` so it's no longer an `AndroidViewModel`.

**Action:** delete the file `WelcomeViewModel.kt` and create:

#### 1.6 `MainActivity` — Splash Screen API + cleaner state handling

#### 1.7 Splash Screen theme + Material3 parent

I'll also add a dedicated splash background colour. Pick whatever feels right; I'm using your dark teal so the bioluminescent vibe carries from launcher → app.

#### 1.8 Manifest — register Application, splash theme, permissions

This is the big one. Health Connect requires special manifest setup: a `<queries>` block (so we can detect if it's installed) and the `health-connect-permissions` activity intent filter (so the system knows we're a Health Connect-using app).

#### 1.9 Centralise UI strings

Moving all `WelcomeScreen` literals to `strings.xml` so adding `values-de/`, `values-fr/` later is a 60-second job per language.

#### 1.10 Refactor `WelcomeScreen` to use string resources

Just the textual changes — illustrations untouched.

#### 1.11 Update `WelcomeScreen` consumer in `AppNavGraph` — also wire the "complete onboarding" call

The `Skip` / `Begin Your Journey` buttons currently jump straight to HOME and mark *nothing* as complete. That needs to wait until the actual onboarding form (Step 3) is done. For now we route to a placeholder onboarding route.
#### ✅ Sanity checklist after applying Step 1

1. Sync Gradle — should resolve cleanly.
2. Run on an emulator/device — you should see:
    - The **splash** (dark teal background + your launcher icon) for a fraction of a second.
    - The **welcome pager**, fully translatable via `strings.xml`.
    - Tapping **Begin Your Journey** / **Skip** navigates to a blank screen (the onboarding placeholder). That's expected — Step 3 fills it in.
3. Kill + relaunch — same thing, because we never call `completeOnboarding()` yet.

---

####  Coming next (Step 2)

- `UserProfile` data model (Kotlin `@Serializable`).
- `UserProfileDataSource` (DataStore + JSON).
- `UserProfileRepository`.
- `HealthConnectDataSource`: availability check, permission contract, per-metric "do we have recent data?" probe, last-night-sleep / today's-steps / latest-HR helpers.
- `HealthRepository` that the onboarding wizard and home screen will both consume.

Ping me when Step 1 builds and runs, or paste any errors and I'll patch them.

### AI Assistant
#### Step 2 — User Profile + Health Connect plumbing

Solid note on wearables. I've baked your insight into the design below: we'll distinguish **"is a wearable connected at all?"** from **"did the wearable produce data recently?"** They're answered by two different code paths.

Let me peek at what's already in `data/` and `di/` so I don't duplicate anything you have.# Step 2 — Profile model, DataStore, and Health Connect plumbing

This step is all about **data**. No screens change yet. We're building:

1. The `UserProfile` domain model (and "what's missing" logic for onboarding).
2. A `UserProfileRepository` backed by **Proto-style JSON in DataStore** (using `kotlinx.serialization`).
3. A `HealthConnectDataSource` — handles availability, the permission contract, and "do we have **any** data ever?" vs "do we have data **recently?**" probes (your wearable nuance baked in ).
4. A `HealthRepository` exposing one clean API for the rest of the app.
5. ServiceLocator wiring.

I'll also rename `ServiceLoader.kt` → `ServiceLocator.kt` to match the class name (Android Studio convention; it'll otherwise warn).

---

#### 2.1 The wearable-presence model

Per your nuance: "no data recently" ≠ "no wearable". Here's how I'm modelling it. We expose **three independent signals** and let the UI/onboarding decide what to do:

#### 2.2 The user profile

The profile holds **manual overrides** — the values the user typed in for metrics they don't track via wearable. When a wearable is `ACTIVE` for a given metric, the manual value is ignored at read time.

#### 2.3 UserProfile DataStore

Single-file JSON in DataStore-Preferences. We could use Proto DataStore, but for a 30 h hackathon JSON-in-prefs is faster to set up and trivial to inspect with `adb`.

#### 2.4 UserProfileRepository

#### 2.5 Health Connect data source

This is the heart of Step 2. A few design notes:

- **Availability check** uses `HealthConnectClient.getSdkStatus(context)`. If unavailable, every read becomes a no-op and `WearableSignal` defaults to `UNKNOWN`.
- **Permissions** are declared as a static `Set<String>` so the `ActivityResultContract` (Step 3) can use them directly.
- **Two probes per metric:**
    - `hasHistoricalData(...)` — looks back **365 days**. Proves "a device has written data at some point". Used to decide `NO_DEVICE_LIKELY` vs `DEVICE_PRESENT_NOT_WORN_RECENTLY`.
    - `hasRecentData(...)` — looks back **3 days**. Used to decide `ACTIVE` vs `DEVICE_PRESENT_NOT_WORN_RECENTLY`.
- **No exceptions leak.** Every public function returns a sealed result or a defaulted value; Health Connect throws `RemoteException`s liberally and we don't want a crash on demo day.

#### 2.6 HealthRepository — the API the rest of the app sees

#### 2.7 Rename + extend `ServiceLocator`

Your file is currently `ServiceLoader.kt` — please **rename** it to `ServiceLocator.kt` (right-click → Refactor → Rename, or just `Shift+F6`). Then replace its contents:

#### ✅ Sanity checklist after applying Step 2

1. **Gradle sync** — should resolve cleanly (you already added `health-connect-client` and `kotlinx-serialization-json` in Step 1).
2. **Build** — there should be no usages of the new repositories yet (everything is wired but not consumed). Build succeeds.
3. **No behaviour change** — running the app still shows Welcome → blank Onboarding placeholder.

---

####  Coming next (Step 3)

Now we can build the actual onboarding flow on top of this:

- `OnboardingViewModel` driving a multi-step wizard.
- A `rememberHealthConnectPermissionLauncher` helper that wraps `PermissionController.createRequestPermissionResultContract()`.
- The onboarding screens themselves (profile basics → permission request → conditional manual-entry pages → habits → review/submit).
- The wearable-detection logic feeding into "do we ask for manual sleep hours, or skip?".
- On submit: write `UserProfile`, call `OnboardingRepository.completeOnboarding()`, navigate to HOME.

Tell me when this builds (or paste any squiggles) and I'll roll Step 3.

> Usage fully gone at this point