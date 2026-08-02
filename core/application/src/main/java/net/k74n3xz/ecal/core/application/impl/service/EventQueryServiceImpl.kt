package net.k74n3xz.ecal.core.application.impl.service

import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.application.port.inbound.service.EventQueryService
import net.k74n3xz.ecal.core.application.port.outbound.repository.EventRepository
import net.k74n3xz.ecal.core.model.Event

internal class EventQueryServiceImpl(private val eventRepository: EventRepository) : EventQueryService {
    override suspend fun findEventByUid(uid: String): Event? = eventRepository.getEventByUid(uid)

    override fun observeEventsOverlappingRange(rangeStart: ZonedDateTime, rangeEnd: ZonedDateTime): Flow<List<Event>> =
        eventRepository.observeEventsOverlappingRange(rangeStart, rangeEnd)
}
