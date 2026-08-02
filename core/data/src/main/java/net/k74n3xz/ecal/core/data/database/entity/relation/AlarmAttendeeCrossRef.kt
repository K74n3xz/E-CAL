package net.k74n3xz.ecal.core.data.database.entity.relation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity

@Entity(
    tableName = "x_alarm_attendee",
    indices = [Index("attendeeId")],
    primaryKeys = ["alarmId", "attendeeId"],
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AttendeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attendeeId"],
            onDelete = ForeignKey.CASCADE, // TODO: Add a placeholder for a deleted attendee.
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class AlarmAttendeeCrossRef(val alarmId: Long, val attendeeId: Long)
