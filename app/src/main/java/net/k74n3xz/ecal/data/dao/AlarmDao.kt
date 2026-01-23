package net.k74n3xz.ecal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import net.k74n3xz.ecal.data.entity.Alarm

@Dao
interface AlarmDao {
    @Insert
    fun insert(alarm: Alarm)

    @Insert
    fun insertAll(vararg alarm: Alarm)

    @Upsert
    fun upsert(alarm: Alarm)

    @Upsert
    fun upsertAll(vararg alarm: Alarm)

    @Delete
    fun delete(alarm: Alarm)

    @Query("SELECT * FROM alarm WHERE refUid = :eventUid")
    suspend fun getAlarmForEvent(eventUid: String): Array<Alarm>
}