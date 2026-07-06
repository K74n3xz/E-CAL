package net.k74n3xz.ecal.core.database.calendar.dao

import androidx.room.Dao
import androidx.room.Query
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmComponent
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmInstance
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.DesiredState
import java.time.Instant

@Dao
internal interface AlarmDao {
    @Query("SELECT * FROM alarm_component AS comp JOIN alarm_instance AS inst ON comp.id = inst.alarmComponentId WHERE inst.triggerAt <= :triggerAt AND inst.desiredState = :desiredState")
    suspend fun queryDueAlarmsByDesiredState(
        triggerAt: Instant,
        desiredState: DesiredState
    ): Map<AlarmComponent, List<AlarmInstance>>
}