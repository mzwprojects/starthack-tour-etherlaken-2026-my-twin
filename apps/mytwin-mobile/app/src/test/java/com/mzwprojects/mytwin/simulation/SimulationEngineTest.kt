package com.mzwprojects.mytwin.simulation

import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationEngineTest {

    private val engine = SimulationEngine()

    @Test
    fun lowSleepAndHighStressCreateWeakOutlook() {
        val report = engine.evaluate(
            SimulationInput(
                sleepHoursAvg7d = 5.2f,
                stepsAvg7d = 2_800,
                restingHeartRateAvg3d = 86,
                stressAvg7d = 9,
                sleepTrendHours = -0.4f,
                stepsTrendDelta = -1200,
                wearableCoverageDays = 6,
                ageYears = 34,
                smokingStatus = SmokingStatus.OCCASIONAL,
                alcoholDrinksPerWeek = 6,
                dietQuality = DietQuality.AVERAGE,
                basedOnManualFallbacks = false,
            ),
        )

        assertTrue(report.scores.recovery < 50)
        assertTrue(report.scores.strain > 55)
        assertTrue(report.scores.longTermRisk > 45)
        assertTrue(
            report.narrative.recommendations.any {
                it.contains("sleep", ignoreCase = true)
            },
        )
    }

    @Test
    fun moreSleepImprovesRecoveryScenario() {
        val comparison = engine.compare(
            baselineInput = SimulationInput(
                sleepHoursAvg7d = 5.8f,
                stepsAvg7d = 6_500,
                restingHeartRateAvg3d = 78,
                stressAvg7d = 7,
                sleepTrendHours = 0f,
                stepsTrendDelta = 0,
                wearableCoverageDays = 5,
                ageYears = 29,
                smokingStatus = SmokingStatus.NEVER,
                alcoholDrinksPerWeek = 2,
                dietQuality = DietQuality.GOOD,
                basedOnManualFallbacks = false,
            ),
            scenario = SimulationScenario.SLEEP_PLUS_ONE_HOUR,
        )

        assertTrue(comparison.delta.recovery > 0)
        assertTrue(comparison.delta.energy > 0)
        assertTrue(comparison.summary.contains("recovery", ignoreCase = true))
    }

    @Test
    fun manualFallbacksLowerConfidence() {
        val report = engine.evaluate(
            SimulationInput(
                sleepHoursAvg7d = 7f,
                stepsAvg7d = 7_000,
                restingHeartRateAvg3d = null,
                stressAvg7d = 5,
                sleepTrendHours = 0f,
                stepsTrendDelta = 0,
                wearableCoverageDays = 1,
                ageYears = 40,
                smokingStatus = SmokingStatus.NEVER,
                alcoholDrinksPerWeek = 1,
                dietQuality = DietQuality.GOOD,
                basedOnManualFallbacks = true,
            ),
        )

        assertEquals(SimulationConfidence.LOW, report.confidence)
    }
}
