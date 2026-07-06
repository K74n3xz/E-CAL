package net.k74n3xz.ecal.core.database.calendar.relation

import androidx.room.Embedded
import androidx.room.Relation
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmComponent
import net.k74n3xz.ecal.core.database.calendar.entity.EventComponent

internal data class EventComponentWithAlarmComponents(
    @Embedded val eventComponent: EventComponent,
    @Relation(parentColumn = "uid", entityColumn = "refUid")
    val alarmComponents: List<AlarmComponent>
)