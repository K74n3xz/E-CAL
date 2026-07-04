package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmInstance
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.DesiredState
import java.time.Instant

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm_component AS comp JOIN alarm_instance AS inst ON comp.id = inst.alarmComponentId WHERE inst.triggerAt <= :triggerAt AND inst.desiredState = :desiredState")
    suspend fun queryDueAlarmsByDesiredState(
        triggerAt: Instant,
        desiredState: DesiredState
    ): Map<AlarmComponent, List<AlarmInstance>>
}