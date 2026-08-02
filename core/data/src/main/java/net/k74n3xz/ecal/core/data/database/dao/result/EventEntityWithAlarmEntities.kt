package net.k74n3xz.ecal.core.data.database.dao.result

import androidx.room.Embedded
import androidx.room.Relation
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.EventEntity

internal data class EventEntityWithAlarmEntities(
    @Embedded
    val eventEntity: EventEntity,

    @Relation(entity = AlarmEntity::class, parentColumn = "uid", entityColumn = "refUid")
    val alarmEntities: List<AlarmWithAttachmentsAndAttendees>
)
