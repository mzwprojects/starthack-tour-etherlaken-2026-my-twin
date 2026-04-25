# User Profile

`UserProfile` is the central domain model for all data collected during onboarding. It is stored as a JSON-serialized string in DataStore Preferences and used by the simulation engine, the AI context builder, and the Home screen.

---

## Data Class

```kotlin
@Serializable
data class UserProfile(
    // Identity
    val displayName: String? = null,
    val ageYears: Int? = null,
    val biologicalSex: BiologicalSex? = null,
    val heightCm: Int? = null,
    val weightKg: Float? = null,

    // Manual lifestyle baselines
    val averageSleepHours: Float? = null,
    val averageDailySteps: Int? = null,
    val perceivedStressLevel: Int? = null,  // 1..10

    // Current-day overrides (Home screen)
    val currentSleepHoursOverride: Float? = null,
    val currentStepsOverride: Int? = null,
    val currentHeartRateOverride: Int? = null,
    val currentStressLevelOverride: Int? = null,

    // Habits
    val smokingStatus: SmokingStatus? = null,
    val alcoholDrinksPerWeek: Int? = null,
    val dietQuality: DietQuality? = null,

    // Bookkeeping
    val onboardingCompletedAtEpochMs: Long? = null,
)
```

All fields are nullable. `null` means "not yet provided" — never conflated with `0` or any other sentinel.

---

## Field Groups

### Identity

Basic demographic data. Used in the simulation engine (age contributes to long-term risk) and in the AI context prompt (age, sex, height, weight).

`displayName` is shown in the Home screen greeting only — it is **not** included in the AI context to preserve the "speaking as yourself" persona.

### Manual Lifestyle Baselines

Collected during onboarding Step 3. Used as fallbacks when no wearable data is available:

- `averageSleepHours` → fallback for `SimulationInput.sleepHoursAvg7d`
- `averageDailySteps` → fallback for `SimulationInput.stepsAvg7d`
- `perceivedStressLevel` → fallback for `SimulationInput.stressAvg7d`

### Current-Day Overrides

Set from the Home screen's manual override dialogs. Take precedence over wearable data for the currently displayed day. Historical wearable records are never touched.

Override priority: `currentOverride > wearable > baseline fallback > null`

### Habits

Always manual — no wearable measures smoking or diet. Fed directly into the simulation risk scores.

| Enum | Values |
|---|---|
| `SmokingStatus` | NEVER, FORMER, OCCASIONAL, REGULAR |
| `DietQuality` | POOR, AVERAGE, GOOD, EXCELLENT |
| `BiologicalSex` | MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY |

---

## Persistence

`UserProfileDataSource` serializes the entire `UserProfile` to a JSON string using `kotlinx.serialization` and stores it as a single DataStore Preferences key.

```kotlin
// Simplified read/write pattern
val profileFlow: Flow<UserProfile?> = dataStore.data
    .map { prefs -> prefs[KEY_PROFILE]?.let { Json.decodeFromString(it) } }

suspend fun save(profile: UserProfile) {
    dataStore.edit { prefs ->
        prefs[KEY_PROFILE] = Json.encodeToString(profile)
    }
}
```

Using JSON rather than Proto DataStore avoids the schema-compilation step, which matters at hackathon speed. The stored JSON is human-readable and inspectable with `adb shell`.
