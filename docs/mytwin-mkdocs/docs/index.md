# MyTwin

**Team:** 404 Brain Not Found (Miloh Zwahlen, Keith Mader)

**Case:** My Future Health - Meet Your Digital Twin  

**Platform:** Android  

**Project Type:** Personal health twin

**Event:** START Hack Tour 2026 Interlaken

---

## What is MyTwin?

Most health apps show isolated numbers. They tell you how many steps you took, how long you slept, or what your heart rate is — but they don't turn those signals into something *personal*, *explainable*, and *emotionally tangible*.

**MyTwin** turns your recent health behavior into a living digital twin:

- It pulls wearable data from **Samsung Health**
- Combines it with your profile and habit data
- **Simulates** short-term and longer-term health trajectories
- Lets you **chat with an AI version of yourself**

The result is a health experience that feels less like a dashboard and more like a conversation with your future self.

---

## Key Features

<div class="grid cards" markdown>

-   :material-sleep: **Samsung Health Integration**

    ---

    Reads recent and historical wearable data: sleep, steps, heart rate, and stress proxy — directly from Samsung Health.

-   :material-pencil: **Manual Overrides**

    ---

    On the Home screen you can override any metric for today. Wearable data delayed? Missing? Wrong? You stay in control.

-   :material-calendar-week: **7-Day Health View**

    ---

    Browse the last 7 days of health data. Historical snapshots are read-only; today's values support live override.

-   :material-chart-timeline-variant: **Rule-Based Simulation**

    ---

    An offline, explainable simulation engine produces scores for Recovery, Energy, Focus, Strain, and Long-Term Risk — plus what-if scenario comparisons.

-   :material-robot: **AI Twin Chat**

    ---

    Real streaming chat powered by Claude. The AI speaks in a *digital twin* voice built around your anonymized health context and precomputed simulations.

-   :material-barcode-scan: **Food Scanner** *(coming soon)*

    ---

    Placeholder screen for a future Open Food Facts barcode integration.

</div>

---

## Philosophy

MyTwin is not a clinical decision engine. It is a research-driven, interactive prototype for **preventive health reflection**, **behavior change motivation**, and **future-self exploration**.

The digital twin concept becomes compelling in a hackathon demo not by showing data, but by making future consequences feel *personal*.

---

## Quick Links

- [Getting Started](getting-started.md) — build and run the app
- [Architecture Overview](architecture/overview.md) — how it all fits together
- [Simulation Engine](features/simulation.md) — the scoring model explained
- [AI Twin Chat](features/chat.md) — how Claude is prompted and streamed
