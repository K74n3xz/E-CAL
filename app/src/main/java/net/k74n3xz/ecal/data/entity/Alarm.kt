package net.k74n3xz.ecal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.k74n3xz.ecal.data.entity.enumeration.alarm.AlarmType
import java.time.Instant

@Entity(
    tableName = "alarm",
    indices = [Index(value = ["refUid"])],
    foreignKeys = [ForeignKey(
        entity = Event::class,
        parentColumns = ["uid"],
        childColumns = ["refUid"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val refUid: String,
    val type: AlarmType,
    val absoluteTrigger: Instant?,
    val relativeMinutesTrigger: Long?
)