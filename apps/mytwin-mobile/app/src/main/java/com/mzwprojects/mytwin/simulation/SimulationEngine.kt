package com.mzwprojects.mytwin.simulation

import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus

class SimulationEngine {

    fun evaluate(input: SimulationInput): SimulationReport {
        val scores = score(input)
        val confidence = confidence(input)
        val narrative = buildNarrative(input, scores, confidence)
        return SimulationReport(
            input = input,
            scores = scores,
            confidence = confidence,
            narrative = narrative,
        )
    }

    fun compare(
        baselineInput: SimulationInput,
        scenario: SimulationScenario,
    ): SimulationScenarioComparison {
        val baseline = evaluate(baselineInput)
        val projected = evaluate(applyAdjustment(baselineInput, scenario.adjustment))
        val delta = SimulationDelta(
            recovery = projected.scores.recovery - baseline.scores.recovery,
            energy = projected.scores.energy - baseline.scores.energy,
            focus = projected.scores.focus - baseline.scores.focus,
            strain = projected.scores.strain - baseline.scores.strain,
            longTermRisk = projected.scores.longTermRisk - baseline.scores.longTermRisk,
        )
        return SimulationScenarioComparison(
            scenario = scenario,
            baseline = baseline,
            projected = projected,
            delta = delta,
            summary = buildScenarioSummary(scenario, delta),
        )
    }

    private fun score(input: SimulationInput): SimulationScores {
        val sleep = input.sleepHoursAvg7d
        val steps = input.stepsAvg7d
        val heartRate = input.restingHeartRateAvg3d
        val stress = input.stressAvg7d

        val recovery = clamp(
            55 +
                sleepRecoveryContribution(sleep) +
                stressRecoveryContribution(stress) +
                heartRateRecoveryContribution(heartRate) +
                trendRecoveryContribution(input.sleepTrendHours) +
                smokingRecoveryContribution(input.smokingStatus),
        )

        val energy = clamp(
            50 +
                sleepEnergyContribution(sleep) +
                stepsEnergyContribution(steps) +
                stressEnergyContribution(stress) +
                trendEnergyContribution(input.stepsTrendDelta),
        )

        val focus = clamp(
            52 +
                sleepFocusContribution(sleep) +
                stressFocusContribution(stress) +
                heartRateFocusContribution(heartRate) +
                dietFocusContribution(input.dietQuality),
        )

        val strain = clamp(
            40 +
                stressStrainContribution(stress) +
                heartRateStrainContribution(heartRate) -
                sleepStrainContribution(sleep) -
                stepsStrainContribution(steps),
        )

        val longTermRisk = clamp(
            35 +
                riskSleepContribution(sleep) +
                riskStepsContribution(steps) +
                riskStressContribution(stress) +
                riskHeartRateContribution(heartRate) +
                riskAgeContribution(input.ageYears) +
                riskSmokingContribution(input.smokingStatus) +
                riskAlcoholContribution(input.alcoholDrinksPerWeek) +
                riskDietContribution(input.dietQuality) -
                riskPositiveTrendContribution(input.sleepTrendHours, input.stepsTrendDelta),
        )

        return SimulationScores(
            recovery = recovery,
            energy = energy,
            focus = focus,
            strain = strain,
            longTermRisk = longTermRisk,
        )
    }

    private fun confidence(input: SimulationInput): SimulationConfidence {
        val score = input.wearableCoverageDays * 12 - if (input.basedOnManualFallbacks) 25 else 0
        return when {
            score >= 55 -> SimulationConfidence.HIGH
            score >= 25 -> SimulationConfidence.MEDIUM
            else -> SimulationConfidence.LOW
        }
    }

