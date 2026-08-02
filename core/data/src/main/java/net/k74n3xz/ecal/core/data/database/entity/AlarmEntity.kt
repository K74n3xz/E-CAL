package net.k74n3xz.ecal.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.Instant
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.ActionType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship

@Entity(
    tableName = "alarm",
    indices = [Index("refUid")],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["uid"],
            childColumns = ["refUid"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
internal data class AlarmEntity(
    /* Metadata */
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val refUid: String,

    /* Action */
    val action: ActionType,
    val summary: String?,
    val description: String?,
    // NOTE: When the `ACTION` is `AUDIO`, the attachment is stored in cross-reference tables.
    // NOTE: When the `ACTION` is `EMAIL`, attachments and attendees are stored in cross-reference tables.

    /* Trigger */
    // the Value Type of TRIGGER, TriggerType.RELATIVE = DURATION and TriggerType.ABSOLUTE = DATE-TIME.
    val triggerType: TriggerType = TriggerType.RELATIVE,
    // If the parameter is not specified on an allowable property, then the default is START.
    val triggerRelativeTo: TriggerRelationship?,
    val triggerOffset: Duration?,
    val triggerAt: Instant?,

    /* Repetition */
    val interval: Duration?, // the DURATION property
    // Default is "0", zero. (if present)
    val repeat: Int?
) {
    init {
        when (action) {
            ActionType.AUDIO -> {
                require(description == null) {
                    "When the action is `AUDIO`, the alarm can't include a `DESCRIPTION` property."
                }
                require(summary == null) {
                    "Only if the action is `EMAIL`, the alarm can include a `SUMMARY` property."
                }
            }

            ActionType.DISPLAY -> {
                requireNotNull(description) {
                    "When the action is `DISPLAY`, the alarm must also include a `DESCRIPTION` property."
                }
                require(summary == null) {
                    "Only if the action is `EMAIL`, the alarm can include a `SUMMARY` property."
                }
            }

            ActionType.EMAIL -> {
                requireNotNull(description) {
                    "When the action is `EMAIL`, the alarm must also include a `DESCRIPTION` property."
                }
                requireNotNull(summary) {
                    "When the action is `EMAIL`, the alarm must also include a `SUMMARY` property."
                }
            }
        }

        when (triggerType) {
            TriggerType.RELATIVE -> {
                require(triggerRelativeTo != null && triggerOffset != null) {
                    "Neither `triggerRelativeTo` nor `triggerOffset` can be null for a relative alarm."
                }
                require(triggerAt == null) {
                    "`triggerAt` must be null for a relative alarm."
                }
            }

            TriggerType.ABSOLUTE -> {
                require(triggerRelativeTo == null && triggerOffset == null) {
                    "Both `triggerRelativeTo` and `triggerOffset` must be null for an absolute alarm."
                }
                requireNotNull(triggerAt) {
                    "`triggerAt` cannot be null for an absolute alarm."
                }
            }
        }

        require((interval == null && repeat == null) || (interval != null && repeat != null)) {
            "`interval` and `repeat` must be assigned values simultaneously or neither must be assigned a value."
        }
        if (interval != null && repeat != null) {
            require(interval.isPositive) {
                "`interval` must be positive."
            }
            require(repeat >= 0) {
                "`repeat` must not be negative."
            }
        }
    }
}
