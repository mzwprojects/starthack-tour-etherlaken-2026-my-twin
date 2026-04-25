# Architecture Layers

A detailed look at each layer, what lives in it, and how data flows through the stack.

---

## Data Sources

Located in `data/datasource/`.

Data sources are the only layer that knows about Android platform specifics or external SDKs.

### `OnboardingDataSource`

Wraps a **DataStore Preferences** instance. Stores and reads a single boolean: whether onboarding has been completed.

```kotlin
// Key usage pattern
val onboardingCompleted: Flow<Boolean>
suspend fun setOnboardingCompleted(completed: Boolean)
```

### `UserProfileDataSource`

Wraps a **DataStore Preferences** instance. Stores the full `UserProfile` data class as a JSON string using `kotlinx.serialization`. Exposes it as a `Flow<UserProfile?>` and provides a `save(profile)` suspend function.

### `SamsungHealthDataSource`

Wraps the **Samsung Health Data SDK**. Handles:

- Availability detection
- Permission request delegation
- Per-metric data reads (sleep, steps, heart rate, stress)
- Historical vs. recent data probing to populate `WearableSignal` states

All reads are wrapped in `try/catch` — Samsung Health throws liberally and the app must never crash during a demo.

---

## Repositories

Located in `data/repository/`, `simulation/`, and `chat/`.

Repositories aggregate data sources and expose clean, domain-oriented APIs to ViewModels.

### `OnboardingRepository`

Thin facade over `OnboardingDataSource`. ViewModels call `isOnboardingComplete(): Flow<Boolean>` and `completeOnboarding()`.

### `UserProfileRepository`

Facade over `UserProfileDataSource`. Provides `getProfile(): Flow<UserProfile?>` and `saveProfile(profile)`. Contains no business logic — the ViewModel drives profile mutations.

### `HealthRepository`

Facade over `SamsungHealthDataSource`. Maps raw SDK results to domain types (`WearableSignal`, `WearableHistoryPoint`). Shields the rest of the app from Samsung SDK types entirely.

Key methods:

```kotlin
suspend fun getWearableSignal(metric: SamsungHealthMetric): WearableSignal
suspend fun getTodaySteps(): Int?
suspend fun getTodaySleep(): Float?
suspend fun getRestingHeartRate(): Int?
suspend fun getRecentHistory(metric, days): List<WearableHistoryPoint<*>>
```

### `SimulationRepository`

Aggregates `UserProfileRepository`, `HealthRepository`, and `SimulationEngine`. The single entry point for computing a `SimulationBundle`:

1. Loads the current `UserProfile`
2. Reads recent wearable history (7-day averages, 3-day HR average)
3. Applies manual fallbacks where wearable data is absent
4. Computes `SimulationInput`
5. Passes it to `SimulationEngine.evaluate()` and `SimulationEngine.compare()` for each scenario
6. Returns the complete `SimulationBundle`

### `ChatRepository`

Assembles the full context for an AI chat turn:

1. Loads `UserProfile`
2. Loads `HealthRepository` history
3. Calls `SimulationRepository` for the current `SimulationBundle`
4. Passes everything to `ClaudeContextBuilder.build()` to produce a `ClaudeContextSnapshot`
5. Delegates streaming to `ClaudeApiService.streamMessage()`

---

## ViewModels

Located in `ui/viewmodels/`.

ViewModels hold `StateFlow`-backed `UiState` data classes and expose `suspend fun` or regular fun actions to the UI.

| ViewModel | Screen | Key State |
|---|---|---|
| `RootViewModel` | `MainActivity` | `startDestination: String` |
| `OnboardingViewModel` | `OnboardingScreen` | `OnboardingUiState` (step, field values, permission signals) |
| `HomeViewModel` | `HomeScreen` | `HomeUiState` (metrics, date, override flags) |
| `SimulationViewModel` | `SimulationScreen` | `SimulationUiState` (loading, bundle, error) |
| `ChatViewModel` | `ChatScreen` | `ChatUiState` (messages, input, isSending) |

### `RootViewModel`

Reads `OnboardingRepository.isOnboardingComplete()` and immediately produces a `startDestination` string (`Routes.HOME` or `Routes.WELCOME`). `MainActivity` waits for this before rendering `AppNavGraph`.

---

## Screens

Located in `ui/screens/`.

All screens are `@Composable` functions. They observe `vm.uiState.collectAsStateWithLifecycle()` and call ViewModel methods in response to user actions. No business logic lives here.

Each screen receives navigation callbacks as lambda parameters — screens never know about the `NavController` directly.

---

## Models

Located in `data/model/`, `simulation/`, and `chat/`.

Pure data classes and enums with zero Android dependencies.

Key types:

| Type | Location | Purpose |
|---|---|---|
| `UserProfile` | `data/model` | All onboarding-collected data for a user |
| `WearableSignal` | `data/model` | Four-state enum for wearable presence |
| `WearableHistoryPoint<T>` | `data/model` | A timestamped data point from the wearable |
| `SimulationInput` | `simulation` | Flattened inputs consumed by `SimulationEngine` |
| `SimulationReport` | `simulation` | Scores + narrative for one simulation run |
| `SimulationBundle` | `simulation` | Baseline report + all scenario comparisons |
| `ChatMessage` | `chat` | A single message in the conversation |
| `ClaudeContextSnapshot` | `chat` | The assembled system prompt for Claude |