    private fun buildNarrative(
        input: SimulationInput,
        scores: SimulationScores,
        confidence: SimulationConfidence,
    ): SimulationNarrative {
        val headline = when {
            scores.recovery >= 70 && scores.energy >= 70 && scores.strain <= 45 ->
                "Your current pattern looks sustainable over the next few days."
            scores.recovery <= 45 || scores.strain >= 65 ->
                "Your recent pattern points to mounting strain unless something changes."
            else ->
                "Your next few days look manageable, but there is room to improve recovery."
        }

        val shortTermInsights = buildList {
            when {
                input.sleepHoursAvg7d < 6f ->
                    add("If sleep stays this low, fatigue and mood dips are likely to show up within days.")
                input.sleepHoursAvg7d >= 7f ->
                    add("Your recent sleep pattern gives you a decent recovery base for the next few days.")
            }
            when {
                input.stressAvg7d != null && input.stressAvg7d >= 8 ->
                    add("Stress is the strongest short-term drag right now and will likely blunt energy even on better days.")
                input.stressAvg7d != null && input.stressAvg7d <= 4 ->
                    add("Lower recent stress should help you stay more stable across the week.")
            }
            when {
                input.stepsAvg7d < 4_000 ->
                    add("Low recent activity points to flatter energy and less resilience over the next few days.")
                input.stepsAvg7d >= 8_000 ->
                    add("Your movement level supports steadier energy and a healthier short-term trend.")
            }
        }

        val recommendations = buildList {
            if (input.sleepHoursAvg7d < 6.5f) add("Protect sleep first: adding even 45 to 60 minutes should lift recovery noticeably.")
            if (input.stepsAvg7d < 7_000) add("Aim for one extra walk or commute block each day to raise your activity baseline.")
            if ((input.stressAvg7d ?: 5) >= 7) add("Reduce strain input: fewer late stressors or a calmer evening routine would likely help quickly.")
            if ((input.restingHeartRateAvg3d ?: 70) >= 82) add("Treat the elevated heart-rate trend as a sign to prioritize recovery over intensity for a few days.")
            if (isEmpty()) add("Keep the current routine steady and watch for consistency rather than large changes.")
        }

        val longTermSignals = buildList {
            when {
                scores.longTermRisk >= 65 ->
                    add("If this pattern continues for weeks, it points toward a less favorable long-term wellbeing trajectory.")
                scores.longTermRisk <= 40 ->
                    add("If you keep this pattern stable, the long-term direction looks broadly positive.")
            }
            if (input.sleepTrendHours > 0.4f) add("Sleep is already trending upward, which is one of the best signs for long-term improvement.")
            if (input.stepsTrendDelta > 1_500) add("Your activity trend is improving, which should compound positively if maintained.")
            if (input.basedOnManualFallbacks) add("Some inputs rely on fallback data, so the long-term projection should be treated as directional rather than precise.")
            when (confidence) {
                SimulationConfidence.HIGH -> add("This projection is based on a reasonably strong week of data.")
                SimulationConfidence.MEDIUM -> add("This projection is useful, but more wearable coverage would make it sharper.")
                SimulationConfidence.LOW -> add("This projection is intentionally conservative because recent data coverage is limited.")
            }
        }

        return SimulationNarrative(
            headline = headline,
            shortTermInsights = shortTermInsights,
            recommendations = recommendations,
            longTermSignals = longTermSignals,
        )
    }

    private fun applyAdjustment(
        input: SimulationInput,
        adjustment: SimulationAdjustment,
    ): SimulationInput {
        return input.copy(
            sleepHoursAvg7d = (input.sleepHoursAvg7d + adjustment.sleepHoursDelta).coerceIn(3.5f, 10.5f),
            stepsAvg7d = (input.stepsAvg7d + adjustment.stepsDelta).coerceIn(0, 30_000),
            stressAvg7d = input.stressAvg7d?.let { (it + adjustment.stressDelta).coerceIn(1, 10) },
            restingHeartRateAvg3d = input.restingHeartRateAvg3d?.let {
                (it + adjustment.restingHeartRateDelta).coerceIn(40, 120)
            },
        )
    }

    private fun buildScenarioSummary(
        scenario: SimulationScenario,
        delta: SimulationDelta,
    ): String {
        val parts = buildList {
            if (delta.recovery > 0) add("recovery +${delta.recovery}")
            if (delta.energy > 0) add("energy +${delta.energy}")
            if (delta.focus > 0) add("focus +${delta.focus}")
            if (delta.strain < 0) add("strain ${delta.strain}")
            if (delta.longTermRisk < 0) add("long-term risk ${delta.longTermRisk}")
        }

        return if (parts.isEmpty()) {
            "${scenario.title} changes the outlook only slightly."
        } else {
            "${scenario.title} would likely shift ${parts.joinToString(", ")}."
        }
    }

    private fun sleepRecoveryContribution(hours: Float): Int = when {
        hours < 5f -> -28
        hours < 6f -> -18
        hours < 7f -> -8
        hours <= 8.5f -> 16
        hours <= 9.5f -> 10
        else -> 0
    }

    private fun stressRecoveryContribution(stress: Int?): Int = when {
        stress == null -> 0
        stress >= 9 -> -22
        stress >= 7 -> -14
        stress >= 5 -> -6
        stress <= 3 -> 10
        else -> 4
    }

    private fun heartRateRecoveryContribution(heartRate: Int?): Int = when {
        heartRate == null -> 0
        heartRate >= 85 -> -12
        heartRate >= 78 -> -6
        heartRate <= 60 -> 6
        else -> 2
    }

    private fun trendRecoveryContribution(sleepTrend: Float): Int = when {
        sleepTrend >= 0.6f -> 8
        sleepTrend >= 0.2f -> 4
        sleepTrend <= -0.6f -> -8
        sleepTrend <= -0.2f -> -4
        else -> 0
    }

    private fun smokingRecoveryContribution(smokingStatus: SmokingStatus?): Int = when (smokingStatus) {
        SmokingStatus.REGULAR -> -8
        SmokingStatus.OCCASIONAL -> -4
        else -> 0
    }

