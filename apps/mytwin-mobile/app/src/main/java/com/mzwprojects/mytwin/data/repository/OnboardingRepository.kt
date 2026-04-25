package com.mzwprojects.mytwin.data.repository

import com.mzwprojects.mytwin.data.datasource.OnboardingDataSource
import kotlinx.coroutines.flow.Flow

class OnboardingRepository(private val dataSource: OnboardingDataSource) {

    val onboardingCompleted: Flow<Boolean> = dataSource.onboardingCompleted

    suspend fun completeOnboarding() = dataSource.setOnboardingCompleted()
}