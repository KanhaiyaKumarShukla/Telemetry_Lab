package com.example.telemetrylab.data.source.local

import com.example.telemetrylab.domain.model.TelemetryData
import com.example.telemetrylab.domain.model.TelemetryState
import kotlinx.coroutines.flow.Flow


interface TelemetryLocalDataSource {

    suspend fun saveTelemetryData(data: TelemetryData)

    suspend fun getLatestTelemetryData(limit: Int = 100): List<TelemetryData>
    

    fun observeTelemetryData(): Flow<List<TelemetryData>>
    

    suspend fun getTelemetryState(): TelemetryState
    

    suspend fun updateTelemetryState(update: (TelemetryState) -> TelemetryState)
    

    fun observeTelemetryState(): Flow<TelemetryState>
    

    suspend fun clearTelemetryData()
}
