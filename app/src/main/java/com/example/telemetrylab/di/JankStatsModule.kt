package com.example.telemetrylab.di

import android.app.Activity
import android.view.Window
import androidx.metrics.performance.JankStats
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object JankStatsModule {

    @Provides
    @ActivityScoped
    fun provideJankStats(activity: Activity): JankStats {
        val window: Window = activity.window

        return JankStats.createAndTrack(
            window = window,
            frameListener = { frameData ->

                android.util.Log.d("JankStats", frameData.toString())
            }
        )
    }
}