package com.example.telemetrylab.domain.usecase

import com.example.telemetrylab.domain.repository.TelemetryRepository
import javax.inject.Inject


class StopTelemetryUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {

    suspend operator fun invoke() {
        repository.stopTelemetry()
    }
}
