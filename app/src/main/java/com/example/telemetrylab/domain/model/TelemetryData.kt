package com.example.telemetrylab.domain.model


data class TelemetryData(
    val frameId: Long,
    val timestamp: Long = System.nanoTime(),
    val latencyMs: Float,
    val computeLoad: Int,
    val isJank: Boolean = false
) {
    companion object {

        fun create(
            frameId: Long,
            latencyMs: Float,
            computeLoad: Int,
            isJank: Boolean = false
        ): TelemetryData {
            return TelemetryData(
                frameId = frameId,
                latencyMs = latencyMs,
                computeLoad = computeLoad.coerceIn(1, 5),
                isJank = isJank
            )
        }
    }
}


data class TelemetryMetrics(
    val averageLatencyMs: Float = 0f,
    val jankPercentage: Float = 0f,
    val jankCount: Int = 0,
    val frameCount: Int = 0,
    val minLatencyMs: Float = 0f,
    val maxLatencyMs: Float = 0f
) {
    companion object {

        fun empty() = TelemetryMetrics()
        

        fun from(telemetryData: List<TelemetryData>): TelemetryMetrics {
            if (telemetryData.isEmpty()) return empty()
            
            val jankFrames = telemetryData.count { it.isJank }
            val totalFrames = telemetryData.size
            val latencies = telemetryData.map { it.latencyMs }
            
            return TelemetryMetrics(
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


data class TelemetryState(
    val isRunning: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val currentFrameId: Long = 0,
    val currentComputeLoad: Int = 1,
    val currentFrameLatencyMs: Float = 0f,
    val metrics: TelemetryMetrics = TelemetryMetrics.empty()
) {
    companion object {

        fun initial() = TelemetryState()
    }
}
