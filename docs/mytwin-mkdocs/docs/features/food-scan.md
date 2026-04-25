# Food Scanner

The Food Scanner is a **placeholder screen** that signals a planned future feature — not yet implemented.

---

## Current State

`FoodScanScreen` is a stub composable accessible from the Home screen via the **"Scan Food (soon)"** button (which carries a "coming soon" badge).

The screen shows a back button and a placeholder message. No camera permission is requested and no scanning logic exists yet.

---

## Planned Integration

The intended implementation is **Open Food Facts** barcode scanning:

- Camera opens via `CameraX` or `ML Kit BarcodeScanner`
- Barcode → Open Food Facts product lookup (local or API)
- Nutritional data attached to the current day's profile
- Diet quality and caloric context fed into simulation and chat

---

## Why Include It Now?

Showing the entry point in the demo makes the *scope of the product vision* tangible — even features not yet built are part of the story being told. The "coming soon" badge is intentional and honest.
