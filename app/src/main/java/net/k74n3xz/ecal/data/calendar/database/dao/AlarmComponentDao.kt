package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent

@Dao
interface AlarmComponentDao {
    @Insert
    fun insert(vararg alarmComponents: AlarmComponent): LongArray

    @Update
    fun update(vararg alarmComponents: AlarmComponent)

    @Upsert
    fun upsert(vararg alarmComponents: AlarmComponent)

    @Delete
    fun delete(vararg alarmComponents: AlarmComponent)

    @Query("DELETE FROM alarm_component WHERE id = :alarmComponentIds")
    fun deleteById(vararg alarmComponentIds: Long)

    @Query("SELECT * FROM alarm_component WHERE id = :id")
    fun queryAlarmComponentById(id: Long): AlarmComponent

    @Query("SELECT * FROM alarm_component WHERE refUid = :refUid")
    fun queryAlarmComponentsByRefUid(refUid: String): List<AlarmComponent>

    @Query("SELECT id FROM alarm_component WHERE refUid = :refUid")
    fun queryAlarmComponentIdsByRefUid(refUid: String): LongArray
}