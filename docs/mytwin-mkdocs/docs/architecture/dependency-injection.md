# Dependency Injection

MyTwin uses a **manual Service Locator** instead of Hilt or Dagger. The `ServiceLocator` object lives in `di/ServiceLocator.kt` and is initialized exactly once in `MyTwinApplication.onCreate()`.

---

## Why Not Hilt?

For a 36-hour hackathon:

- Zero annotation processing overhead — faster builds
- No generated code to debug under time pressure
- Fully explicit — every dependency is visible in one file
- Trivially swappable in tests (just replace the lazy val value if needed)

The trade-off is manual wiring. For a production app, migrating to Hilt is the logical next step.

---

## Structure

```kotlin
object ServiceLocator {

    fun init(context: Context) { ... }

    // ─── Data sources ─────────────────────────────────────
    val onboardingDataSource: OnboardingDataSource by lazy { ... }
    val userProfileDataSource: UserProfileDataSource by lazy { ... }
    val samsungHealthDataSource: SamsungHealthDataSource by lazy { ... }

    // ─── Repositories ─────────────────────────────────────
    val onboardingRepository: OnboardingRepository by lazy { ... }
    val userProfileRepository: UserProfileRepository by lazy { ... }
    val healthRepository: HealthRepository by lazy { ... }

    // ─── Simulation ───────────────────────────────────────
    val simulationEngine: SimulationEngine by lazy { ... }
    val simulationRepository: SimulationRepository by lazy { ... }

    // ─── Chat ─────────────────────────────────────────────
    val claudeContextBuilder: ClaudeContextBuilder by lazy { ... }
    val claudeApiService: ClaudeApiService by lazy { ... }
    val chatRepository: ChatRepository by lazy { ... }
}
```

Every property is a `lazy` delegate — created only when first accessed. Nothing is allocated until the first screen that needs it renders.

---

## Initialization

```kotlin
class MyTwinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
```

Declared in `AndroidManifest.xml` via `android:name=".MyTwinApplication"`.

---

## ViewModel Access Pattern

ViewModels access the ServiceLocator through a factory or directly in `init {}`:

```kotlin
class HomeViewModel : ViewModel() {
    private val userProfileRepository = ServiceLocator.userProfileRepository
    private val healthRepository = ServiceLocator.healthRepository
    ...
}
```

Because `ServiceLocator` is an `object`, ViewModels can reference it without holding an `Application` reference, keeping them as plain `ViewModel` subclasses (no `AndroidViewModel` needed).

---

## Future Migration Path

The ServiceLocator boundary is thin by design — each lazy property maps directly to a Hilt `@Provides` function or `@Inject` constructor. When the project grows beyond prototype scope:

1. Add Hilt dependencies
2. Annotate data sources and repositories with `@Inject constructor`
3. Delete `ServiceLocator.kt`
4. Replace ViewModel access with `@HiltViewModel` + `@Inject`
