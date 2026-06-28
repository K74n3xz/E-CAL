package net.k74n3xz.ecal.data.calendar.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.k74n3xz.ecal.data.calendar.database.CalendarDatabase
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmInstanceDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventComponentDao
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmInstance
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarmcomponent.TriggerRelationship
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarmcomponent.TriggerType
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.data.calendar.model.Alarm
import net.k74n3xz.ecal.data.calendar.utils.toAlarm
import net.k74n3xz.ecal.data.calendar.utils.toAlarmComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val calendarDatabase: CalendarDatabase,
    private val eventComponentDao: EventComponentDao,
    private val alarmComponentDao: AlarmComponentDao,
    private val alarmInstanceDao: AlarmInstanceDao,
    private val alarmDao: AlarmDao
) {
    companion object {
        private const val TAG: String = "AlarmRepository"
    }

    private fun insertAlarmByIdUnsafely(alarm: Alarm) {
        alarmComponentDao.insert(alarm.toAlarmComponent())
            .forEach {
                instantiateAlarmComponentUnsafely(alarmComponentDao.queryAlarmComponentByRowId(it))
            }
    }

    private fun updateAlarmByIdUnsafely(alarm: Alarm) {
        val alarmId = alarm.id!!

        val alarmComponent =
            alarm.toAlarmComponent(alarmComponentDao.queryRawIcsContentById(alarmId))
        alarmInstanceDao.updateDesiredStateByAlarmComponentId(
            alarmComponentId = alarmId,
            desiredState = DesiredState.INACTIVE
        )
        alarmComponentDao.update(alarmComponent)
        instantiateAlarmComponentUnsafely(alarmComponent)
    }

    private fun deleteAlarmByIdUnsafely(alarmId: Long) {
        alarmInstanceDao.updateDesiredStateByAlarmComponentId(
            alarmComponentId = alarmId,
            desiredState = DesiredState.INACTIVE
        )
        alarmInstanceDao.unlinkAlarmComponentFromAlarmInstanceByAlarmComponentId(alarmId)
        alarmComponentDao.deleteById(alarmId)
    }

    private fun instantiateAlarmComponentUnsafely(alarmComponent: AlarmComponent) {
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
        if (interval == null && repeat == null) {
            alarmInstanceDao.insert(
                AlarmInstance(
                    id = null,
                    alarmComponentId = alarmComponent.id!!,
                    triggerAt = firstTriggerTime,
                    desiredState = DesiredState.ACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            )
        } else {
            assert(interval != null && repeat != null)

            var triggerTime = firstTriggerTime
            // TODO: Bound repeat counts before materializing instances to prevent excessive database writes.
            for (i in 0..repeat!!) {
                alarmInstanceDao.insert(
                    AlarmInstance(
                        id = null,
                        alarmComponentId = alarmComponent.id!!,
                        triggerAt = triggerTime,
                        desiredState = if (i == 0) DesiredState.ACTIVE else DesiredState.INACTIVE,
                        lastReconcileResult = ReconcileResult.CANCELLED
                    )
                )
                triggerTime = triggerTime.plus(interval!!)
            }
        }
    }

    fun getAlarmDescriptionByAlarmInstanceId(alarmInstanceId: Long): String? =
        alarmDao.queryAlarmComponentDescriptionByAlarmInstanceId(alarmInstanceId)

    fun observeAlarmForEventByEventUid(eventUid: String): Flow<List<Alarm>> =
        alarmComponentDao
            .observeAlarmComponentsByRefUid(eventUid)
            .map { it.map { alarmComponent -> alarmComponent.toAlarm() } }

    suspend fun applyAlarmsForReference(refUid: String, alarms: List<Alarm>) {
        calendarDatabase.withTransaction {
            val existingAlarmComponentIds =
                alarmComponentDao.queryAlarmComponentIdsByRefUid(refUid).toSet()
            val alarmIdsNotNull = alarms.mapNotNull { it.id }.toSet()

            val alarmComponentIdsToUpdate = existingAlarmComponentIds intersect alarmIdsNotNull
            val alarmComponentIdsToDelete = existingAlarmComponentIds - alarmIdsNotNull
            if ((alarmIdsNotNull - existingAlarmComponentIds).isNotEmpty()) {
                Log.w(TAG, "applyAlarmsForRef: New alarms whose id isn't null are ignored.")
            }
            alarms.filter { it.id == null }
                .forEach { insertAlarmByIdUnsafely(it) }
            alarms.filter { it.id in alarmComponentIdsToUpdate }
                .forEach { updateAlarmByIdUnsafely(it) }
            alarmComponentIdsToDelete.forEach { deleteAlarmByIdUnsafely(it) }
        }
    }

    suspend fun deleteAlarmsForReference(refUid: String) {
        calendarDatabase.withTransaction {
            alarmComponentDao.queryAlarmComponentIdsByRefUid(refUid)
                .forEach { deleteAlarmByIdUnsafely(it) }
        }
    }

    fun deactivateAlarmInstance(alarmInstanceId: Long) {
        alarmInstanceDao.updateDesiredStateById(alarmInstanceId, DesiredState.INACTIVE)
    }

    suspend fun getAlarmInstancesNeedingReconciliation(): Pair<List<AlarmInstance>, List<AlarmInstance>> =
        calendarDatabase.withTransaction {
            alarmInstanceDao
                .queryAlarmInstancesNeedingReconciliation(
                    desiredState = DesiredState.INACTIVE,
                    excludedReconcileResult = ReconcileResult.CANCELLED
                ) to alarmInstanceDao
                .queryAlarmInstancesNeedingReconciliation(
                    desiredState = DesiredState.ACTIVE,
                    excludedReconcileResult = ReconcileResult.SCHEDULED
                )
        }

    fun markAlarmAsCancelled(alarmInstanceId: Long) {
        alarmInstanceDao.updateLastReconcileResultById(alarmInstanceId, ReconcileResult.CANCELLED)
    }

    fun markAlarmAsScheduled(alarmInstanceId: Long) {
        alarmInstanceDao.updateLastReconcileResultById(alarmInstanceId, ReconcileResult.SCHEDULED)
    }

    fun markAllAlarmsAsCancelled() {
        alarmInstanceDao.updateLastReconcileResult(ReconcileResult.CANCELLED)
    }
}