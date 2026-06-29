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
import net.k74n3xz.ecal.domain.repository.AlarmRepository
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
    companion object {
        private const val TAG: String = "RoomAlarmRepository"
    }

    override suspend fun getAlarmDescriptionByAlarmOccurrenceId(alarmOccurrenceId: Long): String? =
        alarmDao.queryAlarmComponentDescriptionByAlarmInstanceId(alarmOccurrenceId)

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

    override suspend fun markAllAlarmOccurrencesAsCancelled() {
        alarmInstanceDao.updateLastReconcileResult(ReconcileResult.CANCELLED)
    }

    override suspend fun handleAlarmOccurrenceRinging(alarmOccurrenceId: Long) {
        calendarDatabase.withTransaction {
            alarmInstanceDao.updateDesiredStateById(alarmOccurrenceId, DesiredState.INACTIVE)
            alarmInstanceDao.queryAlarmComponentIdByAlarmInstanceId(alarmOccurrenceId)
                ?.let { instantiateNextAlarmInstanceUnsafely(it) }
        }
    }

    private fun instantiateNextAlarmInstanceUnsafely(alarmComponentId: Long) {
        val alarmComponent = alarmComponentDao.queryAlarmComponentById(alarmComponentId)

        val firstTriggerTime = when (alarmComponent.triggerType) {
            TriggerType.RELATIVE -> {
                val ref = eventComponentDao.queryEventComponentByUid(alarmComponent.refUid)!!
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