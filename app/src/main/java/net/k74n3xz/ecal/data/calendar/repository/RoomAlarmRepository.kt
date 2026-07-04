package net.k74n3xz.ecal.data.calendar.repository

import android.util.Log
import androidx.room.withTransaction
import net.k74n3xz.ecal.data.calendar.database.CalendarDatabase
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmInstanceDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventComponentDao
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmInstance
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.data.calendar.utils.toAlarmOccurrence
import net.k74n3xz.ecal.domain.model.AlarmOccurrence
import net.k74n3xz.ecal.data.calendar.utils.calculateNextAlarmTrigger
import net.k74n3xz.ecal.data.calendar.utils.toAlarm
import net.k74n3xz.ecal.domain.model.Alarm
import net.k74n3xz.ecal.domain.repository.AlarmRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAlarmRepository @Inject constructor(
    private val calendarDatabase: CalendarDatabase,
    private val eventComponentDao: EventComponentDao,
    private val alarmComponentDao: AlarmComponentDao,
    private val alarmInstanceDao: AlarmInstanceDao,
    private val alarmDao: AlarmDao
) : AlarmRepository {
    private companion object {
        private const val TAG: String = "RoomAlarmRepository"
    }

    override suspend fun getDueAlarmOccurrenceIdsAndActions(triggerAt: Instant): List<Pair<LongArray, Alarm.Action>> =
        alarmDao.queryDueAlarmsByDesiredState(triggerAt, DesiredState.ACTIVE)
            .map { (alarmComponent, alarmInstances) ->
                alarmInstances.map { x -> x.id!! }.toLongArray() to alarmComponent.toAlarm().action
            }

    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long) {
        calendarDatabase.withTransaction {
            alarmInstanceDao.updateLastReconcileResultById(
                id = alarmOccurrenceId,
                lastReconcileResult = ReconcileResult.CANCELLED
            )
            alarmInstanceDao.updateDesiredStateById(alarmOccurrenceId, DesiredState.INACTIVE)
            alarmInstanceDao.queryAlarmComponentIdById(alarmOccurrenceId)
                ?.let { instantiateNextAlarmInstanceUnsafely(it) }
        }
    }

    override suspend fun getAlarmOccurrenceNeedingReconciliation(): Pair<List<AlarmOccurrence>, List<AlarmOccurrence>> =
        calendarDatabase.withTransaction {
            alarmInstanceDao
                .queryAlarmInstancesNeedingReconciliation(
                    desiredState = DesiredState.INACTIVE,
                    excludedReconcileResult = ReconcileResult.CANCELLED
                )
                .map { it.toAlarmOccurrence() } to alarmInstanceDao
                .queryAlarmInstancesNeedingReconciliation(
                    desiredState = DesiredState.ACTIVE,
                    excludedReconcileResult = ReconcileResult.SCHEDULED
                )
                .map { it.toAlarmOccurrence() }
        }

    override suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long) {
        alarmInstanceDao.updateLastReconcileResultById(alarmOccurrenceId, ReconcileResult.CANCELLED)
    }

    override suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long) {
        alarmInstanceDao.updateLastReconcileResultById(alarmOccurrenceId, ReconcileResult.SCHEDULED)
    }

    override suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long) {
        alarmInstanceDao.updateLastReconcileResultById(alarmOccurrenceId, ReconcileResult.UNKNOWN)
    }

    override suspend fun markAllAlarmOccurrencesAsCancelled() {
        alarmInstanceDao.updateLastReconcileResult(ReconcileResult.CANCELLED)
    }

    private suspend fun instantiateNextAlarmInstanceUnsafely(alarmComponentId: Long) {
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

        val interval = alarmComponent.interval
        val repeat = alarmComponent.repeat
        if (interval != null && repeat != null) {
            val instancesCount = alarmInstanceDao.countByAlarmComponentId(alarmComponentId)

            calculateNextAlarmTrigger(firstTriggerTime, interval, instancesCount, repeat)?.let {
                alarmInstanceDao.insert(
                    AlarmInstance(
                        id = null,
                        alarmComponentId = alarmComponentId,
                        triggerAt = it,
                        desiredState = DesiredState.ACTIVE,
                        lastReconcileResult = ReconcileResult.CANCELLED
                    )
                )
            }
        } else if (!(interval == null && repeat == null)) {
            Log.w(
                TAG,
                "instantiateNextAlarmInstanceUnsafely: AlarmComponent(id=${alarmComponent.id}) has incomplete `interval`(=${alarmComponent.interval}) and `repeat`(=${alarmComponent.repeat}). May data in database is broken?"
            )
        }
    }
}