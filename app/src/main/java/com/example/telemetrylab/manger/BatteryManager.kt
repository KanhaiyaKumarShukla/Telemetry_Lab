package com.example.telemetrylab.manger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max


@Singleton
class BatteryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode

    private val powerSaveModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updatePowerSaveState()
        }
    }

    init {
        registerPowerSaveReceiver()
        updatePowerSaveState()
    }

    private fun registerPowerSaveReceiver() {
        val filter = IntentFilter().apply {
            addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(powerSaveModeReceiver, filter)
    }

    private fun updatePowerSaveState() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isPowerSaveMode.value = powerManager.isPowerSaveMode
    }

    fun getAdaptedComputeLoad(requestedLoad: Int): Int {
        return if (_isPowerSaveMode.value) {
            max(1, requestedLoad - 1)
        } else {
            requestedLoad
        }
    }

    fun getAdaptedFrameRate(): Int {
        return if (_isPowerSaveMode.value) 10 else 20
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(powerSaveModeReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
        }
    }
}