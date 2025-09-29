package com.example.telemetrylab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.example.telemetrylab.R
import com.example.telemetrylab.domain.model.TelemetryData
import com.example.telemetrylab.domain.model.TelemetryState
import com.example.telemetrylab.domain.repository.TelemetryRepository
import com.example.telemetrylab.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random


@AndroidEntryPoint
class TelemetryService : Service(), LifecycleOwner {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _lifecycleRegistry = LifecycleRegistry(this)

    init {
        _lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry

    @Inject
    lateinit var repository: TelemetryRepository

    private var isProcessing = false
    private var processingJob: Job? = null
    private var stateObserverJob: Job? = null
    private var currentComputeLoad = 1
    private var frameCount = 0L
    private var isPowerSaveMode = false


    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "telemetry_service_channel"
        private const val CHANNEL_NAME = "Telemetry Service"
        private const val CHANNEL_DESCRIPTION = "Channel for telemetry background processing"



        const val ACTION_START = "com.example.telemetrylab.action.START"
        const val ACTION_STOP = "com.example.telemetrylab.action.STOP"
        const val ACTION_UPDATE_COMPUTE_LOAD = "com.example.telemetrylab.action.UPDATE_COMPUTE_LOAD"


        const val EXTRA_COMPUTE_LOAD = "extra_compute_load"

        fun createStartIntent(context: Context, computeLoad: Int): Intent =
            Intent(context, TelemetryService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_COMPUTE_LOAD, computeLoad)
            }

        fun createStopIntent(context: Context): Intent =
            Intent(context, TelemetryService::class.java).apply {
                action = ACTION_STOP
            }

        fun createUpdateComputeLoadIntent(context: Context, computeLoad: Int): Intent =
            Intent(context, TelemetryService::class.java).apply {
                action = ACTION_UPDATE_COMPUTE_LOAD
                putExtra(EXTRA_COMPUTE_LOAD, computeLoad)
            }
            

        fun start(context: Context, computeLoad: Int) {
            val intent = createStartIntent(context, computeLoad)
            ContextCompat.startForegroundService(context, intent)
        }
        

        fun stop(context: Context) {
            val intent = createStopIntent(context)
            context.stopService(intent)
        }
        

        fun updateComputeLoad(context: Context, computeLoad: Int) {
            val intent = createUpdateComputeLoadIntent(context, computeLoad)
            context.startService(intent)
        }
    }



    override fun onCreate() {
        super.onCreate()
        _lifecycleRegistry.currentState = Lifecycle.State.CREATED
        observeStateChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _lifecycleRegistry.currentState = Lifecycle.State.STARTED
        
        when (intent?.action) {
            ACTION_START -> {
                val computeLoad = intent.getIntExtra(EXTRA_COMPUTE_LOAD, 1)
                startProcessing(computeLoad)
            }
            ACTION_STOP -> stopProcessing()
            ACTION_UPDATE_COMPUTE_LOAD -> {
                val computeLoad = intent.getIntExtra(EXTRA_COMPUTE_LOAD, 1)
                updateComputeLoad(computeLoad)
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        _lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.coroutineContext.cancelChildren()
        processingJob?.cancel()
        stateObserverJob?.cancel()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        
        super.onDestroy()
    }

    private fun observeStateChanges() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            repository.observeTelemetryState().collectLatest { state ->
                updateNotification(state)
                
                isPowerSaveMode = state.isPowerSaveMode
                
                if (state.isRunning && isPowerSaveMode) {
                    val newLoad = max(1, state.currentComputeLoad - 1)
                    if (newLoad != currentComputeLoad) {
                        updateComputeLoad(newLoad)
                    }
                }
            }
        }
    }

    private fun startProcessing(computeLoad: Int) {
        if (isProcessing) return

        isProcessing = true
        currentComputeLoad = computeLoad.coerceIn(1, 5)

        startForeground(NOTIFICATION_ID, createNotification(TelemetryState.initial()))

        processingJob = serviceScope.launch {
            var frameId = 0L
            while (isActive) {
                val frameStartTime = System.nanoTime()
                val result = performComputation(currentComputeLoad)
                val frameTimeMs = (System.nanoTime() - frameStartTime) / 1_000_000f
                val isJank = frameTimeMs > 16.67f

                val telemetryData = TelemetryData(
                    frameId = frameId,
                    timestamp = frameStartTime,
                    latencyMs = frameTimeMs,
                    computeLoad = currentComputeLoad,
                    isJank = isJank
                )
                repository.recordTelemetryData(telemetryData)

                val targetFrameTimeMs = if (isPowerSaveMode) 100L else 50L
                val elapsedMs = (System.nanoTime() - frameStartTime) / 1_000_000L
                val delayMs = max(0, targetFrameTimeMs - elapsedMs)

                delay(delayMs)
                frameId++
            }
        }
    }


    private suspend fun performComputation(computeLoad: Int): Float {
        var result = 0f
        val size = 256
        val matrix = Array(size) { FloatArray(size) { Random.nextFloat() } }
        val kernel = arrayOf(
            floatArrayOf(1f, 0f, -1f),
            floatArrayOf(2f, 0f, -2f),
            floatArrayOf(1f, 0f, -1f)
        )
        
        repeat(computeLoad) {
            for (i in 1 until size - 1) {
                for (j in 1 until size - 1) {
                    var sum = 0f
                    for (ki in 0..2) {
                        for (kj in 0..2) {
                            sum += matrix[i + ki - 1][j + kj - 1] * kernel[ki][kj]
                        }
                    }
                    result += sum
                }
            }
        }
        
        return result
    }

    private fun stopProcessing() {
        processingJob?.cancel()
        processingJob = null
        isProcessing = false
        stopForeground(true)
        stopSelf()
    }

    private fun updateComputeLoad(computeLoad: Int) {
        currentComputeLoad = computeLoad.coerceIn(1, 5)
        
        lifecycleScope.launch {
            repository.updateComputeLoad(currentComputeLoad)
        }
    }

    private fun createNotification(state: TelemetryState): Notification {
        val channelId = "telemetry_service_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.telemetry_running))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(state: TelemetryState) {
        if (!isProcessing) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Service")
            .setContentText("Telemetry is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
