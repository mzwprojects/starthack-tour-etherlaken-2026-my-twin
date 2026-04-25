package com.mzwprojects.mytwin.chat

import com.mzwprojects.mytwin.data.model.UserProfile
import com.mzwprojects.mytwin.data.model.WearableHistoryPoint
import com.mzwprojects.mytwin.simulation.SimulationBundle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ClaudeContextBuilder {

    fun build(
        profile: UserProfile,
        simulation: SimulationBundle,
        sleepHistory: List<WearableHistoryPoint<Float>>,
        stepHistory: List<WearableHistoryPoint<Int>>,
        heartRateHistory: List<WearableHistoryPoint<Float>>,
        stressHistory: List<WearableHistoryPoint<Int>>,
    ): ClaudeContextSnapshot {
        return ClaudeContextSnapshot(
            systemPrompt =
                """
                You are MyTwin, the user's more insightful digital twin.
                Speak like an intelligent future version of the user, not like a generic AI assistant.
                Sound grounded, calm, practical, and a bit personal without becoming theatrical.
                Keep answers concise by default. Usually answer in 2 to 5 short paragraphs or compact bullet points.
                Use the anonymized health and lifestyle context below. Do not mention that the user's name is unavailable.
                Do not invent measurements that are not in the context. When something is uncertain, say so plainly.
                Focus on:
                - how the user is likely to feel in the next days
                - what matters most to improve right now
                - what patterns look positive or risky if continued
                - answering what-if style questions clearly
                This is a coaching-style wellbeing projection, not a diagnosis. If a question sounds urgent or medical, say professional care is appropriate.

                USER CONTEXT
                Profile:
                - ageYears: ${profile.ageYears ?: "unknown"}
                - biologicalSex: ${profile.biologicalSex ?: "unknown"}
                - heightCm: ${profile.heightCm ?: "unknown"}
                - weightKg: ${profile.weightKg ?: "unknown"}
                - smokingStatus: ${profile.smokingStatus ?: "unknown"}
                - alcoholDrinksPerWeek: ${profile.alcoholDrinksPerWeek ?: "unknown"}
                - dietQuality: ${profile.dietQuality ?: "unknown"}

                Current baselines:
                - averageSleepHours: ${profile.averageSleepHours ?: "unknown"}
                - averageDailySteps: ${profile.averageDailySteps ?: "unknown"}
                - perceivedStressLevel: ${profile.perceivedStressLevel ?: "unknown"}

                Simulation baseline:
                - confidence: ${simulation.baseline.confidence}
                - recovery: ${simulation.baseline.scores.recovery}
                - energy: ${simulation.baseline.scores.energy}
                - focus: ${simulation.baseline.scores.focus}
                - strain: ${simulation.baseline.scores.strain}
                - longTermRisk: ${simulation.baseline.scores.longTermRisk}
                - headline: ${simulation.baseline.narrative.headline}
                - shortTermInsights: ${simulation.baseline.narrative.shortTermInsights.joinToString(" | ")}
                - recommendations: ${simulation.baseline.narrative.recommendations.joinToString(" | ")}
                - longTermSignals: ${simulation.baseline.narrative.longTermSignals.joinToString(" | ")}

                Recent wearable history:
                - sleepHoursByDay: ${sleepHistory.toDailySummary()}
                - stepsByDay: ${stepHistory.toDailySummary()}
                - heartRateByDay: ${heartRateHistory.toDailySummary()}
                - stressByDay: ${stressHistory.toDailySummary()}

                Precomputed what-if scenarios:
                ${simulation.scenarioComparisons.joinToString("\n") { scenario ->
                    "- ${scenario.scenario.title}: ${scenario.summary} Headline: ${scenario.projected.narrative.headline}"
                }}
                """.trimIndent(),
        )
    }

    private fun <T> List<WearableHistoryPoint<T>>.toDailySummary(): String {
        if (isEmpty()) return "no recent data"

        return groupBy { it.timestamp.toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .take(7)
            .joinToString(" | ") { (date, points) ->
                val values = points.joinToString(",") { it.value.toString() }
                "$date:$values"
            }
    }

    private fun Instant.toLocalDate(): LocalDate =
        atZone(ZoneId.systemDefault()).toLocalDate()
}
