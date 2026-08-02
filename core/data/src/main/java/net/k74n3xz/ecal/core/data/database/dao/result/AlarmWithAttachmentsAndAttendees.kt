package net.k74n3xz.ecal.core.data.database.dao.result

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttachmentCrossRef
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttendeeCrossRef

internal data class AlarmWithAttachmentsAndAttendees(
    @Embedded
    val alarmEntity: AlarmEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AlarmAttachmentCrossRef::class,
            parentColumn = "alarmId",
            entityColumn = "attachmentId"
        )
    )
    val attachmentEntities: List<AttachmentEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AlarmAttendeeCrossRef::class,
            parentColumn = "alarmId",
            entityColumn = "attendeeId"
        )
    )
    val attendeeEntities: List<AttendeeEntity>
)
