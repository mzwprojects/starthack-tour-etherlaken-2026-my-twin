package com.mzwprojects.mytwin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.di.ServiceLocator
import com.mzwprojects.mytwin.simulation.SimulationBundle
import com.mzwprojects.mytwin.simulation.SimulationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimulationUiState(
    val bundle: SimulationBundle? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class SimulationViewModel(
    private val simulationRepository: SimulationRepository = ServiceLocator.simulationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                simulationRepository.buildSimulationBundle()
            }.onSuccess { bundle ->
                _uiState.value = SimulationUiState(
                    bundle = bundle,
                    isLoading = false,
                )
            }.onFailure { error ->
                _uiState.value = SimulationUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load simulation.",
                )
            }
        }
    }
}
