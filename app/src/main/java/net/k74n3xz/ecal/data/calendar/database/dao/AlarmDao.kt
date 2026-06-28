package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface AlarmDao {
    @Query("SELECT cpn.description FROM alarm_component AS cpn JOIN alarm_instance AS inst ON cpn.id = inst.alarmComponentId WHERE inst.id = :id")
    fun queryAlarmComponentDescriptionByAlarmInstanceId(id: Long): String?
}