package com.example.telemetrylab.data.source.local.dao

import androidx.room.*
import com.example.telemetrylab.data.source.local.entity.TelemetryDataEntity
import com.example.telemetrylab.data.source.local.entity.TelemetryStateEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetryData(data: TelemetryDataEntity)

    @Query("SELECT * FROM telemetry_data ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestTelemetryData(limit: Int): List<TelemetryDataEntity>

    @Query("SELECT * FROM telemetry_data ORDER BY timestamp DESC")
    fun observeTelemetryData(): Flow<List<TelemetryDataEntity>>

    @Query("DELETE FROM telemetry_data")
    suspend fun clearTelemetryData()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetryState(state: TelemetryStateEntity)

    @Query("SELECT * FROM telemetry_state LIMIT 1")
    suspend fun getTelemetryState(): TelemetryStateEntity?

    @Query("SELECT * FROM telemetry_state LIMIT 1")
    fun observeTelemetryState(): Flow<TelemetryStateEntity?>

    @Query("DELETE FROM telemetry_state")
    suspend fun clearTelemetryState()
}
