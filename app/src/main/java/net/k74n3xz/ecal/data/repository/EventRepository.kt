package net.k74n3xz.ecal.data.repository

import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.data.AppDatabase
import net.k74n3xz.ecal.data.dao.EventDao
import net.k74n3xz.ecal.data.dao.EventWithAlarmsDao
import net.k74n3xz.ecal.data.entity.Alarm
import net.k74n3xz.ecal.data.entity.Event
import net.k74n3xz.ecal.util.atEndOfDay
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class EventRepository private constructor(
    private val eventDao: EventDao,
    private val eventWithAlarmsDao: EventWithAlarmsDao
) {
    companion object {
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getRepository(database: AppDatabase): EventRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository(
                    database.eventDao(),
                    database.eventWithAlarmsDao()
                ).also { INSTANCE = it }
            }
        }
    }

    fun getEventOverlappingInCloseRange(
        from: ZonedDateTime,
        to: ZonedDateTime
    ): Flow<Array<Event>> {
        return eventDao.getEventOverlappingInCloseRange(from.toInstant(), to.toInstant())
    }

    fun getEventCoveringDate(localDate: LocalDate, zoneId: ZoneId): Flow<Array<Event>> {
        return eventDao.getEventOverlappingInCloseRange(
            localDate.atStartOfDay(zoneId).toInstant(),
            localDate.atEndOfDay(zoneId).toInstant()
        )
    }

    fun upsertEvent(event: Event) {
        eventDao.upsert(event)
    }

    fun deleteEvent(event: Event) {
        eventDao.delete(event)
    }

    fun upsertEventWithAlarms(event: Event, alarms: Array<Alarm>) {
        eventWithAlarmsDao.upsertEventWithAlarms(event, alarms)
    }
}