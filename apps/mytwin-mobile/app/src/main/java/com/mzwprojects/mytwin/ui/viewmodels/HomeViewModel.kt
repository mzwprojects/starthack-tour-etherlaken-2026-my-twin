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
    val sleepIsManualOverride: Boolean = false,
    val stepsToday: Int? = null,
    val stepsIsFromWearable: Boolean = false,
    val stepsIsManualOverride: Boolean = false,
    val restingHR: Int? = null,
    val heartRateIsManualOverride: Boolean = false,
    val stressLevel: Int? = null,
    val stressIsFromWearable: Boolean = false,
    val stressIsManualOverride: Boolean = false,
    val showWearableNudge: Boolean = false,
    val isLoading: Boolean = true,
) {
    val greetingKey: GreetingTime
        get() = when (LocalTime.now().hour) {
            in 0..11 -> GreetingTime.MORNING
            in 12..17 -> GreetingTime.AFTERNOON
            else -> GreetingTime.EVENING
        }

    val hasSleepOverride: Boolean
        get() = profile.currentSleepHoursOverride != null

    val hasStepsOverride: Boolean
        get() = profile.currentStepsOverride != null

    val hasHeartRateOverride: Boolean
        get() = profile.currentHeartRateOverride != null

    val hasStressOverride: Boolean
        get() = profile.currentStressLevelOverride != null
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
                refreshLiveMetrics()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshSnapshot(_uiState.value.profile)
        }
    }

    fun updateManualSleep(hours: Float) {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentSleepHoursOverride = hours),
                sleepHours = hours,
                sleepIsFromWearable = false,
                sleepIsManualOverride = true,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentSleepHoursOverride = hours) }
        }
    }

    fun updateManualSteps(steps: Int) {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentStepsOverride = steps),
                stepsToday = steps,
                stepsIsFromWearable = false,
                stepsIsManualOverride = true,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentStepsOverride = steps) }
        }
    }

    fun updateManualHeartRate(heartRate: Int) {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentHeartRateOverride = heartRate),
                restingHR = heartRate,
                heartRateIsManualOverride = true,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentHeartRateOverride = heartRate) }
        }
    }

    fun updateManualStress(level: Int) {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentStressLevelOverride = level),
                stressLevel = level,
                stressIsFromWearable = false,
                stressIsManualOverride = true,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentStressLevelOverride = level) }
        }
    }

    fun clearManualSleepOverride() {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentSleepHoursOverride = null),
                sleepIsManualOverride = false,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentSleepHoursOverride = null) }
        }
    }

    fun clearManualStepsOverride() {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentStepsOverride = null),
                stepsIsManualOverride = false,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentStepsOverride = null) }
        }
    }

    fun clearManualHeartRateOverride() {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentHeartRateOverride = null),
                heartRateIsManualOverride = false,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentHeartRateOverride = null) }
        }
    }

    fun clearManualStressOverride() {
        _uiState.update { current ->
            current.copy(
                profile = current.profile.copy(currentStressLevelOverride = null),
                stressIsManualOverride = false,
            )
        }
        viewModelScope.launch {
            userProfileRepository.update { it.copy(currentStressLevelOverride = null) }
        }
    }

    private suspend fun refreshSnapshot(profile: UserProfile) {
        val latestSleep = healthRepository.lastNightSleepHours()
        val latestSteps = healthRepository.stepsToday()
        val wearableHeartRate = healthRepository.latestRestingHeartRate()
        val wearableStress = healthRepository.latestStressLevel()
        val signals = healthRepository.signalsByMetric()

        _uiState.update {
            val displayedSleep = profile.currentSleepHoursOverride ?: latestSleep ?: profile.averageSleepHours
            val displayedSteps = profile.currentStepsOverride ?: latestSteps ?: profile.averageDailySteps
            val displayedHeartRate = profile.currentHeartRateOverride ?: wearableHeartRate
            val displayedStress = profile.currentStressLevelOverride ?: wearableStress ?: profile.perceivedStressLevel
            HomeUiState(
                profile = profile,
                sleepHours = displayedSleep,
                sleepIsFromWearable = profile.currentSleepHoursOverride == null && latestSleep != null,
                sleepIsManualOverride = profile.currentSleepHoursOverride != null,
                stepsToday = displayedSteps,
                stepsIsFromWearable = profile.currentStepsOverride == null && latestSteps != null,
                stepsIsManualOverride = profile.currentStepsOverride != null,
                restingHR = displayedHeartRate,
                heartRateIsManualOverride = profile.currentHeartRateOverride != null,
                stressLevel = displayedStress,
                stressIsFromWearable = profile.currentStressLevelOverride == null && wearableStress != null,
                stressIsManualOverride = profile.currentStressLevelOverride != null,
                showWearableNudge = signals.values.any {
                    it == WearableSignal.DEVICE_PRESENT_NOT_WORN_RECENTLY
                },
                isLoading = false,
            )
        }
    }

    private suspend fun refreshLiveMetrics() {
        val latestSleep = healthRepository.lastNightSleepHours()
        val latestSteps = healthRepository.stepsToday()
        val wearableHeartRate = healthRepository.latestRestingHeartRate()
        val wearableStress = healthRepository.latestStressLevel()

        _uiState.update { current ->
            val profile = current.profile
            val displayedSleep = profile.currentSleepHoursOverride ?: latestSleep ?: current.sleepHours ?: profile.averageSleepHours
            val displayedSteps = profile.currentStepsOverride ?: latestSteps ?: current.stepsToday ?: profile.averageDailySteps
            val displayedHeartRate = profile.currentHeartRateOverride ?: wearableHeartRate ?: current.restingHR
            val displayedStress = profile.currentStressLevelOverride ?: wearableStress ?: current.stressLevel ?: profile.perceivedStressLevel
            current.copy(
                profile = profile,
                sleepHours = displayedSleep,
                sleepIsFromWearable = profile.currentSleepHoursOverride == null && latestSleep != null,
                sleepIsManualOverride = profile.currentSleepHoursOverride != null,
                stepsToday = displayedSteps,
                stepsIsFromWearable = profile.currentStepsOverride == null && latestSteps != null,
                stepsIsManualOverride = profile.currentStepsOverride != null,
                restingHR = displayedHeartRate,
                heartRateIsManualOverride = profile.currentHeartRateOverride != null,
                stressLevel = displayedStress,
                stressIsFromWearable = profile.currentStressLevelOverride == null && wearableStress != null,
                stressIsManualOverride = profile.currentStressLevelOverride != null,
                isLoading = false,
            )
        }
    }
}
