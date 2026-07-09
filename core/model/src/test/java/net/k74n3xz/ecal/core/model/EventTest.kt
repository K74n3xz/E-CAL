package net.k74n3xz.ecal.core.model

import java.time.Instant
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.core.model.enumeration.event.TimeTransparency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EventTest {
    @Test
    fun event_acceptsPriorityBounds() {
        assertEquals(0, Event(uid = "lower-bound", priority = 0).priority)
        assertEquals(9, Event(uid = "upper-bound", priority = 9).priority)
    }

    @Test
    fun event_acceptsUnspecifiedPriority() {
        assertNull(Event(uid = "unspecified-priority", priority = null).priority)
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
    fun event_copyRejectsPriorityOutsideBounds() {
        val event = Event(uid = "event", priority = 1)

        val error = assertThrows(IllegalArgumentException::class.java) {
            @Suppress("UnusedDataClassCopyResult")
            event.copy(priority = 10)
        }

        assertEquals("The priority must be specified in the range 0 to 9.", error.message)
    }

    @Test
    fun event_preservesExplicitFields() {
        val createdAt = Instant.parse("2026-07-09T01:00:00Z")
        val updatedAt = Instant.parse("2026-07-09T02:00:00Z")
        val startAt = Instant.parse("2026-07-09T03:00:00Z")
        val endAt = Instant.parse("2026-07-09T04:00:00Z")
        val alarms = listOf(
            Alarm(
                id = 7L,
                action = Alarm.Action.Display("Leave now"),
                trigger = Alarm.Trigger.RelativeTrigger(
                    relativeTo = TriggerRelationship.END,
                    offset = java.time.Duration.ofMinutes(-5)
                )
            )
        )

        val event = Event(
            uid = "event-1",
            createdAt = createdAt,
            updatedAt = updatedAt,
            summary = "Summary",
            description = "Description",
            location = "Location",
            startAt = startAt,
            isAllDayEvent = true,
            endAt = endAt,
            priority = 5,
            transparency = TimeTransparency.TRANSPARENT,
            recurrenceRule = "FREQ=DAILY",
            status = EventStatus.CONFIRMED,
            alarms = alarms
        )

        assertEquals("event-1", event.uid)
        assertEquals(createdAt, event.createdAt)
        assertEquals(updatedAt, event.updatedAt)
        assertEquals("Summary", event.summary)
        assertEquals("Description", event.description)
        assertEquals("Location", event.location)
        assertEquals(startAt, event.startAt)
        assertEquals(true, event.isAllDayEvent)
        assertEquals(endAt, event.endAt)
        assertEquals(5, event.priority)
        assertEquals(TimeTransparency.TRANSPARENT, event.transparency)
        assertEquals("FREQ=DAILY", event.recurrenceRule)
        assertEquals(EventStatus.CONFIRMED, event.status)
        assertEquals(alarms, event.alarms)
    }
}
