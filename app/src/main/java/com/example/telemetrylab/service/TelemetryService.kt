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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.telemetrylab.R
import com.example.telemetrylab.data.source.remote.JankStatsMonitor
import com.example.telemetrylab.domain.model.TelemetryData
import com.example.telemetrylab.domain.model.TelemetryState
import com.example.telemetrylab.domain.repository.TelemetryRepository
import com.example.telemetrylab.manger.BatteryManager
import com.example.telemetrylab.processor.ConvolutionProcessor
import com.example.telemetrylab.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.random.Random

@AndroidEntryPoint
class TelemetryService : Service(), LifecycleOwner {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry


    @Inject
    lateinit var repository: TelemetryRepository
    
    @Inject 
    lateinit var convolutionProcessor: ConvolutionProcessor
    
    @Inject
    lateinit var batteryManager: BatteryManager
    
    @Inject
    lateinit var jankStatsMonitor: JankStatsMonitor

    private var isProcessing = false
    private var processingJob: Job? = null
    private var batteryObserverJob: Job? = null
    private var stateObserverJob: Job? = null
    private var currentComputeLoad = 1
    private var currentFrameRate = 20
    private var isPowerSaveMode = false


    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "telemetry_service_channel"
        
        const val ACTION_START = "com.example.telemetrylab.action.START"
        const val ACTION_STOP = "com.example.telemetrylab.action.STOP"
        const val ACTION_UPDATE_COMPUTE_LOAD = "com.example.telemetrylab.action.UPDATE_COMPUTE_LOAD"
        const val EXTRA_COMPUTE_LOAD = "extra_compute_load"

        fun start(context: Context, computeLoad: Int) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_COMPUTE_LOAD, computeLoad)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }



    override fun onCreate() {
        super.onCreate()
        _lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createNotificationChannel()
        observeBatteryState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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





    private fun observeBatteryState() {
        batteryObserverJob = serviceScope.launch {
            batteryManager.isPowerSaveMode.collect { isPowerSaveMode ->
                if (isProcessing) {
                    // Adapt to power save mode changes
                    currentComputeLoad = batteryManager.getAdaptedComputeLoad(currentComputeLoad)
                    currentFrameRate = batteryManager.getAdaptedFrameRate()
                    
                    // Update the state through the repository methods
                    if (isPowerSaveMode) {
                        repository.updateComputeLoad(currentComputeLoad)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background telemetry data processing"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Lab")
            .setContentText("Processing telemetry data")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Set FGS type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationBuilder.setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
        }

        return notificationBuilder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        batteryObserverJob?.cancel()
        processingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeStateChanges() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            repository.observeTelemetryState().collectLatest { state ->
                updateNotification(state)

                isPowerSaveMode = state.isPowerSaveMode

                if (state.isRunning && state.isPowerSaveMode) {
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
        currentComputeLoad = batteryManager.getAdaptedComputeLoad(computeLoad)
        currentFrameRate = batteryManager.getAdaptedFrameRate()

        startForeground(NOTIFICATION_ID, createNotification(TelemetryState.initial()))

        processingJob = serviceScope.launch {
            var frameId = 0L
            var lastFrameTime = System.nanoTime()

            while (isActive) {
                val frameStartTime = System.nanoTime()

                // Calculate time since last frame (for frame rate targeting)
                val timeSinceLastFrameNs = frameStartTime - lastFrameTime
                val targetFrameTimeNs = if (isPowerSaveMode) 100_000_000L else 50_000_000L // 10Hz or 20Hz

                // Only process a new frame if enough time has passed
                if (timeSinceLastFrameNs >= targetFrameTimeNs) {
                    lastFrameTime = frameStartTime

                    // Perform computation and measure time
                    val computeStartTime = System.nanoTime()
                    val result = performComputation(currentComputeLoad)
                    val computeTimeNs = System.nanoTime() - computeStartTime
                    val frameTimeMs = computeTimeNs / 1_000_000f

                    // More sophisticated jank detection
                    val frameBudgetMs = if (isPowerSaveMode) 16.67f else 16.67f // 60fps target
                    val isJank = frameTimeMs > frameBudgetMs

                    // Record telemetry data
                    val telemetryData = TelemetryData(
                        frameId = frameId,
                        timestamp = frameStartTime,
                        latencyMs = frameTimeMs,
                        computeLoad = currentComputeLoad,
                        isJank = isJank
                    )
                    repository.recordTelemetryData(telemetryData)

                    // Update repository state
                    val currentState = repository.getCurrentState()
                    val updatedState = currentState.copy(
                        isRunning = true,
                        currentFrameId = frameId,
                        currentFrameLatencyMs = frameTimeMs,
                        currentComputeLoad = currentComputeLoad,
                        isPowerSaveMode = isPowerSaveMode
                    )
                    // Update the state through the repository if needed
                    // Note: The repository's observeTelemetryState() will emit the new state

                    frameId++

                    // Calculate time to next frame
                    val elapsedNs = System.nanoTime() - frameStartTime
                    val remainingNs = targetFrameTimeNs - elapsedNs

                    // If we have time left, delay until next frame
                    if (remainingNs > 0) {
                        delay(remainingNs / 1_000_000)
                    }
                } else {
                    // Not enough time has passed, yield to other coroutines
                    delay(1)
                }
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateComputeLoad(computeLoad: Int) {
        currentComputeLoad = batteryManager.getAdaptedComputeLoad(computeLoad)
        serviceScope.launch {
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
