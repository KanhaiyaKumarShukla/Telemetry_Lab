package com.example.telemetrylab.domain.repository

import com.example.telemetrylab.domain.model.TelemetryData
import com.example.telemetrylab.domain.model.TelemetryMetrics
import com.example.telemetrylab.domain.model.TelemetryState
import kotlinx.coroutines.flow.Flow


interface TelemetryRepository {

    suspend fun startTelemetry(computeLoad: Int): Boolean
    

    suspend fun stopTelemetry()
    

    suspend fun updateComputeLoad(computeLoad: Int)
    

    fun observeTelemetryState(): Flow<TelemetryState>
    

    fun observeTelemetryData(): Flow<List<TelemetryData>>
    

    fun observeMetrics(windowSize: Int = 30): Flow<TelemetryMetrics>
    

    suspend fun getCurrentState(): TelemetryState
    

    suspend fun recordTelemetryData(data: TelemetryData)
    

    suspend fun clearTelemetryData()
}
