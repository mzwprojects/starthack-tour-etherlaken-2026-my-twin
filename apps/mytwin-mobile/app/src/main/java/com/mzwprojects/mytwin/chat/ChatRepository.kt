package com.mzwprojects.mytwin.chat

import com.mzwprojects.mytwin.data.repository.HealthRepository
import com.mzwprojects.mytwin.data.repository.UserProfileRepository
import com.mzwprojects.mytwin.simulation.SimulationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val userProfileRepository: UserProfileRepository,
    private val healthRepository: HealthRepository,
    private val simulationRepository: SimulationRepository,
    private val contextBuilder: ClaudeContextBuilder,
    private val apiService: ClaudeApiService,
) {

    suspend fun streamTwinReply(messages: List<ChatMessage>): Flow<String> {
        val profile = userProfileRepository.profile.first()
        val simulation = simulationRepository.buildSimulationBundle()
        val sleepHistory = healthRepository.sleepHistory(days = 7)
        val stepHistory = healthRepository.dailyStepsHistory(days = 7)
        val heartRateHistory = healthRepository.heartRateHistory(hours = 24 * 7)
        val stressHistory = healthRepository.stressHistory(days = 7)
        val snapshot = contextBuilder.build(
            profile = profile,
            simulation = simulation,
            sleepHistory = sleepHistory,
            stepHistory = stepHistory,
            heartRateHistory = heartRateHistory,
            stressHistory = stressHistory,
        )
        return apiService.streamMessage(
            systemPrompt = snapshot.systemPrompt,
            messages = messages,
        )
    }
}
