package com.example.telemetrylab.data.source.local

import com.example.telemetrylab.data.source.local.dao.TelemetryDao
import com.example.telemetrylab.data.source.local.entity.TelemetryDataEntity
import com.example.telemetrylab.data.source.local.entity.TelemetryStateEntity
import com.example.telemetrylab.domain.model.TelemetryData
import com.example.telemetrylab.domain.model.TelemetryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TelemetryLocalDataSourceImpl @Inject constructor(
    private val telemetryDao: TelemetryDao
) : TelemetryLocalDataSource {

    override suspend fun saveTelemetryData(data: TelemetryData) = withContext(Dispatchers.IO) {
        telemetryDao.insertTelemetryData(TelemetryDataEntity.fromDomain(data))
    }

    override suspend fun getLatestTelemetryData(limit: Int): List<TelemetryData> = withContext(Dispatchers.IO) {
        telemetryDao.getLatestTelemetryData(limit).map { entity -> entity.toDomain() }
    }

    override fun observeTelemetryData(): Flow<List<TelemetryData>> {
        return telemetryDao.observeTelemetryData()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTelemetryState(): TelemetryState = withContext(Dispatchers.IO) {
        telemetryDao.getTelemetryState()?.toDomain() ?: TelemetryState.initial()
    }

    override suspend fun updateTelemetryState(update: (TelemetryState) -> TelemetryState) = withContext(Dispatchers.IO) {
        val currentState = getTelemetryState()
        val updatedState = update(currentState)
        telemetryDao.insertTelemetryState(TelemetryStateEntity.fromDomain(updatedState))
    }

    override fun observeTelemetryState(): Flow<TelemetryState> {
        return telemetryDao.observeTelemetryState()
            .map { it?.toDomain() ?: TelemetryState.initial() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun clearTelemetryData() = withContext(Dispatchers.IO) {
        telemetryDao.clearTelemetryData()
    }
}
