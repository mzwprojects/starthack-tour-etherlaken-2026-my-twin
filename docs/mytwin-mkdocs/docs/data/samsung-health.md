# Samsung Health Integration

MyTwin uses the **Samsung Health Data SDK** (version 1.1.0) rather than Health Connect. The SDK is bundled as a local `.aar` file at `app/libs/samsung-health-data-api-1.1.0.aar`.

---

## Why Samsung Health, Not Health Connect?

For the hackathon, the team had a **Samsung Galaxy Watch Ultra**. The Samsung Health SDK provides direct, low-latency access to all metrics from Samsung wearables without the Health Connect sync delay. For a 36-hour demonstrator with live wearable data, this was the pragmatic choice.

---

## Supported Metrics

`SamsungHealthMetric` is an enum with four values:

| Metric | Samsung Health Data Type |
|---|---|
| `SLEEP` | Sleep session data |
| `STEPS` | Step count |
| `HEART_RATE` | Heart rate samples |
| `STRESS` | Stress score (proxy derived from HR variability) |

---

## `SamsungHealthDataSource`

All SDK access is encapsulated in `SamsungHealthDataSource`. No other class in the codebase imports Samsung Health types.

### Key Operations

| Operation | Purpose |
|---|---|
| Availability check | Detect if Samsung Health is installed and accessible |
| Permission request | Delegate to the Samsung Health permission dialog |
| `hasHistoricalData(metric)` | Looks back 365 days — proves a device has ever written data |
| `hasRecentData(metric)` | Looks back 3 days — detects active wearable use |
| `getTodaySleep()` | Latest sleep session for today |
| `getTodaySteps()` | Cumulative step count for today |
| `getRestingHeartRate()` | Most recent resting HR reading |
| `getRecentHistory(metric, days)` | List of `WearableHistoryPoint<T>` for the last N days |

### Error Handling

Every public method catches all exceptions. Samsung Health throws `RemoteException` and several SDK-specific exceptions during normal operation. Returning `null` or an empty list is always the fallback — the app never crashes on SDK errors.

---

## Permission Flow

Permissions are requested during onboarding via `OnboardingViewModel.requestPermissions(activity)`. The Samsung Health SDK provides its own permission request mechanism through `Activity`.

On `ON_RESUME` in `OnboardingScreen`, `vm.refreshPermissions()` is called to re-check state — so if the user grants permissions in Samsung Health settings and returns to the app, the onboarding step updates automatically.

---

## Developer Notes

- Samsung Health may require **developer mode** to be enabled on the device for full SDK access during development
- Local testing on non-Samsung devices will always show Samsung Health as unavailable — the app gracefully falls back to manual data entry in this case
- The `.aar` in `app/libs/` is the only external binary in the project and is committed to the repo for reproducible builds
