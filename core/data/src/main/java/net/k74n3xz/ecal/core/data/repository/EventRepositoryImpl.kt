package net.k74n3xz.ecal.core.data.repository

import android.util.Log
import androidx.room.withTransaction
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import net.k74n3xz.ecal.core.application.port.outbound.repository.EventRepository
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.dao.AlarmDao
import net.k74n3xz.ecal.core.data.database.dao.AlarmOccurrenceDao
import net.k74n3xz.ecal.core.data.database.dao.EventDao
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.ActionType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttachmentCrossRef
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttendeeCrossRef
import net.k74n3xz.ecal.core.data.utils.calculateNextAlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.utils.toAlarmEntity
import net.k74n3xz.ecal.core.data.utils.toEvent
import net.k74n3xz.ecal.core.data.utils.toEventEntity
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action

@Singleton
internal class EventRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val eventDao: EventDao,
    private val alarmDao: AlarmDao,
    private val alarmOccurrenceDao: AlarmOccurrenceDao
) : EventRepository {
    private companion object {
        private const val TAG: String = "EventRepositoryImpl"
    }

    override suspend fun getEventByUid(uid: String): Event? = eventDao.queryEventWithAlarmsByEventUid(uid)?.toEvent()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEventsOverlappingRange(start: ZonedDateTime, end: ZonedDateTime): Flow<List<Event>> =
        eventDao.observeEventWithAlarmsOverlappingIn(
            start.toInstant(),
            end.toInstant(),
            start.toLocalDate(),
            end.toLocalDate()
        ).mapLatest { list -> list.map { it.toEvent() } }

    override suspend fun saveEvent(event: Event, timeZone: ZoneId) {
        appDatabase.withTransaction {
            eventDao.upsert(event.toEventEntity())
            applyAlarmsForReferenceUnsafely(event.uid, event.alarms, timeZone)
        }
    }

    override suspend fun deleteEventByUid(uid: String) {
        appDatabase.withTransaction {
            alarmDao.queryIdsByRefUid(uid).forEach { deleteAlarmByIdUnsafely(it) }
            eventDao.deleteByUid(uid)
        }
    }

    private suspend fun applyAlarmsForReferenceUnsafely(referenceUid: String, alarms: List<Alarm>, timeZone: ZoneId) {
        val existingAlarmIds = alarmDao.queryIdsByRefUid(referenceUid).toSet()
        val alarmIdsNotNull = alarms.mapNotNull { it.id }.toSet()

        val alarmToUpdateIds = existingAlarmIds intersect alarmIdsNotNull
        val alarmToDeleteIds = existingAlarmIds - alarmIdsNotNull
        if ((alarmIdsNotNull - existingAlarmIds).isNotEmpty()) {
            Log.w(TAG, "applyAlarmsForReferenceUnsafely: New alarms whose id isn't null are ignored.")
        }
        alarms.filter { it.id == null }.forEach { insertAlarmUnsafely(referenceUid, timeZone, it) }
        alarms.filter { it.id in alarmToUpdateIds }.forEach { updateAlarmUnsafely(it, timeZone) }
        alarmToDeleteIds.forEach { deleteAlarmByIdUnsafely(it) }
    }

    private suspend fun insertAlarmUnsafely(referenceUid: String, timeZone: ZoneId, vararg alarms: Alarm) {
        alarms.forEach {
            val id = alarmDao.queryIdByRowId(alarmDao.insert(it.toAlarmEntity(referenceUid)).single())!!
            insertAlarmActionRelatedRecordUnsafely(it.copy(id = id))
            instantiateFirstAlarmOccurrenceUnsafely(id, timeZone)
        }
    }

    private suspend fun updateAlarmUnsafely(alarm: Alarm, timeZone: ZoneId) {
        val alarmId = alarm.id!!

        val oldAlarmEntity = alarmDao.queryById(alarmId)!!
        val newAlarmEntity = alarm.toAlarmEntity(oldAlarmEntity.refUid)
        alarmOccurrenceDao.updateDesiredStateByAlarmId(
            alarmId = alarmId,
            desiredState = DesiredState.INACTIVE
        )
        alarmOccurrenceDao.unlinkAlarmFromAlarmOccurrenceByAlarmId(alarmId)
        cleanupAlarmActionRelatedRecordUnsafely(alarmId)
        alarmDao.update(newAlarmEntity)
        insertAlarmActionRelatedRecordUnsafely(alarm)
        instantiateFirstAlarmOccurrenceUnsafely(alarmId, timeZone)
    }

    private suspend fun deleteAlarmByIdUnsafely(alarmId: Long) {
        alarmOccurrenceDao.updateDesiredStateByAlarmId(
            alarmId = alarmId,
            desiredState = DesiredState.INACTIVE
        )
        alarmOccurrenceDao.unlinkAlarmFromAlarmOccurrenceByAlarmId(alarmId)
        cleanupAlarmActionRelatedRecordUnsafely(alarmId)
        alarmDao.deleteById(alarmId)
    }

    private suspend fun insertAlarmActionRelatedRecordUnsafely(alarm: Alarm) {
        when (val action = alarm.action) {
            is Action.Audio -> action.attach?.let { attachment ->
                alarmDao.insertRelatedAttachmentRecord(
                    AlarmAttachmentCrossRef(
                        requireNotNull(alarm.id),
                        requireNotNull(attachment.id)
                    )
                )
            }

            is Action.Display -> Unit

            is Action.Email -> {
                action.attendee.forEach {
                    alarmDao.insertRelatedAttendeeRecord(
                        AlarmAttendeeCrossRef(
                            requireNotNull(alarm.id),
                            requireNotNull(it.id)
                        )
                    )
                }
                action.attach?.let { attachments ->
                    attachments.forEach {
                        alarmDao.insertRelatedAttachmentRecord(
                            AlarmAttachmentCrossRef(
                                requireNotNull(alarm.id),
                                requireNotNull(it.id)
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun cleanupAlarmActionRelatedRecordUnsafely(alarmId: Long) {
        val alarmEntity = alarmDao.queryById(alarmId)!!
        when (alarmEntity.action) {
            ActionType.AUDIO -> alarmDao.deleteRelatedAttachmentRecordByAlarmId(alarmId)

            ActionType.DISPLAY -> Unit

            ActionType.EMAIL -> {
                alarmDao.deleteRelatedAttachmentRecordByAlarmId(alarmId)
                alarmDao.deleteRelatedAttendeeRecordByAlarmId(alarmId)
            }
        }
    }

    private suspend fun instantiateFirstAlarmOccurrenceUnsafely(alarmId: Long, timeZone: ZoneId) {
        alarmDao.queryById(alarmId)!!.let {
            calculateNextAlarmOccurrenceEntity(
                alarmEntity = it,
                referenceEntity = eventDao.queryByUid(it.refUid)!!,
                lastAlarmOccurrenceIndex = alarmOccurrenceDao.queryLastIndexByAlarmId(it.id!!),
                timeZone = timeZone
            )
        }?.let {
            alarmOccurrenceDao.insert(it)
        }
    }
}
