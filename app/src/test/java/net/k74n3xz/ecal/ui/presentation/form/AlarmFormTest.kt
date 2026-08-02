package net.k74n3xz.ecal.ui.presentation.form

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
import net.k74n3xz.ecal.ui.presentation.form.error.DurationFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError
import net.k74n3xz.ecal.ui.presentation.form.utils.toAlarmForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class AlarmFormTest {
    private val zone = ZoneId.of("Asia/Hong_Kong")

    @Test
    fun converter_mapsDisplayRelativeTriggerAndRepetition() {
        val alarm = Alarm(
            id = 7,
            action = Action.Display("Reminder"),
            trigger = Trigger.RelativeTrigger(TriggerRelationship.END, Duration.ofMinutes(-10)),
            repetition = Repetition(Duration.ofMinutes(5), 3)
        )

        val form = alarm.toAlarmForm(zone)

        assertEquals(ActionType.DISPLAY, form.actionType)
        assertEquals("Reminder", form.description.text.toString())
        assertEquals(TriggerType.RELATIVE, form.triggerType)
        assertEquals(TriggerRelationship.END, form.relativeTo)
        assertEquals(Duration.ofMinutes(-10), form.offset)
        assertTrue(form.isRepetitionEnabled)
        assertEquals(Duration.ofMinutes(5), form.interval)
        assertEquals("3", form.repeat.text.toString())
        assertEquals(alarm, form.resolve(alarm.id, zone).getOrThrow())
    }

    @Test
    fun absoluteTrigger_usesProvidedTimeZoneInBothDirections() {
        val alarm = Alarm(
            action = Action.Display("Absolute"),
            trigger = Trigger.AbsoluteTrigger(Instant.parse("2026-07-20T03:30:00Z"))
        )

        val form = alarm.toAlarmForm(zone)

        assertEquals(TriggerType.ABSOLUTE, form.triggerType)
        assertEquals(LocalDate.of(2026, 7, 20), form.atDate)
        assertEquals(LocalTime.of(11, 30), form.atTime)
        assertEquals(alarm, form.resolve(null, zone).getOrThrow())
    }

    @Test
    fun resolve_mapsEditedDisplayAlarmAndPreservesId() {
        val form = Alarm(
            id = 9,
            action = Action.Display("old"),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        ).toAlarmForm(zone).apply {
            description.replaceText("new")
            relativeTo = TriggerRelationship.END
            offset = Duration.ofHours(-1)
            isRepetitionEnabled = false
        }

        assertEquals(
            Alarm(
                id = 9,
                action = Action.Display("new"),
                trigger = Trigger.RelativeTrigger(TriggerRelationship.END, Duration.ofHours(-1))
            ),
            form.resolve(9, zone).getOrThrow()
        )
    }

    @Test
    fun audioAction_isValidAndRoundTripsDefaultAndSelectedAttachment() {
        val form = Alarm(
            action = Action.Audio(),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        ).toAlarmForm(zone)

        assertEquals(ActionType.AUDIO, form.actionType)
        assertTrue(form.isValid)
        assertEquals(Action.Audio(), form.resolve(null, zone).getOrThrow().action)

        val attachment = Attachment(7, null, "alarm.mp3", "audio/mpeg", 42)
        form.audioAttachment = attachment
        assertEquals(Action.Audio(attachment), form.resolve(null, zone).getOrThrow().action)
    }

    @Test
    fun emailAction_requiresAttendeeAndRoundTripsFieldsAndAttachments() {
        val attendee = Attendee(5, "User", null, "user@example.com")
        val attachment = Attachment(7, null, "agenda.pdf", "application/pdf", 42)
        val form = Alarm(
            action = Action.Audio(),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        ).toAlarmForm(zone)

        form.actionType = ActionType.EMAIL
        assertFalse(form.isValid)

        form.summary.replaceText("Subject")
        form.descriptionEmail.replaceText("Body")
        form.attendees += attendee
        form.hasAttachments = true
        form.attachments += attachment

        assertTrue(form.isValid)
        assertEquals(
            Action.Email("Body", "Subject", listOf(attendee), listOf(attachment)),
            form.resolve(null, zone).getOrThrow().action
        )

        form.hasAttachments = false
        assertEquals(
            Action.Email("Body", "Subject", listOf(attendee), null),
            form.resolve(null, zone).getOrThrow().action
        )
    }

    @Test
    fun converter_restoresEmailFieldsAndSelections() {
        val attendee = Attendee(5, "User", null, "user@example.com")
        val attachment = Attachment(7, null, "agenda.pdf", "application/pdf", 42)
        val action = Action.Email("Body", "Subject", listOf(attendee), listOf(attachment))

        val form = Alarm(
            action = action,
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        ).toAlarmForm(zone)

        assertEquals(ActionType.EMAIL, form.actionType)
        assertEquals("Subject", form.summary.text.toString())
        assertEquals("Body", form.descriptionEmail.text.toString())
        assertEquals(listOf(attendee), form.attendees)
        assertTrue(form.hasAttachments)
        assertEquals(listOf(attachment), form.attachments)
        assertEquals(action, form.resolve(null, zone).getOrThrow().action)
    }

    @Test
    fun enabledRepetition_requiresPositiveIntervalAndIntegerRepeat() {
        val form = Alarm(
            action = Action.Display(""),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO),
            repetition = Repetition(Duration.ofMinutes(1), 1)
        ).toAlarmForm(zone)

        form.interval = Duration.ZERO
        assertEquals(DurationFieldError.NotPositive, form.intervalFieldError)
        assertFalse(form.isValid)

        form.interval = Duration.ofSeconds(1)
        form.repeat.replaceText("")
        assertEquals(NumberTextFieldError.Empty, form.repeatFieldError)

        form.repeat.replaceText("1x")
        assertEquals(NumberTextFieldError.InvalidCharacter, form.repeatFieldError)

        form.repeat.replaceText("2147483648")
        assertEquals(NumberTextFieldError.TooBigNumber, form.repeatFieldError)
        assertTrue(form.resolve(null, zone).isFailure)
    }

    @Test
    fun disabledRepetition_ignoresInvalidRepetitionFields() {
        val form = Alarm(
            action = Action.Display(""),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        ).toAlarmForm(zone).apply {
            interval = Duration.ZERO
            repeat.replaceText("")
            isRepetitionEnabled = false
        }

        assertTrue(form.isValid)
        assertEquals(null, form.resolve(null, zone).getOrThrow().repetition)
    }
}
