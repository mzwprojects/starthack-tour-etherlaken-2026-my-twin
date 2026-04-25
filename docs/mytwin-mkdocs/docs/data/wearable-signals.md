# Wearable Signals

`WearableSignal` is a four-state enum that represents how confident the app is that a given Samsung Health metric is being actively tracked by a wearable.

---

## States

```kotlin
enum class WearableSignal {
    UNKNOWN,
    NO_DEVICE_LIKELY,
    DEVICE_PRESENT_NOT_WORN_RECENTLY,
    ACTIVE,
}
```

| State | Meaning | Triggered when |
|---|---|---|
| `UNKNOWN` | Can't determine wearable status | Permission denied or Samsung Health unavailable |
| `NO_DEVICE_LIKELY` | Permission granted but no data ever | No historical data in the past 365 days |
| `DEVICE_PRESENT_NOT_WORN_RECENTLY` | Device exists but idle | Historical data exists, but nothing in the last 3 days |
| `ACTIVE` | Wearable is syncing | Recent data present (last 3 days) |

---

## Why Four States?

A critical design insight: **absence of recent data ≠ absence of a wearable**.

A user may have a Galaxy Watch but simply not worn it for a few days — left it charging, took a break, or forgot. If the app concluded `NO_DEVICE_LIKELY` from that, it would wrongly hide wearable features and ask for manual entry from someone who has a perfectly good watch.

By distinguishing `DEVICE_PRESENT_NOT_WORN_RECENTLY` from `NO_DEVICE_LIKELY`, the app can show a gentle nudge ("wear your watch again") instead of removing the feature entirely.

---

## Detection Logic

`SamsungHealthDataSource` uses two independent time-window probes per metric:

| Probe | Window | Determines |
|---|---|---|
| `hasHistoricalData(metric)` | 365 days back | Whether *any* device has ever written this metric |
| `hasRecentData(metric)` | 3 days back | Whether the wearable is actively syncing |

Combined:

```
permission not granted → UNKNOWN
permission granted + no historical → NO_DEVICE_LIKELY
permission granted + historical + no recent → DEVICE_PRESENT_NOT_WORN_RECENTLY
permission granted + recent data → ACTIVE
```

---

## Usage in the App

### Onboarding

The Permissions step (`OnboardingStep.PERMISSIONS`) shows one `PermissionStatusRow` per metric, each displaying its current `WearableSignal`. This gives the user immediate feedback on what the app can and can't see.

### Manual Data Step

`OnboardingStep.MANUAL_DATA` uses the signal to decide what to ask:

- `ACTIVE` → skip the manual slider (wearable covers it)
- `DEVICE_PRESENT_NOT_WORN_RECENTLY` → skip (device is present; the user can re-wear it)
- `NO_DEVICE_LIKELY` or `UNKNOWN` → show the manual slider

### Home Dashboard

If any metric is `DEVICE_PRESENT_NOT_WORN_RECENTLY`, `HomeViewModel` sets `showWearableNudge = true` and the nudge card is shown on the Home screen for today.

### Simulation

`SimulationRepository` uses the signal to determine `wearableCoverageDays` and `basedOnManualFallbacks`, which directly affect `SimulationConfidence`.
