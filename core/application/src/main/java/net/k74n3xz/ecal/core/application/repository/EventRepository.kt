package net.k74n3xz.ecal.core.application.repository

import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Event

interface EventRepository {
    suspend fun getEventByUid(uid: String): Event?

    fun observeEventsOverlappingRange(rangeStart: ZonedDateTime, rangeEnd: ZonedDateTime): Flow<List<Event>>

    suspend fun saveEvent(event: Event)

    suspend fun deleteEventByUid(uid: String)
}
