package com.mzwprojects.mytwin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.data.repository.OnboardingRepository
import com.mzwprojects.mytwin.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns app-level startup state.
 *
 * `onboardingCompleted` drives the splash-screen `keepOnScreenCondition` and the
 * NavGraph's start destination. `null` = not yet known (DataStore still loading).
 */
class RootViewModel(
    private val onboardingRepository: OnboardingRepository =
        ServiceLocator.onboardingRepository,
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean?> = onboardingRepository.onboardingCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.completeOnboarding()
        }
    }
}