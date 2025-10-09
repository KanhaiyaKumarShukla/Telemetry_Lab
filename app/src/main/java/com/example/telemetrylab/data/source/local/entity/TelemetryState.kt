package com.example.telemetrylab.data.source.local.entity

// TelemetryState.kt
data class TelemetryState(
    val isRunning: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val currentFrameId: Long = 0,
    val currentComputeLoad: Int = 1,
    val currentFrameLatencyMs: Float = 0f,
    val frameRate: Int = 20, // 20Hz normal, 10Hz power save
    val metrics: TelemetryMetrics = TelemetryMetrics.empty()
) {
    companion object {
        fun initial() = TelemetryState()
    }
}

data class TelemetryMetrics(
    val averageLatencyMs: Float = 0f,
    val jankPercentage: Float = 0f,
    val jankCount: Int = 0,
    val frameCount: Int = 0,
    val minLatencyMs: Float = 0f,
    val maxLatencyMs: Float = 0f,
    val framesInLast30s: Int = 0
) {
    companion object {
        fun empty() = TelemetryMetrics()
    }
}