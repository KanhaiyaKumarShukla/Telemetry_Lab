package com.example.telemetrylab.domain.usecase

import com.example.telemetrylab.domain.model.TelemetryState
import com.example.telemetrylab.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class ObserveTelemetryStateUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {

    operator fun invoke(): Flow<TelemetryState> {
        return repository.observeTelemetryState()
    }
}
