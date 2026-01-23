package net.k74n3xz.ecal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import net.k74n3xz.ecal.data.entity.Alarm
import net.k74n3xz.ecal.data.entity.Event

@Dao
interface EventWithAlarmsDao {
    @Upsert
    fun upsertEvent(event: Event)

    @Insert
    fun insertAllAlarms(alarms: Array<Alarm>)

    @Query("DELETE FROM alarm WHERE refUid = :eventUid")
    fun deleteAlarmsByEventUid(eventUid: String)

    @Transaction
    fun upsertEventWithAlarms(event: Event, alarms: Array<Alarm>) {
        upsertEvent(event)
        deleteAlarmsByEventUid(event.uid)
        insertAllAlarms(alarms)
    }
}