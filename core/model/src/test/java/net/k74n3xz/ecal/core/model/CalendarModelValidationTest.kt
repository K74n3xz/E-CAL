package net.k74n3xz.ecal.core.model

import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration

class CalendarModelValidationTest {
    @Test
    fun event_acceptsPriorityBounds() {
        assertEquals(0, Event(uid = "lower-bound", priority = 0).priority)
        assertEquals(9, Event(uid = "upper-bound", priority = 9).priority)
    }

    @Test
    fun event_rejectsPriorityOutsideBounds() {
        listOf(-1, 10).forEach { priority ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                Event(uid = "invalid-priority", priority = priority)
            }
            assertEquals("The priority must be specified in the range 0 to 9.", error.message)
        }
    }

    @Test
    fun alarm_acceptsIntervalAndRepeatTogether() {
        val alarm = Alarm(
            action = Alarm.Action.Display("Reminder"),
            trigger = Alarm.Trigger.RelativeTrigger(
                TriggerRelationship.START,
                Duration.ofMinutes(-15)
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

        assertEquals(null, alarm.repetition)
    }
}
