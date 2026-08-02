package net.k74n3xz.ecal.core.model

import java.time.Duration
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmTest {
    @Test
    fun alarm_preservesExplicitFields() {
        val action = Action.Display("Leave now")
        val trigger = Trigger.RelativeTrigger(offset = Duration.ofMinutes(-15))
        val repetition = Repetition(interval = Duration.ofMinutes(5), repeat = 2)

        val alarm = Alarm(id = 7L, action = action, trigger = trigger, repetition = repetition)

        assertEquals(7L, alarm.id)
        assertEquals(action, alarm.action)
        assertEquals(trigger, alarm.trigger)
        assertEquals(repetition, alarm.repetition)
    }

    @Test
    fun alarm_supportsMissingIdAndRepetition() {
        val alarm = Alarm(
            action = Action.Display("Reminder"),
            trigger = Trigger.AbsoluteTrigger(java.time.Instant.parse("2026-07-09T05:00:00Z"))
        )

        assertNull(alarm.id)
        assertNull(alarm.repetition)
    }
}
