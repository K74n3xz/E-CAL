package net.k74n3xz.ecal.core.data.repository

import androidx.room.withTransaction
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.core.application.port.out.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.out.repository.result.AlarmOccurrenceNeedingReconciliation
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.dao.AlarmDao
import net.k74n3xz.ecal.core.data.database.dao.AlarmOccurrenceDao
import net.k74n3xz.ecal.core.data.database.dao.EventDao
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.data.utils.calculateNextAlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.utils.toAlarm
import net.k74n3xz.ecal.core.data.utils.toAlarmOccurrence
import net.k74n3xz.ecal.core.model.property.alarm.Action

@Singleton
internal class AlarmRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val eventDao: EventDao,
    private val alarmDao: AlarmDao,
    private val alarmOccurrenceDao: AlarmOccurrenceDao
) : AlarmRepository {
    override suspend fun getAlarmOccurrenceNeedingReconciliation(): AlarmOccurrenceNeedingReconciliation =
        appDatabase.withTransaction {
            AlarmOccurrenceNeedingReconciliation(
                alarmOccurrenceDao.queryAlarmOccurrencesNeedingReconciliation(
                    desiredState = DesiredState.INACTIVE,
                    excludedReconcileResult = ReconcileResult.CANCELLED
                ).map { it.toAlarmOccurrence() },
                alarmOccurrenceDao.queryAlarmOccurrencesNeedingReconciliation(
                    desiredState = DesiredState.ACTIVE,
                    excludedReconcileResult = ReconcileResult.SCHEDULED
                ).map { it.toAlarmOccurrence() }
            )
        }

    override suspend fun getDueAlarmOccurrenceIdsAndActions(now: Instant): List<Pair<LongArray, Action>> =
        alarmOccurrenceDao.queryDueAlarmsByDesiredState(now, DesiredState.ACTIVE)
            .map { (completeAlarmEntity, alarmOccurrenceEntities) ->
                alarmOccurrenceEntities.map { x -> x.id!! }.toLongArray() to completeAlarmEntity.toAlarm().action
            }

    override suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long) {
        alarmOccurrenceDao.updateLastReconcileResultById(alarmOccurrenceId, ReconcileResult.CANCELLED)
    }

    override suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long) {
        alarmOccurrenceDao.updateLastReconcileResultById(alarmOccurrenceId, ReconcileResult.SCHEDULED)
    }

    override suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long) {
        alarmOccurrenceDao.updateLastReconcileResultById(alarmOccurrenceId, ReconcileResult.UNKNOWN)
    }

    override suspend fun markAllAlarmOccurrencesAsCancelled() {
        alarmOccurrenceDao.updateLastReconcileResult(ReconcileResult.CANCELLED)
    }

    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long, hasHandled: Boolean, timeZone: ZoneId) {
        appDatabase.withTransaction {
            alarmOccurrenceDao.updateDesiredStateById(alarmOccurrenceId, DesiredState.INACTIVE)
            alarmOccurrenceDao.updateLastReconcileResultById(
                id = alarmOccurrenceId,
                lastReconcileResult = ReconcileResult.CANCELLED
            )
            alarmOccurrenceDao.queryAlarmById(alarmOccurrenceId)?.let {
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

        if (!hasHandled) {
            return // TODO: Retry to deliver later.
        }
    }

    override suspend fun rebuildAllDayAlarmOccurrences(timeZone: ZoneId) {
        appDatabase.withTransaction {
            alarmDao.queryAlarmIdsForAllDayEventsByTriggerTypeAndDesiredState(TriggerType.RELATIVE, DesiredState.ACTIVE)
                .forEach { alarmId ->
                    val alarmEntity = alarmDao.queryById(alarmId)!!
                    val currentAlarmOccurrenceIndex = alarmOccurrenceDao.queryLastIndexByAlarmId(alarmId)!!
                    alarmOccurrenceDao.updateDesiredStateByAlarmId(alarmId, DesiredState.INACTIVE)
                    alarmOccurrenceDao.unlinkAlarmFromAlarmOccurrenceByAlarmId(alarmId)
                    calculateNextAlarmOccurrenceEntity(
                        alarmEntity = alarmEntity,
                        referenceEntity = eventDao.queryByUid(alarmEntity.refUid)!!,
                        lastAlarmOccurrenceIndex = if (currentAlarmOccurrenceIndex == 0L) {
                            null
                        } else {
                            currentAlarmOccurrenceIndex - 1
                        },
                        timeZone = timeZone
                    )?.let { alarmOccurrenceDao.insert(it) }
                }
        }
    }
}
