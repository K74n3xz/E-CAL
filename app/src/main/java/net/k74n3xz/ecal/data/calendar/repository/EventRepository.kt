package net.k74n3xz.ecal.data.calendar.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.k74n3xz.ecal.data.calendar.database.dao.EventComponentDao
import net.k74n3xz.ecal.data.calendar.model.Event
import net.k74n3xz.ecal.data.calendar.utils.toEvent
import net.k74n3xz.ecal.data.calendar.utils.toEventComponent
import net.k74n3xz.ecal.utils.atEndOfDay
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(private val eventComponentDao: EventComponentDao) {
    fun getEventByUid(uid: String): Event? =
        eventComponentDao.queryEventComponentByUid(uid)?.toEvent()

    fun upsertEvent(event: Event) {
        eventComponentDao.upsert(
            event.toEventComponent(
                originalIcs = eventComponentDao.queryEventComponentByUid(event.uid)?.rawIcs
            )
        )
    }

    fun deleteEventByUid(eventUid: String) {
        eventComponentDao.deleteByUid(eventUid)
    }

    fun getEventOverlappingInCloseRange(from: ZonedDateTime, to: ZonedDateTime): Flow<List<Event>> =
        eventComponentDao
            .queryEventComponentOverlappingInCloseRange(from.toInstant(), to.toInstant())
            .map { it.map { eventComponent -> eventComponent.toEvent() } }

    fun getEventCoveringDate(localDate: LocalDate, zoneId: ZoneId): Flow<List<Event>> =
        getEventOverlappingInCloseRange(
            localDate.atStartOfDay(zoneId),
            localDate.atEndOfDay(zoneId)
        )
}