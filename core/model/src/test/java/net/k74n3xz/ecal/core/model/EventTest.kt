package net.k74n3xz.ecal.core.model

import java.time.Duration
import java.time.Instant
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventStatus
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.model.property.event.TimeTransparency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EventTest {
    private val defaultSchedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-07-09T03:00:00Z"))

    @Test
    fun event_acceptsPriorityBounds() {
        assertEquals(0, Event(uid = "lower-bound", schedule = defaultSchedule, priority = 0).priority)
        assertEquals(9, Event(uid = "upper-bound", schedule = defaultSchedule, priority = 9).priority)
    }

    @Test
    fun event_acceptsUnspecifiedPriority() {
        assertNull(Event(uid = "unspecified-priority", schedule = defaultSchedule).priority)
    }

    @Test
    fun event_rejectsPriorityOutsideBounds() {
        listOf(-1, 10).forEach { priority ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                Event(uid = "invalid-priority", schedule = defaultSchedule, priority = priority)
            }

            assertEquals("The priority must be specified in the range 0 to 9.", error.message)
        }
    }

    @Test
    fun event_copyRejectsPriorityOutsideBounds() {
        val event = Event(uid = "event", schedule = defaultSchedule, priority = 1)

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
        val schedule = EventTiming.Timed.RangeTiming(
            startAt = Instant.parse("2026-07-09T03:00:00Z"),
            endAt = Instant.parse("2026-07-09T04:00:00Z")
        )
        val alarms = listOf(
            Alarm(
                id = 7L,
                action = Action.Display("Leave now"),
                trigger = Trigger.RelativeTrigger(
                    relativeTo = TriggerRelationship.END,
                    offset = Duration.ofMinutes(-5)
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
            schedule = schedule,
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
        assertEquals(schedule, event.schedule)
        assertEquals(5, event.priority)
        assertEquals(TimeTransparency.TRANSPARENT, event.transparency)
        assertEquals("FREQ=DAILY", event.recurrenceRule)
        assertEquals(EventStatus.CONFIRMED, event.status)
        assertEquals(alarms, event.alarms)
    }
}
