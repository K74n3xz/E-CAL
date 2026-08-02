package net.k74n3xz.ecal.core.data.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import net.k74n3xz.ecal.core.data.database.dao.result.AlarmWithAttachmentsAndAttendees
import net.k74n3xz.ecal.core.data.database.dao.result.EventEntityWithAlarmEntities
import net.k74n3xz.ecal.core.data.database.entity.AlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.ActionType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.data.database.entity.enumeration.attachment.FileState
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventStatus
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.model.property.event.TimeTransparency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelEntityConverterTest {
    @Test
    fun everyEventTiming_roundTripsThroughEntity() {
        val timings = listOf<EventTiming>(
            EventTiming.Timed.InstantTiming(START),
            EventTiming.Timed.RangeTiming(START, END),
            EventTiming.Timed.DurationTiming(START, Duration.ofHours(2)),
            EventTiming.AllDay.SingleDateTiming(DATE),
            EventTiming.AllDay.RangeDateTiming(DATE, DATE.plusDays(2)),
            EventTiming.AllDay.PeriodDateTiming(DATE, Period.ofDays(3))
        )
        val alarms = listOf(displayAlarm())

        timings.forEach { timing ->
            val event = event(timing, alarms)

            assertEquals(event, event.toEventEntity().toEvent(alarms))
        }
    }

    @Test
    fun eventConversion_mapsTimedAndAllDayStorageFields() {
        val timed = event(EventTiming.Timed.DurationTiming(START, Duration.ofMinutes(90)))
            .toEventEntity()
        val allDay = event(EventTiming.AllDay.PeriodDateTiming(DATE, Period.ofDays(2)))
            .toEventEntity()

        assertEquals(false, timed.isAllDayEvent)
        assertEquals(START, timed.startAt)
        assertEquals(Duration.ofMinutes(90), timed.duration)
        assertEquals(START.plusSeconds(90 * 60), timed.actualEndAt)
        assertEquals(null, timed.startDate)

        assertEquals(true, allDay.isAllDayEvent)
        assertEquals(DATE, allDay.startDate)
        assertEquals(Period.ofDays(2), allDay.period)
        assertEquals(DATE.plusDays(1), allDay.actualEndDate)
        assertEquals(null, allDay.startAt)
    }

    @Test
    fun alarmActions_mapToStorageFields() {
        val cases = listOf(
            Action.Audio(attachment()) to ActionType.AUDIO,
            Action.Display("Reminder") to ActionType.DISPLAY,
            Action.Email("Body", "Subject", listOf(attendee()), listOf(attachment())) to ActionType.EMAIL
        )

        cases.forEach { (action, expectedType) ->
            val alarm = Alarm(id = 5, action = action, trigger = Trigger.RelativeTrigger(offset = Duration.ZERO))
            val entity = alarm.toAlarmEntity("event-1")

            assertEquals(expectedType, entity.action)
            assertEquals(alarm, entity.toAlarm(action))
        }
    }

    @Test
    fun relativeAndAbsoluteTriggers_roundTripWithOptionalRepetition() {
        val alarms = listOf(
            Alarm(
                id = 5,
                action = Action.Display("Relative"),
                trigger = Trigger.RelativeTrigger(TriggerRelationship.END, Duration.ofMinutes(-10)),
                repetition = Repetition(Duration.ofMinutes(2), 3)
            ),
            Alarm(
                id = 6,
                action = Action.Display("Absolute"),
                trigger = Trigger.AbsoluteTrigger(START),
                repetition = null
            )
        )

        alarms.forEach { alarm ->
            val entity = alarm.toAlarmEntity("event-1")

            assertEquals(alarm, entity.toAlarm(alarm.action))
        }
    }

    @Test
    fun simpleEntities_convertToModels() {
        val occurrenceEntity = AlarmOccurrenceEntity(
            id = 11,
            alarmId = 5,
            index = 0,
            triggerAt = START,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )
        val attachmentEntity = attachmentEntity()
        val attendee = attendee()

        assertEquals(11L, occurrenceEntity.toAlarmOccurrence().id)
        assertEquals(5L, occurrenceEntity.toAlarmOccurrence().alarmId)
        assertEquals(START, occurrenceEntity.toAlarmOccurrence().triggerAt)
        assertEquals(attachment(), attachmentEntity.toAttachment())
        assertEquals(attendee, attendee.toAttendeeEntity().toAttendee())
    }

    @Test
    fun relationConversion_buildsAudioDisplayAndEmailActions() {
        val audio = relation(
            alarm = Alarm(
                id = 1,
                action = Action.Audio(),
                trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
            ),
            attachments = listOf(attachmentEntity())
        ).toAlarm()
        val display = relation(alarm = displayAlarm()).toAlarm()
        val email = relation(
            alarm = Alarm(
                id = 3,
                action = Action.Email("Body", "Subject", listOf(attendee())),
                trigger = Trigger.AbsoluteTrigger(START)
            ),
            attachments = listOf(attachmentEntity()),
            attendees = listOf(attendee().toAttendeeEntity())
        ).toAlarm()

        assertEquals(Action.Audio(attachment()), audio.action)
        assertEquals(Action.Display("Reminder"), display.action)
        assertEquals(
            Action.Email("Body", "Subject", listOf(attendee()), listOf(attachment())),
            email.action
        )
    }

    @Test
    fun eventRelationConversion_includesConvertedAlarms() {
        val alarmRelation = relation(alarm = displayAlarm())
        val eventRelation = EventEntityWithAlarmEntities(
            eventEntity = event(EventTiming.Timed.InstantTiming(START)).toEventEntity(),
            alarmEntities = listOf(alarmRelation)
        )

        assertEquals(listOf(alarmRelation.toAlarm()), eventRelation.toEvent().alarms)
    }

    @Test
    fun relationConversion_rejectsInvalidActionRelations() {
        val audio = Alarm(
            id = 1,
            action = Action.Audio(),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        )
        val display = displayAlarm()
        val email = Alarm(
            id = 3,
            action = Action.Email("Body", "Subject", listOf(attendee())),
            trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
        )

        assertThrows(IllegalArgumentException::class.java) {
            relation(audio, attachments = listOf(attachmentEntity(), attachmentEntity(id = 10))).toAlarm()
        }
        assertThrows(IllegalArgumentException::class.java) {
            relation(audio, attendees = listOf(attendee().toAttendeeEntity())).toAlarm()
        }
        assertThrows(IllegalArgumentException::class.java) {
            relation(display, attachments = listOf(attachmentEntity())).toAlarm()
        }
        assertThrows(IllegalArgumentException::class.java) {
            relation(email).toAlarm()
        }
    }

    private fun event(schedule: EventTiming, alarms: List<Alarm> = emptyList()) = Event(
        uid = "event-1",
        createdAt = Instant.parse("2026-07-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-07-02T00:00:00Z"),
        summary = "Planning",
        description = "Quarterly planning",
        location = "Room 7",
        schedule = schedule,
        priority = 3,
        transparency = TimeTransparency.OPAQUE,
        recurrenceRule = "FREQ=DAILY;COUNT=3",
        status = EventStatus.CONFIRMED,
        alarms = alarms
    )

    private fun displayAlarm() = Alarm(
        id = 2,
        action = Action.Display("Reminder"),
        trigger = Trigger.RelativeTrigger(offset = Duration.ofMinutes(-5))
    )

    private fun relation(
        alarm: Alarm,
        attachments: List<AttachmentEntity> = emptyList(),
        attendees: List<AttendeeEntity> = emptyList()
    ) = AlarmWithAttachmentsAndAttendees(
        alarmEntity = alarm.toAlarmEntity("event-1"),
        attachmentEntities = attachments,
        attendeeEntities = attendees
    )

    private fun attachment() = Attachment(
        id = 9,
        description = "Agenda",
        name = "agenda.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1024
    )

    private fun attachmentEntity(id: Long = 9) = AttachmentEntity(
        id = id,
        description = "Agenda",
        name = "agenda.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1024,
        relativePath = "attachments/agenda.pdf",
        platformToken = "token",
        state = FileState.OK
    )

    private fun attendee() = Attendee(
        id = 8,
        name = "Alex",
        description = "Required",
        email = "alex@example.com"
    )

    private companion object {
        val START: Instant = Instant.parse("2026-07-20T01:00:00Z")
        val END: Instant = Instant.parse("2026-07-20T03:00:00Z")
        val DATE: LocalDate = LocalDate.of(2026, 7, 20)
    }
}
