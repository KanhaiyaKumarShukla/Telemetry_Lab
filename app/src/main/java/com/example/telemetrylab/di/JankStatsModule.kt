package com.example.telemetrylab.di

import android.app.Activity
import android.app.Application
import android.view.Window
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Module
@InstallIn(ViewModelComponent::class)
object JankStatsModule {
    
    @Provides
    @ViewModelScoped
    fun provideJankStatsManager(application: Application): JankStatsManager {
        return JankStatsManager(application)
    }
}

@ViewModelScoped
class JankStatsManager @Inject constructor(
    private val application: Application
) : LifecycleEventObserver {
    private val _jankStatsFlow = MutableStateFlow(JankStatsState())
    private var jankStats: JankStats? = null
    private var currentActivity: Activity? = null
    val jankStatsFlow: StateFlow<JankStatsState> = _jankStatsFlow

    fun registerActivity(activity: Activity) {
        if (activity is FragmentActivity) {
            currentActivity = activity
            activity.lifecycle.addObserver(this)
            initializeJankStats()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            jankStats = null
            currentActivity = null
        }
    }

    private fun initializeJankStats() {
        if (jankStats != null) return

        val window = currentActivity?.window ?: return

        jankStats = JankStats.createAndTrack(
            window = window,
            frameListener = { frameData ->
                val isJank = frameData.isJank
                val frameDuration = frameData.frameDurationUiNanos

                // Update state with new frame data
                val currentState = _jankStatsFlow.value
                _jankStatsFlow.value = currentState.copy(
                    totalFrames = currentState.totalFrames + 1L,
                    jankFrames = currentState.jankFrames + (if (isJank) 1L else 0L),
                    totalFrameTimeNanos = currentState.totalFrameTimeNanos + frameDuration,
                    lastFrameTimeNanos = frameDuration,
                    timestamp = System.currentTimeMillis()
                )
            }
        )
    }
}
data class JankStatsState(
    val totalFrames: Long = 0,
    val jankFrames: Long = 0,
    val totalFrameTimeNanos: Long = 0,
    val lastFrameTimeNanos: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val jankPercentage: Float
        get() = if (totalFrames > 0) {
            (jankFrames.toFloat() / totalFrames) * 100f
        } else 0f

    val averageFrameTimeMs: Float
        get() = if (totalFrames > 0) {
            totalFrameTimeNanos.toFloat() / (totalFrames * 1_000_000f)
        } else 0f
}