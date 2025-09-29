package com.example.telemetrylab.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemetrylab.domain.model.TelemetryMetrics
import com.example.telemetrylab.domain.model.TelemetryState
import com.example.telemetrylab.domain.usecase.ObserveMetricsUseCase
import com.example.telemetrylab.domain.usecase.ObserveTelemetryStateUseCase
import com.example.telemetrylab.domain.usecase.StartTelemetryUseCase
import com.example.telemetrylab.domain.usecase.StopTelemetryUseCase
import com.example.telemetrylab.domain.usecase.UpdateComputeLoadUseCase
import com.example.telemetrylab.service.TelemetryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startTelemetryUseCase: StartTelemetryUseCase,
    private val stopTelemetryUseCase: StopTelemetryUseCase,
    private val updateComputeLoadUseCase: UpdateComputeLoadUseCase,
    private val observeTelemetryStateUseCase: ObserveTelemetryStateUseCase,
    private val observeMetricsUseCase: ObserveMetricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observeTelemetryState()
        observeMetrics()
    }


    fun toggleTelemetry() {
        viewModelScope.launch {
            val currentState = _uiState.value.isRunning
            if (currentState) {
                stopTelemetry()
            } else {
                startTelemetry()
            }
        }
    }


    fun updateComputeLoad(computeLoad: Int) {
        viewModelScope.launch {
            updateComputeLoadUseCase(computeLoad)
        }
    }

    private fun startTelemetry() {
        viewModelScope.launch {
            val computeLoad = _uiState.value.computeLoad
            startTelemetryUseCase(computeLoad)
        }
    }

    private fun stopTelemetry() {
        viewModelScope.launch {
            stopTelemetryUseCase()
        }
    }

    private fun observeTelemetryState() {
        observeTelemetryStateUseCase()
            .onEach { state ->
                _uiState.value = _uiState.value.copy(
                    isRunning = state.isRunning,
                    isPowerSaveMode = state.isPowerSaveMode,
                    currentFrameId = state.currentFrameId,
                    currentFrameLatencyMs = state.currentFrameLatencyMs,
                    computeLoad = state.currentComputeLoad
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeMetrics() {
        observeMetricsUseCase()
            .onEach { metrics ->
                _uiState.value = _uiState.value.copy(
                    averageLatencyMs = metrics.averageLatencyMs,
                    jankPercentage = metrics.jankPercentage,
                    jankCount = metrics.jankCount,
                    frameCount = metrics.frameCount,
                    minLatencyMs = metrics.minLatencyMs,
                    maxLatencyMs = metrics.maxLatencyMs
                )
            }
            .launchIn(viewModelScope)
    }
}


data class HomeUiState(
    val isRunning: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val currentFrameId: Long = 0,
    val currentFrameLatencyMs: Float = 0f,
    val computeLoad: Int = 1,
    val averageLatencyMs: Float = 0f,
    val jankPercentage: Float = 0f,
    val jankCount: Int = 0,
    val frameCount: Int = 0,
    val minLatencyMs: Float = 0f,
    val maxLatencyMs: Float = 0f
)
