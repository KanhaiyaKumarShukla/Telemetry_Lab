package com.example.telemetrylab

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TelemetryApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "telemetry_service_channel"
    }
}

@Composable
fun rememberTelemetryApp(): TelemetryApp {
    val context = LocalContext.current
    return remember { context.applicationContext as TelemetryApp }
}
