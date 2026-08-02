package net.k74n3xz.ecal.core.application.port.`in`.service

import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Event

interface EventQueryService {
    suspend fun findEventByUid(uid: String): Event?
    fun observeEventsOverlappingRange(rangeStart: ZonedDateTime, rangeEnd: ZonedDateTime): Flow<List<Event>>
}
