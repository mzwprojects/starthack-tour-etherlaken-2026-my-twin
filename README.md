# MyTwin - START Hack Tour 2026 Interlaken

**Track:** My Future Health - Meet Your Digital Twin  
**Case Partner:** Monash University  
**Platform:** Android  
**Project Type:** Personal health twin

---

## The Vision

Most health apps show isolated numbers. They tell you how many steps you took, how long you slept,
or what your heart rate is, but they do not turn those signals into something personal, explainable,
and emotionally tangible.

**MyTwin** turns recent health behavior into a living digital twin:

- It pulls wearable data from Samsung Health
- Combines it with user profile and habit data
- Simulates short-term and longer-term trajectory signals
- Lets the user chat with an AI version of themselves

The result is a health experience that feels less like a dashboard and more like a conversation with
your future self.

## Key Features

### 1. Samsung Health Integration

- Reads recent and historical wearable-backed health data from Samsung Health
- Supports:
    - Sleep
    - Steps
    - Heart rate
    - Stress proxy / energy-derived context

### 2. Manual Overrides

- Manual input is not just a fallback baseline
- On the Home screen, the user can override the **currently shown** values for today if the wearable
  data is delayed, missing, or wrong
- Historical wearable data stays intact for later reasoning and simulation

### 3. 7-Day Health View

- Home supports browsing the last 7 days
- Historical days are shown as read-only snapshots
- Aggregates:
    - Daily steps
    - Average heart rate for the day
    - Daily stress signal
    - Latest relevant sleep sample for that day

### 4. Rule-Based Simulation Layer

- A fast, explainable, and rule-based (offline) simulation
- Generates internal scores for:
    - Recovery
    - Energy
    - Focus
    - Strain
    - Long-term risk
- Produces:
    - Short-term insights
    - Improvement recommendations
    - Longer-term trajectory signals
    - What-if scenario comparisons

### 5. AI Twin Chat with Claude

- Real chat experience inside the app
- Claude answers in a "digital twin" voice rather than as a generic assistant
- Prompt includes:
    - Anonymized context
    - Simulation baseline
    - Precomputed what-if scenarios
- Responses stream into the UI progressively to feel live and conversational

---

## Tech Stack

### Android App

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **AndroidX Navigation Compose**
- **DataStore Preferences**
- **kotlinx.serialization**

### Health Data

- **Samsung Health Data SDK**
- Historical and recent data reads for health metrics

### AI / Simulation

- **Claude API** for twin chat
- **Rule-based simulation engine** in pure Kotlin

### Architecture

- **MVVM**
- **Manual Service Locator** for dependency wiring
- Clean layering across:
    - `data/datasource`
    - `data/repository`
    - `simulation`
    - `chat`
    - `ui/viewmodels`
    - `ui/screens`

---

## How To Run Locally

### Requirements

- Android Studio
- Android SDK 36
- Java 11+
- A Samsung device with Samsung Health if you want full wearable integration

### 1. Configure `local.properties`

This project reads the Claude key directly from `local.properties` for demo purposes.

Add:

```properties
claudeApiKey=YOUR_ANTHROPIC_KEY
claudeModel=claude-sonnet-4-20250514
```

`claudeModel` is optional. If omitted, the app uses a default Claude Sonnet model.

### 2. Build the App

```bash
./gradlew assembleDebug
```

### 3. Run Unit Tests

```bash
./gradlew testDebugUnitTest
```

### 4. Install on Device

```bash
./gradlew installDebug
```

---

## Samsung Health Notes

- The app uses **Samsung Health**, not Health Connect
- Local testing may require Samsung Health developer-mode / policy configuration depending on the
  device setup

---

## Current MVP Scope

- Onboarding flow
- Samsung Health integration
- Home dashboard
- 7-day history view
- Manual current-value overrides
- Simulation layer
- Claude-based twin chat
- Food scanner placeholder screen to show future potential

## Framing

MyTwin is not trying to be a clinical decision engine. It is a research-driven, interactive
prototype for **preventive health reflection**, **behavior change motivation**, and **future-self
exploration**.

That is exactly where the digital twin concept becomes compelling in a hackathon demo: not just
showing data, but making future consequences feel personal.

---

*Built for START Hack Tour 2026 in Interlaken.*
