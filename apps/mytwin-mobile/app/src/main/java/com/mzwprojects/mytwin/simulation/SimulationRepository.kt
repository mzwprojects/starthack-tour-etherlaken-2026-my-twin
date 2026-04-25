package com.mzwprojects.mytwin.simulation

import com.mzwprojects.mytwin.data.model.UserProfile
import com.mzwprojects.mytwin.data.model.WearableHistoryPoint
import com.mzwprojects.mytwin.data.repository.HealthRepository
import com.mzwprojects.mytwin.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class SimulationRepository(
    private val userProfileRepository: UserProfileRepository,
    private val healthRepository: HealthRepository,
    private val simulationEngine: SimulationEngine,
) {

    suspend fun buildSimulationBundle(
        scenarios: List<SimulationScenario> = SimulationScenario.entries,
    ): SimulationBundle {
        val baselineInput = buildSimulationInput()
        val baseline = simulationEngine.evaluate(baselineInput)
        val scenarioComparisons = scenarios.map { scenario ->
            simulationEngine.compare(baselineInput, scenario)
        }
        return SimulationBundle(
            baseline = baseline,
            scenarioComparisons = scenarioComparisons,
        )
    }

    suspend fun buildSimulationInput(): SimulationInput {
        val profile = userProfileRepository.profile.first()
        val sleepHistory = healthRepository.sleepHistory(days = 7)
        val stepsHistory = healthRepository.dailyStepsHistory(days = 7)
        val heartRateHistory = healthRepository.heartRateHistory(hours = 24 * 7)
        val stressHistory = healthRepository.stressHistory(days = 7)

        val sleepByDay = sleepHistory.groupLatestFloatByDay()
        val stepByDay = stepsHistory.groupLatestIntByDay()
        val stressByDay = stressHistory.groupAverageIntByDay()
        val heartRateByDay = heartRateHistory.groupAverageFloatByDay()

        val wearableCoverageDays = listOf(
            sleepByDay.size,
            stepByDay.size,
            stressByDay.size,
            heartRateByDay.size,
        ).maxOrNull() ?: 0

        val sleepAvg = sleepByDay.values.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            ?: profile.averageSleepHours
            ?: 7f
        val stepsAvg = stepByDay.values.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
            ?: profile.averageDailySteps
            ?: 7_000
        val heartRateAvg3d = heartRateHistory
            .filter { it.timestamp.toLocalDate() >= LocalDate.now().minusDays(2) }
            .map { it.value }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
        val stressAvg = stressByDay.values.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
            ?: profile.perceivedStressLevel

        val basedOnManualFallbacks =
            sleepByDay.isEmpty() || stepByDay.isEmpty() || (stressByDay.isEmpty() && profile.perceivedStressLevel != null)

        return SimulationInput(
            sleepHoursAvg7d = sleepAvg,
            stepsAvg7d = stepsAvg,
            restingHeartRateAvg3d = heartRateAvg3d,
            stressAvg7d = stressAvg,
            sleepTrendHours = sleepByDay.sortedFloatTrend(),
            stepsTrendDelta = stepByDay.sortedIntTrend().roundToInt(),
            wearableCoverageDays = wearableCoverageDays,
            ageYears = profile.ageYears,
            smokingStatus = profile.smokingStatus,
            alcoholDrinksPerWeek = profile.alcoholDrinksPerWeek,
            dietQuality = profile.dietQuality,
            basedOnManualFallbacks = basedOnManualFallbacks,
        )
    }

    private fun List<WearableHistoryPoint<Float>>.groupLatestFloatByDay(): Map<LocalDate, Float> =
        groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, points) -> points.maxByOrNull { it.timestamp }?.value ?: return@mapValues 0f }

    private fun List<WearableHistoryPoint<Int>>.groupLatestIntByDay(): Map<LocalDate, Int> =
        groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, points) -> points.maxByOrNull { it.timestamp }?.value ?: return@mapValues 0 }

    private fun List<WearableHistoryPoint<Float>>.groupAverageFloatByDay(): Map<LocalDate, Float> =
        groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, points) -> points.map { it.value }.average().toFloat() }

    private fun List<WearableHistoryPoint<Int>>.groupAverageIntByDay(): Map<LocalDate, Int> =
        groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, points) -> points.map { it.value }.average().roundToInt() }

    private fun Map<LocalDate, Float>.sortedFloatTrend(): Float {
        val values = entries.sortedBy { it.key }.map { it.value }
        return if (values.size < 2) 0f else values.last() - values.first()
    }

    private fun Map<LocalDate, Int>.sortedIntTrend(): Float {
        val values = entries.sortedBy { it.key }.map { it.value.toFloat() }
        return if (values.size < 2) 0f else values.last() - values.first()
    }

    private fun Instant.toLocalDate(): LocalDate =
        atZone(ZoneId.systemDefault()).toLocalDate()
}
