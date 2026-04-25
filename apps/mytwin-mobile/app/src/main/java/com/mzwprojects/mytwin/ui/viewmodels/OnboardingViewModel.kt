package com.mzwprojects.mytwin.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.data.datasource.SamsungHealthMetric
import com.mzwprojects.mytwin.data.model.BiologicalSex
import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus
import com.mzwprojects.mytwin.data.model.UserProfile
import com.mzwprojects.mytwin.data.model.WearableSignal
import com.mzwprojects.mytwin.data.repository.HealthRepository
import com.mzwprojects.mytwin.data.repository.OnboardingRepository
import com.mzwprojects.mytwin.data.repository.UserProfileRepository
import com.mzwprojects.mytwin.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { PROFILE, PERMISSIONS, MANUAL_DATA, HABITS, REVIEW }

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.PROFILE,
    val displayName: String = "",
    val ageInput: String = "",
    val biologicalSex: BiologicalSex? = null,
    val heightInput: String = "",
    val weightInput: String = "",
    val isPermissionsChecked: Boolean = false,
    val isCheckingHealthState: Boolean = false,
    val isSamsungHealthAvailable: Boolean = false,
    val grantedMetrics: Set<SamsungHealthMetric> = emptySet(),
    val metricSignals: Map<SamsungHealthMetric, WearableSignal> = emptyMap(),
    val sleepHours: Float = 7f,
    val dailySteps: Int = 8000,
    val stressLevel: Int = 5,
    val smokingStatus: SmokingStatus = SmokingStatus.NEVER,
    val alcoholDrinksPerWeek: Int = 2,
    val dietQuality: DietQuality = DietQuality.AVERAGE,
    val isSubmitting: Boolean = false,
) {
    val isProfileValid: Boolean
        get() = ageInput.toIntOrNull() != null &&
            heightInput.toIntOrNull() != null &&
            weightInput.toFloatOrNull() != null &&
            biologicalSex != null

    val allPermissionsGranted: Boolean
        get() = grantedMetrics.containsAll(SamsungHealthMetric.entries)

    val needsSleepInput: Boolean
        get() = metricSignals[SamsungHealthMetric.SLEEP] != WearableSignal.ACTIVE

    val needsStepsInput: Boolean
        get() = metricSignals[SamsungHealthMetric.STEPS] != WearableSignal.ACTIVE

    val hasRecentWearableData: Boolean
        get() = metricSignals.values.any { it == WearableSignal.ACTIVE }
}

class OnboardingViewModel(
    private val userProfileRepository: UserProfileRepository = ServiceLocator.userProfileRepository,
    private val onboardingRepository: OnboardingRepository = ServiceLocator.onboardingRepository,
    private val healthRepository: HealthRepository = ServiceLocator.healthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun advance() {
        val nextStep = when (_uiState.value.currentStep) {
            OnboardingStep.PROFILE -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.MANUAL_DATA
            OnboardingStep.MANUAL_DATA -> OnboardingStep.HABITS
            OnboardingStep.HABITS -> OnboardingStep.REVIEW
            OnboardingStep.REVIEW -> OnboardingStep.REVIEW
        }

        _uiState.update { it.copy(currentStep = nextStep) }

        if (nextStep == OnboardingStep.PERMISSIONS && !_uiState.value.isPermissionsChecked) {
            viewModelScope.launch { refreshHealthState() }
        }
    }

    fun back() {
        _uiState.update {
            it.copy(
                currentStep = when (it.currentStep) {
                    OnboardingStep.PROFILE -> OnboardingStep.PROFILE
                    OnboardingStep.PERMISSIONS -> OnboardingStep.PROFILE
                    OnboardingStep.MANUAL_DATA -> OnboardingStep.PERMISSIONS
                    OnboardingStep.HABITS -> OnboardingStep.MANUAL_DATA
                    OnboardingStep.REVIEW -> OnboardingStep.HABITS
                },
            )
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            if (_uiState.value.currentStep == OnboardingStep.PERMISSIONS) {
                refreshHealthState()
            }
        }
    }

    fun requestPermissions(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealthState = true) }

            val isAvailable = healthRepository.connectSamsung()
            if (!isAvailable) {
                _uiState.update {
                    it.copy(
                        isPermissionsChecked = true,
                        isCheckingHealthState = false,
                        isSamsungHealthAvailable = false,
                        grantedMetrics = emptySet(),
                        metricSignals = emptyMap(),
                    )
                }
                return@launch
            }

            healthRepository.requestPermissions(activity)
            refreshHealthState()
        }
    }

    private suspend fun refreshHealthState() {
        _uiState.update { it.copy(isCheckingHealthState = true) }

        val isAvailable = healthRepository.connectSamsung()
        val grantedMetrics = if (isAvailable) {
            healthRepository.grantedMetrics()
        } else {
            emptySet()
        }
        val metricSignals = if (grantedMetrics.isNotEmpty()) {
            healthRepository.signalsByMetric()
        } else {
            emptyMap()
        }

        _uiState.update {
            it.copy(
                isPermissionsChecked = true,
                isCheckingHealthState = false,
                isSamsungHealthAvailable = isAvailable,
                grantedMetrics = grantedMetrics,
                metricSignals = metricSignals,
            )
        }
    }

    fun setDisplayName(value: String) = _uiState.update { it.copy(displayName = value) }
    fun setAge(value: String) = _uiState.update { it.copy(ageInput = value) }
    fun setBiologicalSex(value: BiologicalSex) = _uiState.update { it.copy(biologicalSex = value) }
    fun setHeight(value: String) = _uiState.update { it.copy(heightInput = value) }
    fun setWeight(value: String) = _uiState.update { it.copy(weightInput = value) }
    fun setSleepHours(value: Float) = _uiState.update { it.copy(sleepHours = value) }
    fun setDailySteps(value: Int) = _uiState.update { it.copy(dailySteps = value) }
    fun setStressLevel(value: Int) = _uiState.update { it.copy(stressLevel = value) }
    fun setSmokingStatus(value: SmokingStatus) = _uiState.update { it.copy(smokingStatus = value) }
    fun setAlcohol(value: Int) = _uiState.update { it.copy(alcoholDrinksPerWeek = value) }
    fun setDietQuality(value: DietQuality) = _uiState.update { it.copy(dietQuality = value) }

    fun submit(onComplete: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            userProfileRepository.update {
                UserProfile(
                    displayName = state.displayName.ifBlank { null },
                    ageYears = state.ageInput.toIntOrNull(),
                    biologicalSex = state.biologicalSex,
                    heightCm = state.heightInput.toIntOrNull(),
                    weightKg = state.weightInput.toFloatOrNull(),
                    averageSleepHours = if (state.needsSleepInput) state.sleepHours else null,
                    averageDailySteps = if (state.needsStepsInput) state.dailySteps else null,
                    perceivedStressLevel = state.stressLevel,
                    smokingStatus = state.smokingStatus,
                    alcoholDrinksPerWeek = state.alcoholDrinksPerWeek,
                    dietQuality = state.dietQuality,
                    onboardingCompletedAtEpochMs = System.currentTimeMillis(),
                )
            }
            onboardingRepository.completeOnboarding()
            onComplete()
        }
    }
}
