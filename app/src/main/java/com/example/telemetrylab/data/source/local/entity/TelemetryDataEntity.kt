package com.example.telemetrylab.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.telemetrylab.domain.model.TelemetryData
import java.util.UUID


@Entity(tableName = "telemetry_data")
data class TelemetryDataEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val frameId: Long,
    val timestamp: Long,
    val latencyMs: Float,
    val computeLoad: Int,
    val isJank: Boolean
) {

    fun toDomain(): TelemetryData {
        return TelemetryData(
            frameId = frameId,
            timestamp = timestamp,
            latencyMs = latencyMs,
            computeLoad = computeLoad,
            isJank = isJank
        )
    }

    companion object {

        fun fromDomain(data: TelemetryData): TelemetryDataEntity {
            return TelemetryDataEntity(
                frameId = data.frameId,
                timestamp = data.timestamp,
                latencyMs = data.latencyMs,
                computeLoad = data.computeLoad,
                isJank = data.isJank
            )
        }
    }
}
