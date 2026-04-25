package com.mzwprojects.mytwin.di

import android.content.Context
import com.mzwprojects.mytwin.data.datasource.OnboardingDataSource
import com.mzwprojects.mytwin.data.datasource.SamsungHealthDataSource
import com.mzwprojects.mytwin.data.datasource.UserProfileDataSource
import com.mzwprojects.mytwin.data.repository.HealthRepository
import com.mzwprojects.mytwin.data.repository.OnboardingRepository
import com.mzwprojects.mytwin.data.repository.UserProfileRepository

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
}
