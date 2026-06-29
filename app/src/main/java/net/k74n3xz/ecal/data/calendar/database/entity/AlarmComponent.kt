package net.k74n3xz.ecal.data.calendar.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import net.k74n3xz.ecal.domain.model.enumeration.alarm.Action
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerType
import java.time.Duration
import java.time.Instant

@Entity(
    tableName = "alarm_component",
    indices = [Index("refUid")],
    foreignKeys = [
        ForeignKey(
            entity = EventComponent::class,
            parentColumns = ["uid"],
            childColumns = ["refUid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class AlarmComponent(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val refUid: String,
    val action: Action,
    val description: String?,
    val triggerType: TriggerType = TriggerType.RELATIVE,  // the Value Type of TRIGGER, TriggerType.RELATIVE = DURATION and TriggerType.ABSOLUTE = DATE-TIME.
    val triggerRelativeTo: TriggerRelationship?,  // If the parameter is not specified on an allowable property, then the default is START.
    val triggerOffset: Duration?,
    val triggerAt: Instant?,
    val summary: String?,
    // TODO: attendee (3.8.4.1.  Attendee)
    val interval: Duration?,  // the DURATION property
    val repeat: Int?,  // Default is "0", zero. (if present)
    // TODO: attach (3.8.1.1.  Attachment)
    val rawIcs: String
) {
    init {
        when (action) {
            Action.AUDIO, Action.EMAIL -> TODO()
            Action.DISPLAY -> {
                if (description == null) {
                    throw IllegalArgumentException("When the action is \"DISPLAY\", the alarm MUST also include a \"DESCRIPTION\" property.")
                }
                when (triggerType) {
                    TriggerType.RELATIVE -> {
                        if (triggerRelativeTo == null || triggerOffset == null) {
                            throw IllegalArgumentException("Neither `triggerRelativeTo` nor `triggerOffset` can be null for a relative alarm.")
                        }
                        if (triggerAt != null) {
                            throw IllegalArgumentException("`triggerAt` must be null for a relative alarm.")
                        }
                    }

                    TriggerType.ABSOLUTE -> {
                        if (triggerRelativeTo != null || triggerOffset != null) {
                            throw IllegalArgumentException("Both `triggerRelativeTo` and `triggerOffset` must be null for an absolute alarm.")
                        }
                        if (triggerAt == null) {
                            throw IllegalArgumentException("`triggerAt` cannot be null for an absolute alarm.")
                        }
                    }
                }
                if (summary != null) {
                    throw IllegalArgumentException("Only if the action is \"EMAIL\", the alarm can include a \"SUMMARY\" property.")
                }
                if (!((interval == null && repeat == null) || (interval != null && repeat != null))) {
                    throw IllegalArgumentException("`interval` and `repeat` must be assigned values simultaneously or neither must be assigned a value.")
                }
            }
        }
    }
}