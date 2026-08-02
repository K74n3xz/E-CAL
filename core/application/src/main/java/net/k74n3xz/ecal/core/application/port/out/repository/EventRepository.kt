package net.k74n3xz.ecal.core.application.port.out.repository

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Event

interface EventRepository {
    suspend fun getEventByUid(uid: String): Event?
    fun observeEventsOverlappingRange(start: ZonedDateTime, end: ZonedDateTime): Flow<List<Event>>
    suspend fun saveEvent(event: Event, timeZone: ZoneId)
    suspend fun deleteEventByUid(uid: String)
}
