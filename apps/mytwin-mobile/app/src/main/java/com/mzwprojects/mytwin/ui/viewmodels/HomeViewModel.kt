package com.mzwprojects.mytwin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.data.model.UserProfile
import com.mzwprojects.mytwin.data.model.WearableSignal
import com.mzwprojects.mytwin.data.repository.HealthRepository
import com.mzwprojects.mytwin.data.repository.UserProfileRepository
import com.mzwprojects.mytwin.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

data class HomeUiState(
    val profile: UserProfile = UserProfile(),
    val sleepHours: Float? = null,
    val sleepIsFromWearable: Boolean = false,
    val stepsToday: Int? = null,
    val stepsIsFromWearable: Boolean = false,
    val restingHR: Int? = null,
    val stressLevel: Int? = null,
    val showWearableNudge: Boolean = false,
    val isLoading: Boolean = true,
) {
    val greetingKey: GreetingTime get() {
        val hour = LocalTime.now().hour
        return when {
            hour < 12 -> GreetingTime.MORNING
            hour < 18 -> GreetingTime.AFTERNOON
            else -> GreetingTime.EVENING
        }
    }
}

enum class GreetingTime { MORNING, AFTERNOON, EVENING }

class HomeViewModel(
    private val userProfileRepository: UserProfileRepository = ServiceLocator.userProfileRepository,
    private val healthRepository: HealthRepository = ServiceLocator.healthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            healthRepository.connectSamsung()
            userProfileRepository.profile.collect { profile ->
                val hcSleep = healthRepository.lastNightSleepHours()
                val hcSteps = healthRepository.stepsToday()
                val hcHR = healthRepository.latestRestingHeartRate()
                val signals = healthRepository.signalsByMetric()

                val showNudge = signals.values.any {
                    it == WearableSignal.DEVICE_PRESENT_NOT_WORN_RECENTLY
                }

                _uiState.update {
                    HomeUiState(
                        profile = profile,
                        sleepHours = hcSleep ?: profile.averageSleepHours,
                        sleepIsFromWearable = hcSleep != null,
                        stepsToday = hcSteps ?: profile.averageDailySteps,
                        stepsIsFromWearable = hcSteps != null,
                        restingHR = hcHR,
                        stressLevel = profile.perceivedStressLevel,
                        showWearableNudge = showNudge,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
