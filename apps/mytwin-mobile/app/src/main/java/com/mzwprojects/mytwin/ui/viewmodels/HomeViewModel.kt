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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
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
    val stressIsFromWearable: Boolean = false,
    val showWearableNudge: Boolean = false,
    val isLoading: Boolean = true,
) {
    val greetingKey: GreetingTime
        get() = when (LocalTime.now().hour) {
            in 0..11 -> GreetingTime.MORNING
            in 12..17 -> GreetingTime.AFTERNOON
            else -> GreetingTime.EVENING
        }

    val canEditSleepManually: Boolean
        get() = !sleepIsFromWearable

    val canEditStepsManually: Boolean
        get() = !stepsIsFromWearable

    val canEditStressManually: Boolean
        get() = !stressIsFromWearable
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
                refreshSnapshot(profile)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(2_000)
                refreshLiveMetrics(_uiState.value.profile)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshSnapshot(_uiState.value.profile)
        }
    }

    fun updateManualSleep(hours: Float) {
        viewModelScope.launch {
            userProfileRepository.update { it.copy(averageSleepHours = hours) }
        }
    }

    fun updateManualSteps(steps: Int) {
        viewModelScope.launch {
            userProfileRepository.update { it.copy(averageDailySteps = steps) }
        }
    }

    fun updateManualStress(level: Int) {
        viewModelScope.launch {
            userProfileRepository.update { it.copy(perceivedStressLevel = level) }
        }
    }

    private suspend fun refreshSnapshot(profile: UserProfile) {
        val latestSleep = healthRepository.lastNightSleepHours()
        val latestSteps = healthRepository.stepsToday()
        val wearableHeartRate = healthRepository.latestRestingHeartRate()
        val wearableStress = healthRepository.latestStressLevel()
        val signals = healthRepository.signalsByMetric()

        _uiState.update {
            HomeUiState(
                profile = profile,
                sleepHours = latestSleep ?: profile.averageSleepHours,
                sleepIsFromWearable = latestSleep != null,
                stepsToday = latestSteps ?: profile.averageDailySteps,
                stepsIsFromWearable = latestSteps != null,
                restingHR = wearableHeartRate,
                stressLevel = wearableStress ?: profile.perceivedStressLevel,
                stressIsFromWearable = wearableStress != null,
                showWearableNudge = signals.values.any {
                    it == WearableSignal.DEVICE_PRESENT_NOT_WORN_RECENTLY
                },
                isLoading = false,
            )
        }
    }

    private suspend fun refreshLiveMetrics(profile: UserProfile) {
        val latestSleep = healthRepository.lastNightSleepHours()
        val latestSteps = healthRepository.stepsToday()
        val wearableHeartRate = healthRepository.latestRestingHeartRate()
        val wearableStress = healthRepository.latestStressLevel()

        _uiState.update { current ->
            current.copy(
                profile = profile,
                sleepHours = latestSleep ?: current.sleepHours ?: profile.averageSleepHours,
                sleepIsFromWearable = latestSleep != null || current.sleepIsFromWearable,
                stepsToday = latestSteps ?: current.stepsToday ?: profile.averageDailySteps,
                stepsIsFromWearable = latestSteps != null || current.stepsIsFromWearable,
                restingHR = wearableHeartRate ?: current.restingHR,
                stressLevel = wearableStress ?: current.stressLevel ?: profile.perceivedStressLevel,
                stressIsFromWearable = wearableStress != null || current.stressIsFromWearable,
                isLoading = false,
            )
        }
    }
}
