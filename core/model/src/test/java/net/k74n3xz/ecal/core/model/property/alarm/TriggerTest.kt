package net.k74n3xz.ecal.core.model.property.alarm

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerTest {
    @Test
    fun relativeTrigger_defaultsToEventStart() {
        val trigger = Trigger.RelativeTrigger(offset = Duration.ofMinutes(-15))

        assertEquals(TriggerRelationship.START, trigger.relativeTo)
        assertEquals(Duration.ofMinutes(-15), trigger.offset)
    }

    @Test
    fun relativeTrigger_preservesExplicitEventBoundary() {
        val trigger = Trigger.RelativeTrigger(
            relativeTo = TriggerRelationship.END,
            offset = Duration.ofMinutes(10)
        )

        assertEquals(TriggerRelationship.END, trigger.relativeTo)
        assertEquals(Duration.ofMinutes(10), trigger.offset)
    }

    @Test
    fun absoluteTrigger_preservesInstant() {
        val instant = Instant.parse("2026-07-09T05:00:00Z")

        assertEquals(instant, Trigger.AbsoluteTrigger(instant).at)
    }
}
