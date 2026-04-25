package com.mzwprojects.mytwin.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.data.model.BiologicalSex
import com.mzwprojects.mytwin.data.model.DietQuality
import com.mzwprojects.mytwin.data.model.SmokingStatus
import com.mzwprojects.mytwin.data.model.UserProfile
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
    // Profile
    val displayName: String = "",
    val ageInput: String = "",
    val biologicalSex: BiologicalSex? = null,
    val heightInput: String = "",
    val weightInput: String = "",
    // Permissions — Samsung Health
    val isPermissionsChecked: Boolean = false,
    val isSamsungHealthConnected: Boolean = false,
    val allPermissionsGranted: Boolean = false,
    val isLoadingSignals: Boolean = false,
    // Manual data
    val sleepHours: Float = 7f,
    val dailySteps: Int = 8000,
    val stressLevel: Int = 5,
    // Habits
    val smokingStatus: SmokingStatus = SmokingStatus.NEVER,
    val alcoholDrinksPerWeek: Int = 2,
    val dietQuality: DietQuality = DietQuality.AVERAGE,
    // Submit
    val isSubmitting: Boolean = false,
) {
    val isProfileValid: Boolean
        get() = ageInput.toIntOrNull() != null &&
                heightInput.toIntOrNull() != null &&
                weightInput.toFloatOrNull() != null &&
                biologicalSex != null

    val needsSleepInput: Boolean get() = !allPermissionsGranted
    val needsStepsInput: Boolean get() = !allPermissionsGranted
    val allWearableCovered: Boolean get() = allPermissionsGranted
}

class OnboardingViewModel(
    private val userProfileRepository: UserProfileRepository = ServiceLocator.userProfileRepository,
    private val onboardingRepository: OnboardingRepository = ServiceLocator.onboardingRepository,
    private val healthRepository: HealthRepository = ServiceLocator.healthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ─── Navigation ──────────────────────────────────────────────────────

    fun advance() {
        val next = when (_uiState.value.currentStep) {
            OnboardingStep.PROFILE -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.MANUAL_DATA
            OnboardingStep.MANUAL_DATA -> OnboardingStep.HABITS
            OnboardingStep.HABITS -> OnboardingStep.REVIEW
            OnboardingStep.REVIEW -> _uiState.value.currentStep
        }
        _uiState.update { it.copy(currentStep = next) }
        if (next == OnboardingStep.PERMISSIONS && !_uiState.value.isPermissionsChecked) {
            viewModelScope.launch { loadPermissionsState() }
        }
    }

    fun back() {
        _uiState.update {
            it.copy(currentStep = when (it.currentStep) {
                OnboardingStep.PROFILE -> it.currentStep
                OnboardingStep.PERMISSIONS -> OnboardingStep.PROFILE
                OnboardingStep.MANUAL_DATA -> OnboardingStep.PERMISSIONS
                OnboardingStep.HABITS -> OnboardingStep.MANUAL_DATA
                OnboardingStep.REVIEW -> OnboardingStep.HABITS
            })
        }
    }

    // ─── Permissions step — Samsung Health ───────────────────────────────

    private suspend fun loadPermissionsState() {
        val connected = healthRepository.connectSamsung()
        val granted = connected && healthRepository.areAllPermissionsGranted()
        _uiState.update {
            it.copy(
                isPermissionsChecked = true,
                isSamsungHealthConnected = connected,
                allPermissionsGranted = granted,
            )
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            if (_uiState.value.currentStep != OnboardingStep.PERMISSIONS) return@launch
            if (!_uiState.value.isSamsungHealthConnected) return@launch

            val granted = healthRepository.areAllPermissionsGranted()

            _uiState.update {
                it.copy(
                    isPermissionsChecked = true,
                    allPermissionsGranted = granted,
                )
            }
        }
    }

    fun requestPermissions(activity: Activity) {
        viewModelScope.launch {
            if (!_uiState.value.isSamsungHealthConnected) {
                _uiState.update {
                    it.copy(
                        isPermissionsChecked = true,
                        isSamsungHealthConnected = false,
                        allPermissionsGranted = false,
                    )
                }
                return@launch
            }

            healthRepository.requestPermissions(activity)

            val granted = healthRepository.areAllPermissionsGranted()

            _uiState.update {
                it.copy(
                    isPermissionsChecked = true,
                    allPermissionsGranted = granted,
                )
            }
        }
    }

    // ─── Profile step ────────────────────────────────────────────────────

    fun setDisplayName(v: String) = _uiState.update { it.copy(displayName = v) }
    fun setAge(v: String) = _uiState.update { it.copy(ageInput = v) }
    fun setBiologicalSex(v: BiologicalSex) = _uiState.update { it.copy(biologicalSex = v) }
    fun setHeight(v: String) = _uiState.update { it.copy(heightInput = v) }
    fun setWeight(v: String) = _uiState.update { it.copy(weightInput = v) }

    // ─── Manual data step ────────────────────────────────────────────────

    fun setSleepHours(v: Float) = _uiState.update { it.copy(sleepHours = v) }
    fun setDailySteps(v: Int) = _uiState.update { it.copy(dailySteps = v) }
    fun setStressLevel(v: Int) = _uiState.update { it.copy(stressLevel = v) }

    // ─── Habits step ─────────────────────────────────────────────────────

    fun setSmokingStatus(v: SmokingStatus) = _uiState.update { it.copy(smokingStatus = v) }
    fun setAlcohol(v: Int) = _uiState.update { it.copy(alcoholDrinksPerWeek = v) }
    fun setDietQuality(v: DietQuality) = _uiState.update { it.copy(dietQuality = v) }

    // ─── Submit ──────────────────────────────────────────────────────────

    fun submit(onComplete: () -> Unit) {
        val s = _uiState.value
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            userProfileRepository.update {
                UserProfile(
                    displayName = s.displayName.ifBlank { null },
                    ageYears = s.ageInput.toIntOrNull(),
                    biologicalSex = s.biologicalSex,
                    heightCm = s.heightInput.toIntOrNull(),
                    weightKg = s.weightInput.toFloatOrNull(),
                    averageSleepHours = if (s.needsSleepInput) s.sleepHours else null,
                    averageDailySteps = if (s.needsStepsInput) s.dailySteps else null,
                    perceivedStressLevel = s.stressLevel,
                    smokingStatus = s.smokingStatus,
                    alcoholDrinksPerWeek = s.alcoholDrinksPerWeek,
                    dietQuality = s.dietQuality,
                    onboardingCompletedAtEpochMs = System.currentTimeMillis(),
                )
            }
            onboardingRepository.completeOnboarding()
            onComplete()
        }
    }
}
