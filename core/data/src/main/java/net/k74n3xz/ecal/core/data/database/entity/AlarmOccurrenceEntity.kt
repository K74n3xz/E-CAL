package net.k74n3xz.ecal.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult

@Entity(
    tableName = "alarm_occurrence",
    indices = [Index("alarmId")],
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
internal data class AlarmOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val alarmId: Long?,
    val index: Long,
    val triggerAt: Instant,
    val desiredState: DesiredState,
    val lastReconcileResult: ReconcileResult
)
