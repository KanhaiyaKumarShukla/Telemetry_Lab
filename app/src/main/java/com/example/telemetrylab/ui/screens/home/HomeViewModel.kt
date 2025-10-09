package com.example.telemetrylab.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemetrylab.domain.model.TelemetryMetrics
import com.example.telemetrylab.domain.usecase.ObserveMetricsUseCase
import com.example.telemetrylab.domain.usecase.ObserveTelemetryStateUseCase
import com.example.telemetrylab.domain.usecase.StartTelemetryUseCase
import com.example.telemetrylab.domain.usecase.StopTelemetryUseCase
import com.example.telemetrylab.domain.usecase.UpdateComputeLoadUseCase
import com.example.telemetrylab.manger.BatteryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startTelemetryUseCase: StartTelemetryUseCase,
    private val stopTelemetryUseCase: StopTelemetryUseCase,
    private val updateComputeLoadUseCase: UpdateComputeLoadUseCase,
    private val observeTelemetryStateUseCase: ObserveTelemetryStateUseCase,
    private val observeMetricsUseCase: ObserveMetricsUseCase,
    private val batteryManager: BatteryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observeTelemetryState()
        observeMetrics()
        observeBatteryState()
    }

    fun toggleTelemetry() {
        viewModelScope.launch {
            if (_uiState.value.isRunning) {
                stopTelemetry()
            } else {
                startTelemetry()
            }
        }
    }

    fun updateComputeLoad(computeLoad: Int) {
        viewModelScope.launch {
            val adaptedLoad = batteryManager.getAdaptedComputeLoad(computeLoad)
            updateComputeLoadUseCase(adaptedLoad)
        }
    }

    private fun startTelemetry() {
        viewModelScope.launch {
            val computeLoad = _uiState.value.computeLoad
            val adaptedLoad = batteryManager.getAdaptedComputeLoad(computeLoad)
            startTelemetryUseCase(adaptedLoad)
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
                _uiState.update { current ->
                    current.copy(
                        isRunning = state.isRunning,
                        isPowerSaveMode = state.isPowerSaveMode,
                        currentFrameId = state.currentFrameId,
                        currentFrameLatencyMs = state.currentFrameLatencyMs,
                        computeLoad = state.currentComputeLoad
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeMetrics() {
        observeMetricsUseCase(windowSize = 600) // 30 seconds at 20Hz
            .onEach { metrics ->
                _uiState.update { current ->
                    current.copy(
                        averageLatencyMs = metrics.averageLatencyMs,
                        jankPercentage = metrics.jankPercentage,
                        jankCount = metrics.jankCount,
                        frameCount = metrics.frameCount,
                        minLatencyMs = metrics.minLatencyMs,
                        maxLatencyMs = metrics.maxLatencyMs
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBatteryState() {
        batteryManager.isPowerSaveMode
            .onEach { isPowerSaveMode ->
                _uiState.update { current ->
                    current.copy(isPowerSaveMode = isPowerSaveMode)
                }
                // Update compute load if running
                if (_uiState.value.isRunning) {
                    updateComputeLoad(_uiState.value.computeLoad)
                }
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
