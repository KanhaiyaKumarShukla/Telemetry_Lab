package com.example.telemetrylab.data.repository

import com.example.telemetrylab.data.source.local.TelemetryLocalDataSource
import com.example.telemetrylab.domain.model.TelemetryData
import com.example.telemetrylab.domain.model.TelemetryMetrics
import com.example.telemetrylab.domain.model.TelemetryState
import com.example.telemetrylab.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val localDataSource: TelemetryLocalDataSource
) : TelemetryRepository {

    override suspend fun startTelemetry(computeLoad: Int): Boolean {
        return try {
            localDataSource.updateTelemetryState { currentState ->
                if (currentState.isRunning) {
                    return@updateTelemetryState currentState
                }
                currentState.copy(
                    isRunning = true,
                    currentComputeLoad = computeLoad.coerceIn(1, 5),
                    currentFrameId = 0,
                    currentFrameLatencyMs = 0f,
                    metrics = TelemetryMetrics.empty()
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun stopTelemetry() {
        localDataSource.updateTelemetryState { currentState ->
            currentState.copy(isRunning = false)
        }
    }

    override suspend fun updateComputeLoad(computeLoad: Int) {
        localDataSource.updateTelemetryState { currentState ->
            currentState.copy(currentComputeLoad = computeLoad.coerceIn(1, 5))
        }
    }

    override fun observeTelemetryState(): Flow<TelemetryState> {
        return localDataSource.observeTelemetryState()
    }

    override fun observeTelemetryData(): Flow<List<TelemetryData>> {
        return localDataSource.observeTelemetryData()
    }

    override fun observeMetrics(windowSize: Int): Flow<TelemetryMetrics> {
        return localDataSource.observeTelemetryData()
            .map { dataList ->
                val recentData = dataList.takeLast(windowSize).takeIf { it.isNotEmpty() } 
                    ?: localDataSource.getLatestTelemetryData(windowSize)
                
                if (recentData.isEmpty()) {
                    TelemetryMetrics.empty()
                } else {
                    val jankFrames = recentData.count { it.isJank }
                    val totalFrames = recentData.size
                    val latencies = recentData.map { it.latencyMs }
                    
                    TelemetryMetrics(
                        averageLatencyMs = latencies.average().toFloat(),
                        jankPercentage = (jankFrames.toFloat() / totalFrames) * 100,
                        jankCount = jankFrames,
                        frameCount = totalFrames,
                        minLatencyMs = latencies.minOrNull() ?: 0f,
                        maxLatencyMs = latencies.maxOrNull() ?: 0f
                    )
                }
            }
    }

    override suspend fun getCurrentState(): TelemetryState {
        return localDataSource.getTelemetryState()
    }

    override suspend fun recordTelemetryData(data: TelemetryData) {
        localDataSource.saveTelemetryData(data)
        

        localDataSource.updateTelemetryState { currentState ->
            currentState.copy(
                currentFrameId = data.frameId,
                currentFrameLatencyMs = data.latencyMs,
                currentComputeLoad = data.computeLoad
            )
        }
    }

    override suspend fun clearTelemetryData() {
        localDataSource.clearTelemetryData()
        localDataSource.updateTelemetryState { currentState ->
            currentState.copy(
                currentFrameId = 0,
                currentFrameLatencyMs = 0f,
                metrics = TelemetryMetrics.empty()
            )
        }
    }
}
