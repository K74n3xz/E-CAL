package net.k74n3xz.ecal.core.model

import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import java.net.URI
import java.time.Duration
import java.time.Instant

data class Alarm(
    val id: Long? = null,
    val action: Action,
    val trigger: Trigger,
    val repetition: Repetition? = null
) {
    sealed interface Action {
        data class Audio(val attach: URI) : Action {
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
            val attendee: List<URI>,
            val attach: List<URI>?
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

    data class Repetition(val interval: Duration, val repeat: Int)
}