package net.k74n3xz.ecal.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EventWithAlarms(
    @Embedded val event: Event,
    @Relation(parentColumn = "uid", entityColumn = "refUid") val alarms: List<Alarm>
)