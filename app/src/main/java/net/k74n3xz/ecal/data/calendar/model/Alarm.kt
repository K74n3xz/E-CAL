package net.k74n3xz.ecal.data.calendar.model

import android.net.Uri
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.TriggerRelationship
import java.time.Duration
import java.time.Instant

data class Alarm(
    val id: Long? = null,
    val refUid: String,
    val action: Action,
    val trigger: Trigger,
    val interval: Duration? = null,
    val repeat: Int? = null
) {
    init {
        if (!((interval == null && repeat == null) || (interval != null && repeat != null))) {
            throw IllegalArgumentException("`interval` and `repeat` must be assigned values simultaneously or neither must be assigned a value.")
        }
    }

    sealed interface Action {
        data class Audio(val attach: Uri) : Action {
            init {
                /*
                * 3.8.1.1.  Attachment
                *   Value Type: The default value type for this property is URI.
                *               The value type can also be set to BINARY to indicate inline binary encoded content information.
                * */
                TODO()
            }
        }

        data class Display(val description: String) : Action

        data class Email(
            val description: String,
            val summary: String,
            val attendee: List<Uri>,
            val attach: List<Uri>?
        ) : Action {
            init {
                if (attendee.isEmpty()) {
                    throw IllegalArgumentException("When the action is \"EMAIL\", the alarm MUST include one or more \"ATTENDEE\" properties.")
                }
                TODO()
            }
        }
    }

    sealed interface Trigger {
        data class RelativeTrigger(
            val relativeTo: TriggerRelationship = TriggerRelationship.START,
            val offset: Duration
        ) : Trigger

        data class AbsoluteTrigger(val at: Instant) : Trigger
    }
}