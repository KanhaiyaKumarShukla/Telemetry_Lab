package com.example.telemetrylab.domain.usecase

import com.example.telemetrylab.domain.repository.TelemetryRepository
import javax.inject.Inject


class UpdateComputeLoadUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {

    suspend operator fun invoke(computeLoad: Int) {
        repository.updateComputeLoad(computeLoad)
    }
}
