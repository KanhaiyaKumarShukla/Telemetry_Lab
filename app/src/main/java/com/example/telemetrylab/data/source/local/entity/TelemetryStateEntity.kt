package com.example.telemetrylab.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.telemetrylab.domain.model.TelemetryState
import com.example.telemetrylab.domain.model.TelemetryMetrics

@Entity(tableName = "telemetry_state")
data class TelemetryStateEntity(
    @PrimaryKey
    val id: Int = 0,
    val isRunning: Boolean,
    val isPowerSaveMode: Boolean,
    val currentFrameId: Long,
    val currentComputeLoad: Int,
    val currentFrameLatencyMs: Float,
    val averageLatencyMs: Float,
    val jankPercentage: Float,
    val jankCount: Int,
    val frameCount: Int,
    val minLatencyMs: Float,
    val maxLatencyMs: Float
) {

    fun toDomain(): TelemetryState {
        return TelemetryState(
            isRunning = isRunning,
            isPowerSaveMode = isPowerSaveMode,
            currentFrameId = currentFrameId,
            currentComputeLoad = currentComputeLoad,
            currentFrameLatencyMs = currentFrameLatencyMs,
            metrics = TelemetryMetrics(
                averageLatencyMs = averageLatencyMs,
                jankPercentage = jankPercentage,
                jankCount = jankCount,
                frameCount = frameCount,
                minLatencyMs = minLatencyMs,
                maxLatencyMs = maxLatencyMs
            )
        )
    }

    companion object {

        fun fromDomain(state: TelemetryState): TelemetryStateEntity {
            return TelemetryStateEntity(
                isRunning = state.isRunning,
                isPowerSaveMode = state.isPowerSaveMode,
                currentFrameId = state.currentFrameId,
                currentComputeLoad = state.currentComputeLoad,
                currentFrameLatencyMs = state.currentFrameLatencyMs,
                averageLatencyMs = state.metrics.averageLatencyMs,
                jankPercentage = state.metrics.jankPercentage,
                jankCount = state.metrics.jankCount,
                frameCount = state.metrics.frameCount,
                minLatencyMs = state.metrics.minLatencyMs,
                maxLatencyMs = state.metrics.maxLatencyMs
            )
        }
    }
}
