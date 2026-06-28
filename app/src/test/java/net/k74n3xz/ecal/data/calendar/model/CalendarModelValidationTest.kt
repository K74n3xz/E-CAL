package net.k74n3xz.ecal.data.calendar.model

import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarmcomponent.TriggerRelationship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration

class CalendarModelValidationTest {
    @Test
    fun event_acceptsPriorityBounds() {
        assertEquals(0, Event(priority = 0).priority)
        assertEquals(9, Event(priority = 9).priority)
    }

    @Test
    fun event_rejectsPriorityOutsideBounds() {
        listOf(-1, 10).forEach { priority ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                Event(priority = priority)
            }
            assertEquals("The priority must be specified in the range 0 to 9.", error.message)
        }
    }

    @Test
    fun alarm_acceptsIntervalAndRepeatTogether() {
        val alarm = Alarm(
            refUid = "event-1",
            action = Alarm.Action.Display("Reminder"),
            trigger = Alarm.Trigger.RelativeTrigger(
                TriggerRelationship.START,
                Duration.ofMinutes(-15)
            ),
            interval = Duration.ofMinutes(5),
            repeat = 2
        )

        assertEquals(Duration.ofMinutes(5), alarm.interval)
        assertEquals(2, alarm.repeat)
    }

    @Test
    fun alarm_rejectsIncompleteRepeatPair() {
        val intervalOnlyError = assertThrows(IllegalArgumentException::class.java) {
            Alarm(
                refUid = "event-1",
                action = Alarm.Action.Display("Reminder"),
                trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ZERO),
                interval = Duration.ofMinutes(5)
            )
        }
        val repeatOnlyError = assertThrows(IllegalArgumentException::class.java) {
            Alarm(
                refUid = "event-1",
                action = Alarm.Action.Display("Reminder"),
                trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ZERO),
                repeat = 2
            )
        }

        val expected =
            "`interval` and `repeat` must be assigned values simultaneously or neither must be assigned a value."
        assertEquals(expected, intervalOnlyError.message)
        assertEquals(expected, repeatOnlyError.message)
    }
}