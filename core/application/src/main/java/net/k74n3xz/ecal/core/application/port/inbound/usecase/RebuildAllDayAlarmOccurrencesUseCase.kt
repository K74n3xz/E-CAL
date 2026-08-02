package net.k74n3xz.ecal.core.application.port.inbound.usecase

import java.time.ZoneId

fun interface RebuildAllDayAlarmOccurrencesUseCase {
    suspend operator fun invoke(timeZone: ZoneId)
}
