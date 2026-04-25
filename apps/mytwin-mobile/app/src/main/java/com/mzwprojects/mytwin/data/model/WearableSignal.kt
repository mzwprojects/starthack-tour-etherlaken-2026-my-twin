package com.mzwprojects.mytwin.data.model

/**
 * Represents how confident we are that the user owns a wearable that streams
 * data into Samsung Health for a given metric.
 *
 * Note that "no data" alone is *not* enough to conclude "no wearable". The user
 * may simply not have worn the watch recently. We therefore combine:
 *
 *  - Whether the permission is granted (a strong intent signal).
 *  - Whether *any* historical sample exists for that metric (proves a device
 *    has written data at some point).
 *  - Whether a *recent* sample exists (last ~3 days), to decide whether to
 *    show a "wear your watch" nudge.
 */
enum class WearableSignal {
    /** Permission denied or Samsung Health unavailable — we genuinely don't know. */
    UNKNOWN,

    /** Permission granted but never any data → user likely lacks a wearable. */
    NO_DEVICE_LIKELY,

    /** Has historical data, but nothing in the last few days → "wear it again" nudge. */
    DEVICE_PRESENT_NOT_WORN_RECENTLY,

    /** Recent data present → wearable is active. */
    ACTIVE,
}
