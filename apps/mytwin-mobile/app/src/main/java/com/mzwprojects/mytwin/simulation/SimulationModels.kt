package com.mzwprojects.mytwin.simulation

import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus

data class SimulationInput(
    val sleepHoursAvg7d: Float,
    val stepsAvg7d: Int,
    val restingHeartRateAvg3d: Int?,
    val stressAvg7d: Int?,
    val sleepTrendHours: Float,
    val stepsTrendDelta: Int,
    val wearableCoverageDays: Int,
    val ageYears: Int?,
    val smokingStatus: SmokingStatus?,
    val alcoholDrinksPerWeek: Int?,
    val dietQuality: DietQuality?,
    val basedOnManualFallbacks: Boolean,
)

data class SimulationScores(
    val recovery: Int,
    val energy: Int,
    val focus: Int,
    val strain: Int,
    val longTermRisk: Int,
)

data class SimulationNarrative(
    val headline: String,
    val shortTermInsights: List<String>,
    val recommendations: List<String>,
    val longTermSignals: List<String>,
)

enum class SimulationConfidence {
    LOW,
    MEDIUM,
    HIGH,
}

data class SimulationReport(
    val input: SimulationInput,
    val scores: SimulationScores,
    val confidence: SimulationConfidence,
    val narrative: SimulationNarrative,
)

data class SimulationAdjustment(
    val sleepHoursDelta: Float = 0f,
    val stepsDelta: Int = 0,
    val stressDelta: Int = 0,
    val restingHeartRateDelta: Int = 0,
)

enum class SimulationScenario(
    val title: String,
    val description: String,
    val adjustment: SimulationAdjustment,
) {
    SLEEP_PLUS_ONE_HOUR(
        title = "Sleep +1h",
        description = "Add roughly one hour of sleep to the next few nights.",
        adjustment = SimulationAdjustment(sleepHoursDelta = 1f),
    ),
    SLEEP_MINUS_ONE_HOUR(
        title = "Sleep -1h",
        description = "Model what happens if sleep drops by about an hour.",
        adjustment = SimulationAdjustment(sleepHoursDelta = -1f),
    ),
    STEPS_PLUS_3000(
        title = "Steps +3000",
        description = "Add a meaningful activity bump to the daily routine.",
        adjustment = SimulationAdjustment(stepsDelta = 3_000),
    ),
    STRESS_MINUS_TWO(
        title = "Stress -2",
        description = "Reduce perceived stress by roughly two points.",
        adjustment = SimulationAdjustment(stressDelta = -2),
    ),
    RECOVERY_PUSH(
        title = "Recovery Push",
        description = "Sleep a bit more, move more, and lower stress together.",
        adjustment = SimulationAdjustment(
            sleepHoursDelta = 0.8f,
            stepsDelta = 2_500,
            stressDelta = -2,
            restingHeartRateDelta = -2,
        ),
    ),
}

data class SimulationDelta(
    val recovery: Int,
    val energy: Int,
    val focus: Int,
    val strain: Int,
    val longTermRisk: Int,
)

data class SimulationScenarioComparison(
    val scenario: SimulationScenario,
    val baseline: SimulationReport,
    val projected: SimulationReport,
    val delta: SimulationDelta,
    val summary: String,
)

data class SimulationBundle(
    val baseline: SimulationReport,
    val scenarioComparisons: List<SimulationScenarioComparison>,
)
