package net.k74n3xz.ecal.core.data.database.entity.relation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity

@Entity(
    tableName = "x_alarm_attachment",
    indices = [Index("attachmentId")],
    primaryKeys = ["alarmId", "attachmentId"],
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class AlarmAttachmentCrossRef(val alarmId: Long, val attachmentId: Long)
