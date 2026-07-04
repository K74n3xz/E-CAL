package net.k74n3xz.ecal.data.calendar.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import net.k74n3xz.ecal.data.calendar.database.CalendarDatabase
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmInstanceDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventDao
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmInstance
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.data.calendar.utils.toAlarm
import net.k74n3xz.ecal.data.calendar.utils.toAlarmComponent
import net.k74n3xz.ecal.data.calendar.utils.toEvent
import net.k74n3xz.ecal.data.calendar.utils.toEventComponent
import net.k74n3xz.ecal.domain.model.Alarm
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.domain.repository.EventRepository
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEventRepository @Inject constructor(
    private val calendarDatabase: CalendarDatabase,
    private val eventComponentDao: EventComponentDao,
    private val alarmComponentDao: AlarmComponentDao,
    private val alarmInstanceDao: AlarmInstanceDao,
    private val eventDao: EventDao
) : EventRepository {
    private companion object {
        private const val TAG: String = "RoomEventRepository"
    }

    override suspend fun getEventByUid(uid: String): Event? =
        eventDao
            .queryEventComponentWithAlarmComponentsByEventComponentUid(uid)
            ?.let { (eventComponent, alarmComponents) ->
                eventComponent.toEvent(alarmComponents.map { alarmComponent -> alarmComponent.toAlarm() })
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEventsOverlappingRange(
        rangeStart: ZonedDateTime,
        rangeEnd: ZonedDateTime
    ): Flow<List<Event>> =
        eventDao
            .observeEventComponentWithAlarmComponentsOverlappingInCloseRange(
                rangeStart.toInstant(),
                rangeEnd.toInstant()
            )
            .mapLatest {
                it.map { (eventComponent, alarmComponents) ->
                    eventComponent.toEvent(alarmComponents.map { alarmComponent -> alarmComponent.toAlarm() })
                }
            }

    override suspend fun saveEvent(event: Event) {
        calendarDatabase.withTransaction {
            eventComponentDao.upsert(event.toEventComponent(eventComponentDao.queryRawIcsByUid(event.uid)))
            applyAlarmsForReferenceUnsafely(event.uid, event.alarms)
        }
    }

    override suspend fun deleteEventByUid(uid: String) {
        calendarDatabase.withTransaction {
            applyAlarmsForReferenceUnsafely(uid, emptyList())
            eventComponentDao.deleteByUid(uid)
        }
    }

    private suspend fun applyAlarmsForReferenceUnsafely(referenceUid: String, alarms: List<Alarm>) {
        val existingAlarmComponentIds = alarmComponentDao.queryIdsByRefUid(referenceUid).toSet()
        val alarmIdsNotNull = alarms.mapNotNull { it.id }.toSet()

        val alarmComponentIdsToUpdate = existingAlarmComponentIds intersect alarmIdsNotNull
        val alarmComponentIdsToDelete = existingAlarmComponentIds - alarmIdsNotNull
        if ((alarmIdsNotNull - existingAlarmComponentIds).isNotEmpty()) {
            Log.w(
                TAG,
                "applyAlarmsForReferenceUnsafely: New alarms whose id isn't null are ignored."
            )
        }
        alarms.filter { it.id == null }.forEach { insertAlarmUnsafely(referenceUid, it) }
        alarms.filter { it.id in alarmComponentIdsToUpdate }.forEach { updateAlarmUnsafely(it) }
        alarmComponentIdsToDelete.forEach { deleteAlarmByIdUnsafely(it) }
    }

    private suspend fun insertAlarmUnsafely(referenceUid: String, vararg alarms: Alarm) {
        alarmComponentDao.insert(*alarms.map { it.toAlarmComponent(referenceUid) }.toTypedArray())
            .forEach { instantiateFirstAlarmInstanceUnsafely(it) }
    }

    private suspend fun updateAlarmUnsafely(alarm: Alarm) {
        val alarmId = alarm.id!!

        val oldAlarmComponent = alarmComponentDao.queryById(alarmId)
        val newAlarmComponent =
            alarm.toAlarmComponent(oldAlarmComponent.refUid, oldAlarmComponent.rawIcs)
        alarmInstanceDao.updateDesiredStateByAlarmComponentId(
            alarmComponentId = alarmId,
            desiredState = DesiredState.INACTIVE
        )
        alarmInstanceDao.unlinkAlarmComponentFromAlarmInstanceByAlarmComponentId(alarmId)
        alarmComponentDao.update(newAlarmComponent)
        instantiateFirstAlarmInstanceUnsafely(alarmId)
    }

    private suspend fun deleteAlarmByIdUnsafely(alarmId: Long) {
        alarmInstanceDao.updateDesiredStateByAlarmComponentId(
            alarmComponentId = alarmId,
            desiredState = DesiredState.INACTIVE
        )
        alarmInstanceDao.unlinkAlarmComponentFromAlarmInstanceByAlarmComponentId(alarmId)
        alarmComponentDao.deleteById(alarmId)
    }

    private suspend fun instantiateFirstAlarmInstanceUnsafely(alarmComponentId: Long) {
        val alarmComponent = alarmComponentDao.queryById(alarmComponentId)

        val firstTriggerTime = when (alarmComponent.triggerType) {
            TriggerType.RELATIVE -> {
                val ref = eventComponentDao.queryByUid(alarmComponent.refUid)!!
                // TODO: Generalize reference lookup before alarms can target components other than events.
                when (alarmComponent.triggerRelativeTo!!) {
                    TriggerRelationship.START -> ref.startAt
                    TriggerRelationship.END -> ref.endAt ?: ref.startAt
                    // TODO: Normalize all-day event boundaries before calculating relative trigger times.
                }.plus(alarmComponent.triggerOffset!!)
            }

            TriggerType.ABSOLUTE ->
                alarmComponent.triggerAt!!
        }

        alarmInstanceDao.insert(
            AlarmInstance(
                id = null,
                alarmComponentId = alarmComponent.id!!,
                triggerAt = firstTriggerTime,
                desiredState = DesiredState.ACTIVE,
                lastReconcileResult = ReconcileResult.CANCELLED
            )
        )
    }
}