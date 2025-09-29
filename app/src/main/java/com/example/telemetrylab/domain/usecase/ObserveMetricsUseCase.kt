package com.example.telemetrylab.domain.usecase

import com.example.telemetrylab.domain.model.TelemetryMetrics
import com.example.telemetrylab.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class ObserveMetricsUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {

    operator fun invoke(windowSize: Int = 30): Flow<TelemetryMetrics> {
        return repository.observeMetrics(windowSize)
    }
}
