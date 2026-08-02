package net.k74n3xz.ecal.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import java.time.Instant
import net.k74n3xz.ecal.core.data.database.dao.result.AlarmWithAttachmentsAndAttendees
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult

@Dao
internal interface AlarmOccurrenceDao {
    @Insert
    suspend fun insert(vararg alarmOccurrenceEntities: AlarmOccurrenceEntity)

    @Update
    suspend fun update(vararg alarmOccurrenceEntities: AlarmOccurrenceEntity)

    @Query("UPDATE alarm_occurrence SET alarmId = NULL WHERE alarmId = :alarmId")
    suspend fun unlinkAlarmFromAlarmOccurrenceByAlarmId(alarmId: Long)

    @Query("UPDATE alarm_occurrence SET desiredState = :desiredState WHERE id = :id")
    suspend fun updateDesiredStateById(id: Long, desiredState: DesiredState)

    @Query("UPDATE alarm_occurrence SET desiredState = :desiredState WHERE alarmId = :alarmId")
    suspend fun updateDesiredStateByAlarmId(alarmId: Long, desiredState: DesiredState)

    @Query("UPDATE alarm_occurrence SET lastReconcileResult = :lastReconcileResult WHERE id = :id")
    suspend fun updateLastReconcileResultById(id: Long, lastReconcileResult: ReconcileResult)

    @Query("UPDATE alarm_occurrence SET lastReconcileResult = :lastReconcileResult")
    suspend fun updateLastReconcileResult(lastReconcileResult: ReconcileResult)

    @Upsert
    suspend fun upsert(vararg alarmOccurrenceEntities: AlarmOccurrenceEntity)

    @Delete
    suspend fun delete(vararg alarmOccurrenceEntities: AlarmOccurrenceEntity)

    @Query(
        """
        SELECT *
        FROM alarm_occurrence
        WHERE desiredState = :desiredState
            AND lastReconcileResult != :excludedReconcileResult
        """
    )
    suspend fun queryAlarmOccurrencesNeedingReconciliation(
        desiredState: DesiredState,
        excludedReconcileResult: ReconcileResult
    ): List<AlarmOccurrenceEntity>

    @Query(
        """
        SELECT alarm.*
        FROM alarm
        JOIN alarm_occurrence ON alarm.id = alarm_occurrence.alarmId
        WHERE alarm_occurrence.id = :id
        """
    )
    suspend fun queryAlarmById(id: Long): AlarmEntity?

    @Query("SELECT MAX(`index`) FROM alarm_occurrence WHERE alarmId = :alarmId")
    suspend fun queryLastIndexByAlarmId(alarmId: Long): Long?

    @Transaction
    @Query(
        """
        SELECT *
        FROM alarm
        JOIN alarm_occurrence ON alarm.id = alarm_occurrence.alarmId
        WHERE alarm_occurrence.triggerAt <= :triggerAt
            AND alarm_occurrence.desiredState = :desiredState
        """
    )
    suspend fun queryDueAlarmsByDesiredState(
        triggerAt: Instant,
        desiredState: DesiredState
    ): Map<AlarmWithAttachmentsAndAttendees, List<AlarmOccurrenceEntity>>
}
