package com.example.telemetrylab.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.telemetrylab.service.TelemetryService
import com.example.telemetrylab.ui.screens.home.HomeScreen
import com.example.telemetrylab.ui.theme.TelemetryLabTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            TelemetryLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopService(TelemetryService.createStopIntent(this))
    }
}
