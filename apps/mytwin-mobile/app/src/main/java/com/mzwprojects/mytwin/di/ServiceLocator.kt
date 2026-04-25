package com.mzwprojects.mytwin.di

import android.content.Context
import com.google.gson.Gson
import com.mzwprojects.mytwin.chat.ChatRepository
import com.mzwprojects.mytwin.chat.ClaudeApiService
import com.mzwprojects.mytwin.chat.ClaudeContextBuilder
import com.mzwprojects.mytwin.data.datasource.OnboardingDataSource
import com.mzwprojects.mytwin.data.datasource.SamsungHealthDataSource
import com.mzwprojects.mytwin.data.datasource.UserProfileDataSource
import com.mzwprojects.mytwin.data.repository.HealthRepository
import com.mzwprojects.mytwin.data.repository.OnboardingRepository
import com.mzwprojects.mytwin.data.repository.UserProfileRepository
import com.mzwprojects.mytwin.simulation.SimulationEngine
import com.mzwprojects.mytwin.simulation.SimulationRepository

/**
 * Lightweight manual dependency container.
 * Initialised once in [com.mzwprojects.mytwin.MyTwinApplication.onCreate].
 */
object ServiceLocator {

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context = checkNotNull(appContext) {
        "ServiceLocator not initialised. Did MyTwinApplication.onCreate() run?"
    }

    // ─── Data sources ────────────────────────────────────────────────────
    val onboardingDataSource: OnboardingDataSource by lazy {
        OnboardingDataSource(requireContext())
    }

    val userProfileDataSource: UserProfileDataSource by lazy {
        UserProfileDataSource(requireContext())
    }

    val samsungHealthDataSource: SamsungHealthDataSource by lazy {
        SamsungHealthDataSource(requireContext())
    }

    val gson: Gson by lazy {
        Gson()
    }

    // ─── Repositories ────────────────────────────────────────────────────
    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingRepository(onboardingDataSource)
    }

    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(userProfileDataSource)
    }

    val healthRepository: HealthRepository by lazy {
        HealthRepository(samsungHealthDataSource)
    }

    val simulationEngine: SimulationEngine by lazy {
        SimulationEngine()
    }

    val simulationRepository: SimulationRepository by lazy {
        SimulationRepository(
            userProfileRepository,
            healthRepository,
            simulationEngine,
        )
    }

    val claudeContextBuilder: ClaudeContextBuilder by lazy {
        ClaudeContextBuilder()
    }

    val claudeApiService: ClaudeApiService by lazy {
        ClaudeApiService(gson)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            userProfileRepository = userProfileRepository,
            healthRepository = healthRepository,
            simulationRepository = simulationRepository,
            contextBuilder = claudeContextBuilder,
            apiService = claudeApiService,
        )
    }
}
