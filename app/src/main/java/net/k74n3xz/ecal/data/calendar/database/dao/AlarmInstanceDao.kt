package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmInstance
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.ReconcileResult

@Dao
interface AlarmInstanceDao {
    @Insert
    fun insert(vararg alarmComponents: AlarmInstance)

    @Update
    fun update(vararg alarmComponents: AlarmInstance)

    @Query("UPDATE alarm_instance SET alarmComponentId = NULL WHERE alarmComponentId = :alarmComponentId")
    fun unlinkAlarmComponentFromAlarmInstanceByAlarmComponentId(alarmComponentId: Long)

    @Query("UPDATE alarm_instance SET desiredState = :desiredState WHERE id = :id")
    fun updateDesiredStateById(id: Long, desiredState: DesiredState)

    @Query("UPDATE alarm_instance SET desiredState = :desiredState WHERE alarmComponentId = :alarmComponentId")
    fun updateDesiredStateByAlarmComponentId(alarmComponentId: Long, desiredState: DesiredState)

    @Query("UPDATE alarm_instance SET lastReconcileResult = :lastReconcileResult WHERE id = :id")
    fun updateLastReconcileResultById(id: Long, lastReconcileResult: ReconcileResult)

    @Query("UPDATE alarm_instance SET lastReconcileResult = :lastReconcileResult")
    fun updateLastReconcileResult(lastReconcileResult: ReconcileResult)

    @Upsert
    fun upsert(vararg alarmComponents: AlarmInstance)

    @Delete
    fun delete(vararg alarmComponents: AlarmInstance)

    @Query("SELECT * FROM alarm_instance WHERE desiredState = :desiredState AND lastReconcileResult != :excludedReconcileResult")
    fun queryAlarmInstancesNeedingReconciliation(
        desiredState: DesiredState,
        excludedReconcileResult: ReconcileResult
    ): List<AlarmInstance>
}