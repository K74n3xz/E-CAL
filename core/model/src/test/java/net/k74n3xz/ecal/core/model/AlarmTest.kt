package net.k74n3xz.ecal.core.model

import java.net.URI
import java.time.Duration
import java.time.Instant
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AlarmTest {
    @Test
    fun displayAction_preservesDescription() {
        val action = Alarm.Action.Display("Reminder")

        assertEquals("Reminder", action.description)
    }

    @Test
    fun relativeTrigger_defaultsToEventStart() {
        val trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ofMinutes(-15))

        assertEquals(TriggerRelationship.START, trigger.relativeTo)
        assertEquals(Duration.ofMinutes(-15), trigger.offset)
    }

    @Test
    fun relativeTrigger_preservesExplicitEventBoundary() {
        val trigger = Alarm.Trigger.RelativeTrigger(
            relativeTo = TriggerRelationship.END,
            offset = Duration.ofMinutes(10)
        )

        assertEquals(TriggerRelationship.END, trigger.relativeTo)
        assertEquals(Duration.ofMinutes(10), trigger.offset)
    }

    @Test
    fun absoluteTrigger_preservesInstant() {
        val instant = Instant.parse("2026-07-09T05:00:00Z")

        val trigger = Alarm.Trigger.AbsoluteTrigger(instant)

        assertEquals(instant, trigger.at)
    }

    @Test
    fun alarm_acceptsIntervalAndRepeatTogether() {
        val alarm = Alarm(
            action = Alarm.Action.Display("Reminder"),
            trigger = Alarm.Trigger.RelativeTrigger(
                relativeTo = TriggerRelationship.START,
                offset = Duration.ofMinutes(-15)
            ),
            repetition = Alarm.Repetition(Duration.ofMinutes(5), 2)
        )

        assertEquals(Duration.ofMinutes(5), alarm.repetition?.interval)
        assertEquals(2, alarm.repetition?.repeat)
    }

    @Test
    fun alarm_supportsNoRepetition() {
        val alarm = Alarm(
            action = Alarm.Action.Display("Reminder"),
            trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ZERO)
        )

        assertNull(alarm.repetition)
    }

    @Test
    fun audioAction_isCurrentlyUnsupported() {
        assertThrows(NotImplementedError::class.java) {
            Alarm.Action.Audio(URI.create("file:///alarm.wav"))
        }
    }

    @Test
    fun emailAction_rejectsEmptyAttendees() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            Alarm.Action.Email(
                description = "Description",
                summary = "Summary",
                attendee = emptyList(),
                attach = null
            )
        }

        assertEquals(
            "When the action is \"EMAIL\", the alarm MUST include one or more \"ATTENDEE\" properties.",
            error.message
        )
    }

    @Test
    fun emailActionWithAttendees_isCurrentlyUnsupported() {
        assertThrows(NotImplementedError::class.java) {
            Alarm.Action.Email(
                description = "Description",
                summary = "Summary",
                attendee = listOf(URI.create("mailto:user@example.com")),
                attach = listOf(URI.create("file:///agenda.txt"))
            )
        }
    }
}
