package com.mzwprojects.mytwin.data.model

import kotlinx.serialization.Serializable

/**
 * The full set of profile data we collect during onboarding.
 *
 * All numeric fields are nullable. `null` means "not yet provided" — never
 * conflate `null` with `0`. Concrete defaults live in the UI layer.
 *
 * Manual entries (e.g. [averageSleepHours]) are only used when the corresponding
 * Samsung Health metric is *not* [WearableSignal.ACTIVE].
 */
@Serializable
data class UserProfile(
    // ─── Identity ────────────────────────────────────────────────────────
    val displayName: String? = null,
    val ageYears: Int? = null,
    val biologicalSex: BiologicalSex? = null,
    val heightCm: Int? = null,
    val weightKg: Float? = null,

    // ─── Manual lifestyle entries (used only when no wearable data) ──────
    val averageSleepHours: Float? = null,
    val averageDailySteps: Int? = null,
    val perceivedStressLevel: Int? = null, // 1..10

    // ─── Habits (always manual) ──────────────────────────────────────────
    val smokingStatus: SmokingStatus? = null,
    val alcoholDrinksPerWeek: Int? = null,
    val dietQuality: DietQuality? = null,

    // ─── Bookkeeping ─────────────────────────────────────────────────────
    /** Marker so onboarding never re-runs once submitted. */
    val onboardingCompletedAtEpochMs: Long? = null,
)

@Serializable
enum class BiologicalSex { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }

@Serializable
enum class SmokingStatus { NEVER, FORMER, OCCASIONAL, REGULAR }

@Serializable
enum class DietQuality { POOR, AVERAGE, GOOD, EXCELLENT }
