package net.k74n3xz.ecal.core.model.property.alarm

import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee

sealed interface Action {
    data class Audio(val attach: Attachment? = null) : Action

    data class Display(val description: String) : Action

    data class Email(
        val description: String,
        val summary: String,
        val attendee: List<Attendee>,
        val attach: List<Attachment>? = null
    ) : Action {
        init {
            if (attendee.isEmpty()) {
                throw IllegalArgumentException(
                    "When the action is `EMAIL`, the alarm must include one or more `ATTENDEE` properties."
                )
            }
        }
    }
}