    private fun sleepEnergyContribution(hours: Float): Int = when {
        hours < 5f -> -24
        hours < 6.5f -> -12
        hours <= 8.5f -> 12
        else -> 4
    }

    private fun stepsEnergyContribution(steps: Int): Int = when {
        steps < 3_000 -> -18
        steps < 5_000 -> -10
        steps < 7_000 -> -4
        steps <= 11_000 -> 12
        steps <= 15_000 -> 8
        else -> 4
    }

    private fun stressEnergyContribution(stress: Int?): Int = when {
        stress == null -> 0
        stress >= 8 -> -18
        stress >= 6 -> -10
        stress <= 3 -> 8
        else -> 0
    }

    private fun trendEnergyContribution(stepsTrendDelta: Int): Int = when {
        stepsTrendDelta >= 2_000 -> 8
        stepsTrendDelta >= 800 -> 4
        stepsTrendDelta <= -2_000 -> -8
        stepsTrendDelta <= -800 -> -4
        else -> 0
    }

    private fun sleepFocusContribution(hours: Float): Int = when {
        hours < 5.5f -> -18
        hours < 6.5f -> -10
        hours <= 8.5f -> 12
        else -> 2
    }

    private fun stressFocusContribution(stress: Int?): Int = when {
        stress == null -> 0
        stress >= 8 -> -20
        stress >= 6 -> -10
        stress <= 3 -> 6
        else -> 0
    }

    private fun heartRateFocusContribution(heartRate: Int?): Int = when {
        heartRate == null -> 0
        heartRate >= 85 -> -8
        heartRate <= 62 -> 4
        else -> 0
    }

    private fun dietFocusContribution(dietQuality: DietQuality?): Int = when (dietQuality) {
        DietQuality.EXCELLENT -> 8
        DietQuality.GOOD -> 4
        DietQuality.POOR -> -6
        else -> 0
    }

    private fun stressStrainContribution(stress: Int?): Int = when {
        stress == null -> 0
        stress >= 9 -> 28
        stress >= 7 -> 18
        stress >= 5 -> 8
        else -> -4
    }

    private fun heartRateStrainContribution(heartRate: Int?): Int = when {
        heartRate == null -> 0
        heartRate >= 88 -> 16
        heartRate >= 80 -> 8
        else -> 0
    }

    private fun sleepStrainContribution(hours: Float): Int = when {
        hours < 5.5f -> 0
        hours < 7f -> 4
        hours <= 8.5f -> 12
        else -> 8
    }

    private fun stepsStrainContribution(steps: Int): Int = when {
        steps < 4_000 -> 0
        steps < 7_000 -> 4
        steps <= 11_000 -> 10
        else -> 8
    }

    private fun riskSleepContribution(hours: Float): Int = when {
        hours < 5f -> 22
        hours < 6f -> 14
        hours < 7f -> 6
        hours <= 8.5f -> -6
        else -> 0
    }

    private fun riskStepsContribution(steps: Int): Int = when {
        steps < 3_000 -> 18
        steps < 5_000 -> 10
        steps < 7_000 -> 4
        steps >= 9_000 -> -8
        else -> 0
    }

    private fun riskStressContribution(stress: Int?): Int = when {
        stress == null -> 0
        stress >= 8 -> 18
        stress >= 6 -> 10
        stress <= 3 -> -6
        else -> 0
    }

    private fun riskHeartRateContribution(heartRate: Int?): Int = when {
        heartRate == null -> 0
        heartRate >= 85 -> 12
        heartRate >= 78 -> 6
        heartRate <= 60 -> -4
        else -> 0
    }

    private fun riskAgeContribution(age: Int?): Int = when {
        age == null -> 0
        age >= 60 -> 8
        age >= 45 -> 4
        else -> 0
    }

    private fun riskSmokingContribution(smokingStatus: SmokingStatus?): Int = when (smokingStatus) {
        SmokingStatus.REGULAR -> 20
        SmokingStatus.OCCASIONAL -> 10
        SmokingStatus.FORMER -> 4
        else -> 0
    }

    private fun riskAlcoholContribution(alcohol: Int?): Int = when {
        alcohol == null -> 0
        alcohol >= 15 -> 10
        alcohol >= 8 -> 5
        else -> 0
    }

    private fun riskDietContribution(dietQuality: DietQuality?): Int = when (dietQuality) {
        DietQuality.POOR -> 10
        DietQuality.AVERAGE -> 4
        DietQuality.GOOD -> -3
        DietQuality.EXCELLENT -> -6
        null -> 0
    }

    private fun riskPositiveTrendContribution(sleepTrend: Float, stepsTrendDelta: Int): Int {
        var bonus = 0
        if (sleepTrend >= 0.4f) bonus += 4
        if (stepsTrendDelta >= 1_500) bonus += 4
        return bonus
    }

    private fun clamp(value: Int): Int = value.coerceIn(0, 100)
}
