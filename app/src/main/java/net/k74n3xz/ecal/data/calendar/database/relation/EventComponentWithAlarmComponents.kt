package net.k74n3xz.ecal.data.calendar.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent
import net.k74n3xz.ecal.data.calendar.database.entity.EventComponent

data class EventComponentWithAlarmComponents(
    @Embedded val eventComponent: EventComponent,
    @Relation(parentColumn = "uid", entityColumn = "refUid")
    val alarmComponents: List<AlarmComponent>
)