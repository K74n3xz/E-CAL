package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent

@Dao
interface AlarmComponentDao {
    @Insert
    fun insert(alarm: AlarmComponent)

    @Insert
    fun insertAll(vararg alarm: AlarmComponent)

    @Upsert
    fun upsert(alarm: AlarmComponent)

    @Upsert
    fun upsertAll(vararg alarm: AlarmComponent)

    @Delete
    fun delete(alarm: AlarmComponent)

    @Query("SELECT * FROM alarm_component WHERE id = :id")
    fun queryAlarmComponentById(id: Long): AlarmComponent?

    @Query("SELECT * FROM alarm_component WHERE refUid = :eventUid")
    fun queryAlarmComponentForEventByEventUid(eventUid: String): Flow<List<AlarmComponent>>
}