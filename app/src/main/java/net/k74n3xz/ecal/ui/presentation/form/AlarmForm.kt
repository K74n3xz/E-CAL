package net.k74n3xz.ecal.ui.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.text.isDigitsOnly
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.ui.presentation.form.enumeration.alarm.ActionType
import net.k74n3xz.ecal.ui.presentation.form.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.ui.presentation.form.error.ContainerFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.DurationFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError

@Stable
internal class AlarmForm(
    initialAction: Action,
    initialTrigger: Trigger,
    initialRepetition: Repetition?,
    timeZone: ZoneId
) {
    /* Action */
    var actionType: ActionType by mutableStateOf(
        when (initialAction) {
            is Action.Audio -> ActionType.AUDIO
            is Action.Display -> ActionType.DISPLAY
            is Action.Email -> ActionType.EMAIL
        }
    )

    /* Action: AUDIO */
    var audioAttachment: Attachment? by mutableStateOf(
        when (initialAction) {
            is Action.Audio -> initialAction.attach
            else -> null
        }
    )

    /* Action: DISPLAY */
    val description: TextFieldState = TextFieldState(
        when (initialAction) {
            is Action.Display -> initialAction.description
            else -> ""
        }
    )

    /* Action: EMAIL */
    val summary: TextFieldState = TextFieldState(
        when (initialAction) {
            is Action.Email -> initialAction.summary
            else -> ""
        }
    )
    val descriptionEmail: TextFieldState = TextFieldState(
        when (initialAction) {
            is Action.Email -> initialAction.description
            else -> ""
        }
    )
    val attendees: SnapshotStateList<Attendee> = when (initialAction) {
        is Action.Email -> initialAction.attendee.toMutableStateList()
        else -> mutableStateListOf()
    }
    val attendeesFieldError: ContainerFieldError?
        get() = if (attendees.isEmpty()) {
            ContainerFieldError.Empty
        } else {
            null
        }
    var hasAttachments: Boolean by mutableStateOf(
        when (initialAction) {
            is Action.Email -> initialAction.attach != null
            else -> false
        }
    )
    val attachments: SnapshotStateList<Attachment> = when (initialAction) {
        is Action.Email -> initialAction.attach?.toMutableStateList() ?: mutableStateListOf()
        else -> mutableStateListOf()
    }

    /* Trigger */
    var triggerType: TriggerType by mutableStateOf(
        when (initialTrigger) {
            is Trigger.RelativeTrigger -> TriggerType.RELATIVE
            is Trigger.AbsoluteTrigger -> TriggerType.ABSOLUTE
        }
    )

    /* Trigger: Relative Trigger */
    var relativeTo: TriggerRelationship by mutableStateOf(
        when (initialTrigger) {
            is Trigger.RelativeTrigger -> initialTrigger.relativeTo
            is Trigger.AbsoluteTrigger -> TriggerRelationship.START
        }
    )
    var offset: Duration by mutableStateOf(
        when (initialTrigger) {
            is Trigger.RelativeTrigger -> initialTrigger.offset
            is Trigger.AbsoluteTrigger -> Duration.ofMinutes(-15)
        }
    )

    /* Trigger: Absolute Trigger */
    var atDate: LocalDate by mutableStateOf(
        when (initialTrigger) {
            is Trigger.RelativeTrigger -> LocalDateTime.now().plusMinutes(15).toLocalDate()
            is Trigger.AbsoluteTrigger -> initialTrigger.at.atZone(timeZone).toLocalDate()
        }
    )
    var atTime: LocalTime by mutableStateOf(
        when (initialTrigger) {
            is Trigger.RelativeTrigger -> LocalDateTime.now().plusMinutes(15).toLocalTime()
            is Trigger.AbsoluteTrigger -> initialTrigger.at.atZone(timeZone).toLocalTime()
        }
    )

    /* Repetition */
    var isRepetitionEnabled: Boolean by mutableStateOf(initialRepetition != null)
    var interval: Duration by mutableStateOf(initialRepetition?.interval ?: Duration.ofMinutes(15))
    val intervalFieldError: DurationFieldError?
        get() = if (!interval.isPositive) {
            DurationFieldError.NotPositive
        } else {
            null
        }
    val repeat: TextFieldState = TextFieldState(initialRepetition?.repeat?.toString() ?: "0")
    val repeatFieldError: NumberTextFieldError?
        get() = if (repeat.text.isEmpty()) {
            NumberTextFieldError.Empty
        } else if (!repeat.text.isDigitsOnly()) {
            NumberTextFieldError.InvalidCharacter
        } else if (repeat.text.toString().toIntOrNull() == null) {
            NumberTextFieldError.TooBigNumber
        } else {
            null
        }

    val isValid: Boolean
        get() = (!(actionType == ActionType.EMAIL && attendeesFieldError != null)) &&
            (!isRepetitionEnabled || (isRepetitionEnabled && repeatFieldError == null && intervalFieldError == null))

    fun resolve(id: Long?, timeZone: ZoneId): Result<Alarm> = if (isValid) {
        try {
            Result.success(
                Alarm(
                    id = id,
                    action = when (actionType) {
                        ActionType.AUDIO -> Action.Audio(audioAttachment)

                        ActionType.DISPLAY -> Action.Display(description.text.toString())

                        ActionType.EMAIL -> Action.Email(
                            description = descriptionEmail.text.toString(),
                            summary = summary.text.toString(),
                            attendee = attendees.toList(),
                            attach = if (hasAttachments) attachments.toList() else null
                        )
                    },
                    trigger = when (triggerType) {
                        TriggerType.RELATIVE -> Trigger.RelativeTrigger(
                            relativeTo = relativeTo,
                            offset = offset
                        )

                        TriggerType.ABSOLUTE -> Trigger.AbsoluteTrigger(
                            LocalDateTime.of(atDate, atTime).atZone(timeZone).toInstant()
                        )
                    },
                    repetition = if (isRepetitionEnabled) {
                        Repetition(interval, repeat.text.toString().toInt())
                    } else {
                        null
                    }
                )
            )
        } catch (numberFormatException: NumberFormatException) {
            Result.failure(numberFormatException)
        }
    } else {
        Result.failure(IllegalArgumentException("Unresolvable input."))
    }
}
