package net.k74n3xz.ecal.core.application.port.`in`.usecase

import java.time.Instant

interface HandleDueAlarmsUseCase {
    suspend operator fun invoke(now: Instant)
}
