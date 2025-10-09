package com.example.telemetrylab.data.source.remote

import android.app.Activity
import android.view.Window
import androidx.metrics.performance.JankStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JankStatsMonitor @Inject constructor() {
    private var jankStats: JankStats? = null

    private val _jankMetrics = MutableStateFlow(JankMetrics())
    val jankMetrics: StateFlow<JankMetrics> = _jankMetrics

    fun initialize(activity: Activity) {
        // Create JankStats with the current window
        jankStats = JankStats.createAndTrack(activity.window) { frameData ->
            // This callback runs on a background thread
            val isJanky = frameData.isJank

            // Calculate frame duration using frameDurationUiNanos which is available in the JankStats API
            // Convert nanoseconds to milliseconds by dividing by 1,000,000
            val frameDurationMs = frameData.frameDurationUiNanos / 1_000_000f

            // Update metrics
            _jankMetrics.update { current ->
                current.copy(
                    jankCount = if (isJanky) current.jankCount + 1 else current.jankCount,
                    totalFrames = current.totalFrames + 1,
                    lastJankTime = if (isJanky) System.currentTimeMillis() else current.lastJankTime,
                    lastFrameDurationMs = frameDurationMs,
                    totalFrameTimeMs = current.totalFrameTimeMs + frameDurationMs
                )
            }
        }

        // Enable tracking
        jankStats?.isTrackingEnabled = true
    }

    fun getJankPercentage(): Float {
        val metrics = _jankMetrics.value
        return if (metrics.totalFrames > 0) {
            (metrics.jankCount.toFloat() / metrics.totalFrames) * 100f
        } else {
            0f
        }
    }
    
    fun getAverageFrameTimeMs(): Float {
        val metrics = _jankMetrics.value
        return if (metrics.totalFrames > 0) {
            metrics.totalFrameTimeMs / metrics.totalFrames
        } else {
            0f
        }
    }

    fun recordFrame() {
        _jankMetrics.update { current ->
            current.copy(totalFrames = current.totalFrames + 1)
        }
    }

    fun reset() {
        _jankMetrics.value = JankMetrics()
    }
    
    fun onPause() {
        jankStats?.isTrackingEnabled = false
    }
    
    fun onResume() {
        jankStats?.isTrackingEnabled = true
    }
}

data class JankMetrics(
    val jankCount: Int = 0,
    val totalFrames: Int = 0,
    val lastJankTime: Long = 0,
    val lastFrameDurationMs: Float = 0f,
    val totalFrameTimeMs: Float = 0f
)