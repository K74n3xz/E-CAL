package net.k74n3xz.ecal.core.database.calendar.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.ReconcileResult
import java.time.Instant

@Entity(
    tableName = "alarm_instance",
    indices = [Index("alarmComponentId")],
    foreignKeys = [
        ForeignKey(
            entity = AlarmComponent::class,
            parentColumns = ["id"],
            childColumns = ["alarmComponentId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
internal data class AlarmInstance(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val alarmComponentId: Long?,
    val triggerAt: Instant,
    val desiredState: DesiredState,
    val lastReconcileResult: ReconcileResult
)