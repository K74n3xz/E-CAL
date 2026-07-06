package net.k74n3xz.ecal.core.database.calendar.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmComponent

@Dao
internal interface AlarmComponentDao {
    @Insert
    suspend fun insert(vararg alarmComponents: AlarmComponent): LongArray

    @Update
    suspend fun update(vararg alarmComponents: AlarmComponent)

    @Upsert
    suspend fun upsert(vararg alarmComponents: AlarmComponent)

    @Delete
    suspend fun delete(vararg alarmComponents: AlarmComponent)

    @Query("DELETE FROM alarm_component WHERE id = :alarmComponentIds")
    suspend fun deleteById(vararg alarmComponentIds: Long)

    @Query("SELECT * FROM alarm_component WHERE id = :id")
    suspend fun queryById(id: Long): AlarmComponent

    @Query("SELECT id FROM alarm_component WHERE refUid = :refUid")
    suspend fun queryIdsByRefUid(refUid: String): LongArray
}