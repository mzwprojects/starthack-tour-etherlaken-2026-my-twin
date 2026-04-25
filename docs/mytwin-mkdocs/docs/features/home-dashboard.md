# Home Dashboard

The Home screen is the central hub of the app. It shows your current health snapshot, lets you browse the last 7 days, override today's metrics manually, and navigate to the Simulation and Chat features.

---

## Layout

```
┌─────────────────────────────────────┐
│  TODAY                              │
│  Good morning,                      │
│  Alex                               │
│                                     │
│  [Today] [Yesterday] [Mon] [Sun]... │
│                                     │
│  ┌──────────┐  ┌──────────┐         │
│  │ Sleep    │  │ Steps    │         │
│  │ 7.5 h    │  │ 9,240    │         │
│  │ wearable │  │ wearable │         │
│  └──────────┘  └──────────┘         │
│  ┌──────────┐  ┌──────────┐         │
│  │ Heart    │  │ Stress   │         │
│  │ 68 bpm   │  │ 5 / 10   │         │
│  │ wearable │  │ manual   │         │
│  └──────────┘  └──────────┘         │
│                                     │
│  [ Talk to Your Twin  ]             │
│  [ Scan Food (soon)   ]             │
└─────────────────────────────────────┘
```

---

## 7-Day Date Selector

A horizontal scrollable `Row` of `OutlinedButton` chips — one per day covering the last 7 days (including today). The selected day is highlighted with a primary-tinted background.

- **Today**: shows "Today"
- **Yesterday**: shows "Yesterday"
- **Older days**: shows the short weekday name (Mon, Tue, …)

When a historical day is selected, the metrics grid switches to read-only snapshot mode and the manual override section is hidden.

---

## Metric Grid

Four metric cards in a 2×2 grid:

| Metric | Icon | Source Label |
|---|---|---|
| Sleep | Star | `wearable` / `manual` / `manual override` |
| Steps | Play arrow | `wearable` / `manual` / `manual override` |
| Heart rate | Favorite (outline) | `wearable` / `manual override` |
| Stress | Warning | `wearable` / `manual` / `manual override` |

The source label is shown in the tertiary color (vital green) so users can always see where a value came from.

---

## Manual Overrides (Today Only)

Below the metric grid, when viewing today, four `ManualTrackingCard` components let the user edit or clear each metric:

- Tapping **Update** opens a `ManualValueDialog` — an `AlertDialog` with a `Slider`
- Tapping **Clear** removes the override and reverts to wearable or baseline data

### Override Priority

```
Manual override > Wearable data > Profile baseline fallback > null (—)
```

Overrides apply only to the *currently displayed* day. Historical wearable data is never modified.

---

## Simulation Teaser Card

A card with a brief description of the Simulation feature and a **Run Simulation** button that navigates to `SimulationScreen`.

---

## Wearable Nudge

If `showWearableNudge` is true (wearable is `DEVICE_PRESENT_NOT_WORN_RECENTLY`) and you're viewing today, a soft nudge card appears:

> "Haven't seen data from your watch in a few days — wearing it lately?"

---

## Greeting Logic

`HomeViewModel` computes `GreetingTime` based on the current hour:

| Hour | Greeting |
|---|---|
| 5–11 | Good morning |
| 12–17 | Good afternoon |
| 18–23, 0–4 | Good evening |

The greeting is personalized with `profile.displayName` if set.

---

## Data Refresh

The screen uses a `LifecycleEventObserver` to call `vm.refresh()` on every `ON_RESUME`. This ensures that if the user grants wearable permissions and returns, the metrics update automatically.
