package net.k74n3xz.ecal.data.calendar.utils

import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.domain.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.domain.model.enumeration.event.TimeTransparency
import net.k74n3xz.ecal.domain.model.Alarm
import net.k74n3xz.ecal.domain.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ModelEntityConverterTest {
    @Test
    fun event_roundTripsThroughEntityAndGeneratedIcs() {
        val event = Event(
            uid = "event-123",
            createdAt = Instant.parse("2026-06-01T01:02:03Z"),
            updatedAt = Instant.parse("2026-06-02T04:05:06Z"),
            summary = "Planning",
            description = "Quarterly planning",
            location = "Room 7",
            startAt = Instant.parse("2026-06-28T01:30:00Z"),
            endAt = Instant.parse("2026-06-28T03:00:00Z"),
            priority = 3,
            transparency = TimeTransparency.OPAQUE,
            recurrenceRule = "FREQ=DAILY;COUNT=3",
            status = EventStatus.CONFIRMED
        )

        val entity = event.toEventComponent()

        assertEquals(event, entity.toEvent(emptyList()))
        assertTrue(entity.rawIcs.contains("BEGIN:VEVENT"))
        assertTrue(entity.rawIcs.contains("UID:event-123"))
        assertTrue(entity.rawIcs.contains("SUMMARY:Planning"))
    }

    @Test
    fun eventConversion_marksAllDayValuesAsDates() {
        val entity = Event(
            uid = "all-day",
            startAt = Instant.parse("2026-06-28T00:00:00Z"),
            endAt = Instant.parse("2026-06-29T00:00:00Z"),
            isAllDayEvent = true
        ).toEventComponent()

        assertTrue(entity.rawIcs.contains("DTSTART;VALUE=DATE:20260628"))
        assertTrue(entity.rawIcs.contains("DTEND;VALUE=DATE:20260629"))
        assertTrue(entity.toEvent(emptyList()).isAllDayEvent)
    }

    @Test
    fun eventConversion_updatesExistingIcs() {
        val original = Event(
            uid = "event-1",
            summary = "Old",
            startAt = Instant.parse("2026-06-28T01:00:00Z")
        ).toEventComponent()
        val updated = original.toEvent(emptyList()).copy(
            summary = "New",
            updatedAt = Instant.parse("2026-06-29T01:00:00Z")
        )

        val result = updated.toEventComponent(original.rawIcs)

        assertEquals(updated, result.toEvent(emptyList()))
        assertTrue(result.rawIcs.contains("SUMMARY:New"))
    }

    @Test
    fun eventConversion_handlesNullableFieldsAndEscapedUnicodeText() {
        val event = Event(
            uid = "unicode-event",
            summary = "会议, review; next",
            description = "第一行\nSecond line",
            location = null,
            startAt = Instant.parse("2026-06-28T01:00:00Z"),
            endAt = null,
            priority = null,
            transparency = null,
            recurrenceRule = null,
            status = null
        )

        val generated = event.toEventComponent()
        val reparsed = event.copy(updatedAt = Instant.parse("2026-06-29T01:00:00Z"))
            .toEventComponent(generated.rawIcs)

        assertEquals(event.summary, reparsed.summary)
        assertEquals(event.description, reparsed.description)
        assertEquals(event.location, reparsed.location)
        assertTrue(reparsed.rawIcs.contains("会议"))
    }

    @Test
    fun eventConversion_preservesCustomIcsPropertiesWhenUpdating() {
        val original = Event(
            uid = "event-1",
            summary = "Old",
            startAt = Instant.parse("2026-06-28T01:00:00Z")
        ).toEventComponent()
        val withCustomProperty = original.rawIcs.replace(
            "END:VEVENT",
            "X-ECAL-TEST:keep-me\r\nEND:VEVENT"
        )

        val result = original.toEvent(emptyList()).copy(summary = "New")
            .toEventComponent(withCustomProperty)

        assertTrue(result.rawIcs.contains("X-ECAL-TEST:keep-me"))
        assertTrue(result.rawIcs.contains("SUMMARY:New"))
    }

    @Test
    fun eventConversion_rejectsCalendarWithoutEvent() {
        val invalid = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Test//EN\r\nEND:VCALENDAR\r\n"

        val error = assertThrows(IllegalArgumentException::class.java) {
            Event(uid = "replacement").toEventComponent(invalid)
        }

        assertEquals("Unrecognized ICS content.", error.message)
    }

    @Test
    fun eventConversion_rejectsCalendarWithMultipleEvents() {
        val rawIcs = Event(
            uid = "event-1",
            startAt = Instant.parse("2026-06-28T01:00:00Z")
        ).toEventComponent().rawIcs
        val eventBlock = rawIcs.substringAfter("BEGIN:VEVENT").substringBefore("END:VEVENT")
        val invalid = rawIcs.replace(
            "END:VCALENDAR",
            "BEGIN:VEVENT${eventBlock}END:VEVENT\r\nEND:VCALENDAR"
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            Event(uid = "replacement").toEventComponent(invalid)
        }
        assertEquals("Unrecognized ICS content.", error.message)
    }

    @Test
    fun eventConversion_rejectsInvalidRecurrenceRule() {
        assertThrows(IllegalArgumentException::class.java) {
            Event(recurrenceRule = "not-a-rule").toEventComponent()
        }
    }

    @Test
    fun relativeDisplayAlarm_roundTripsWithRepeat() {
        val alarm = Alarm(
            id = 42,
            action = Alarm.Action.Display("Leave now"),
            trigger = Alarm.Trigger.RelativeTrigger(
                TriggerRelationship.END,
                Duration.ofMinutes(-10)
            ),
            interval = Duration.ofMinutes(2),
            repeat = 3
        )

        val entity = alarm.toAlarmComponent("event-1")

        assertEquals(alarm, entity.toAlarm())
        assertTrue(entity.rawIcs.contains("ACTION:DISPLAY"))
        assertTrue(entity.rawIcs.contains("TRIGGER;VALUE=DURATION;RELATED=END:-PT10M"))
        assertTrue(entity.rawIcs.contains("REPEAT:3"))
    }

    @Test
    fun absoluteDisplayAlarm_roundTrips() {
        val alarm = Alarm(
            id = 7,
            action = Alarm.Action.Display("Call"),
            trigger = Alarm.Trigger.AbsoluteTrigger(
                Instant.parse("2026-06-28T01:30:00Z")
            )
        )

        val entity = alarm.toAlarmComponent("event-1")

        assertEquals(alarm, entity.toAlarm())
        assertTrue(entity.rawIcs.contains("TRIGGER"))
        assertTrue(entity.rawIcs.contains("20260628"))
    }

    @Test
    fun alarmConversion_updatesExistingIcs() {
        val original = Alarm(
            id = 5,
            action = Alarm.Action.Display("Old"),
            trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ofMinutes(-5))
        ).toAlarmComponent("event-1")
        val updated = original.toAlarm().copy(
            action = Alarm.Action.Display("New"),
            trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ofMinutes(-15))
        )

        val result = updated.toAlarmComponent("event-1", original.rawIcs)

        assertEquals(updated, result.toAlarm())
        assertTrue(result.rawIcs.contains("DESCRIPTION:New"))
        assertTrue(result.rawIcs.contains("TRIGGER;VALUE=DURATION;RELATED=START:-PT15M"))
    }

    @Test
    fun relativeDisplayAlarm_supportsPositiveStartOffsetWithoutRepeat() {
        val alarm = Alarm(
            action = Alarm.Action.Display("Follow up"),
            trigger = Alarm.Trigger.RelativeTrigger(
                TriggerRelationship.START,
                Duration.ofMinutes(20)
            )
        )

        val entity = alarm.toAlarmComponent("event-1")

        assertEquals(alarm, entity.toAlarm())
        assertEquals(null, entity.interval)
        assertEquals(null, entity.repeat)
        assertTrue(entity.rawIcs.contains("RELATED=START:PT20M"))
    }

    @Test
    fun alarmConversion_rejectsCalendarWithoutAlarm() {
        val eventIcs = Event(
            uid = "event-1",
            startAt = Instant.parse("2026-06-28T01:00:00Z")
        ).toEventComponent().rawIcs
        val alarm = Alarm(
            action = Alarm.Action.Display("Reminder"),
            trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ZERO)
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            alarm.toAlarmComponent("event-1", eventIcs)
        }
        assertEquals("Unrecognized ICS content.", error.message)
    }

    @Test
    fun alarmConversion_rejectsCalendarWithMultipleAlarms() {
        val alarm = Alarm(
            action = Alarm.Action.Display("Reminder"),
            trigger = Alarm.Trigger.RelativeTrigger(offset = Duration.ZERO)
        )
        val rawIcs = alarm.toAlarmComponent("event-1").rawIcs
        val alarmBlock = rawIcs.substringAfter("BEGIN:VALARM").substringBefore("END:VALARM")
        val invalid = rawIcs.replace(
            "END:VEVENT",
            "BEGIN:VALARM${alarmBlock}END:VALARM\r\nEND:VEVENT"
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            alarm.toAlarmComponent("event-1", invalid)
        }
        assertEquals("Unrecognized ICS content.", error.message)
    }
}