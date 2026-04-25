package com.mzwprojects.mytwin.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.data.datasource.OnboardingDataSource
import com.mzwprojects.mytwin.data.repository.OnboardingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WelcomeViewModel(app: Application) : AndroidViewModel(app) {

    // AndroidViewModel bringt den Application-Context mit — kein Hilt nötig
    private val repository = OnboardingRepository(
        OnboardingDataSource(app.applicationContext)
    )

    // null = noch am Laden, true/false = Entscheidung getroffen
    val onboardingCompleted: StateFlow<Boolean?> = repository.onboardingCompleted
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = null,
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
        }
    }
}