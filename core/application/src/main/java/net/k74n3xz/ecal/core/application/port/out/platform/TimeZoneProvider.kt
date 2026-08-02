package net.k74n3xz.ecal.core.application.port.out.platform

import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface TimeZoneProvider {
    val timeZone: Flow<ZoneId>

    suspend fun currentTimeZone(): ZoneId = timeZone.first()
}
