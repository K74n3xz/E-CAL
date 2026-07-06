package net.k74n3xz.ecal.core.application.repository

import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Event
import java.time.ZonedDateTime

interface EventRepository {
    suspend fun getEventByUid(uid: String): Event?

    fun observeEventsOverlappingRange(
        rangeStart: ZonedDateTime,
        rangeEnd: ZonedDateTime
    ): Flow<List<Event>>

    suspend fun saveEvent(event: Event)

    suspend fun deleteEventByUid(uid: String)
}