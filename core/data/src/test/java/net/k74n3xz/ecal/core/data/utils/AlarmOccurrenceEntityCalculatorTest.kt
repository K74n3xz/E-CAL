package net.k74n3xz.ecal.core.data.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.EventEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.ActionType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmOccurrenceEntityCalculatorTest {
    @Test
    fun firstRelativeOccurrence_usesTimedEventStartAndInitialState() {
        val occurrence = calculateNextAlarmOccurrenceEntity(
            alarmEntity = relativeAlarm(offset = Duration.ofMinutes(-15)),
            referenceEntity = timedEvent(),
            lastAlarmOccurrenceIndex = null,
            timeZone = ZoneOffset.UTC
        )

        requireNotNull(occurrence)
        assertNull(occurrence.id)
        assertEquals(7L, occurrence.alarmId)
        assertEquals(0L, occurrence.index)
        assertEquals(START.minusSeconds(15 * 60), occurrence.triggerAt)
        assertEquals(DesiredState.ACTIVE, occurrence.desiredState)
        assertEquals(ReconcileResult.CANCELLED, occurrence.lastReconcileResult)
    }

    @Test
    fun relativeOccurrence_canUseTimedEventEnd() {
        val occurrence = calculateNextAlarmOccurrenceEntity(
            alarmEntity = relativeAlarm(relativeTo = TriggerRelationship.END),
            referenceEntity = timedEvent(),
            lastAlarmOccurrenceIndex = null,
            timeZone = ZoneOffset.UTC
        )

        assertEquals(END, occurrence?.triggerAt)
    }

    @Test
    fun absoluteOccurrence_usesTriggerTime() {
        val triggerAt = Instant.parse("2026-07-21T08:30:00Z")
        val occurrence = calculateNextAlarmOccurrenceEntity(
            alarmEntity = absoluteAlarm(triggerAt),
            referenceEntity = timedEvent(),
            lastAlarmOccurrenceIndex = null,
            timeZone = ZoneOffset.ofHours(8)
        )

        assertEquals(triggerAt, occurrence?.triggerAt)
    }

    @Test
    fun allDayStartOccurrence_usesProvidedTimeZone() {
        val event = allDayEvent()
        val alarm = relativeAlarm()

        val positiveOffset = calculateNextAlarmOccurrenceEntity(
            alarm,
            event,
            lastAlarmOccurrenceIndex = null,
            timeZone = ZoneOffset.ofHours(14)
        )
        val negativeOffset = calculateNextAlarmOccurrenceEntity(
            alarm,
            event,
            lastAlarmOccurrenceIndex = null,
            timeZone = ZoneOffset.ofHours(-10)
        )

        assertEquals(Instant.parse("2026-07-19T10:00:00Z"), positiveOffset?.triggerAt)
        assertEquals(Instant.parse("2026-07-20T10:00:00Z"), negativeOffset?.triggerAt)
    }

    @Test
    fun allDayEndOccurrence_usesEndOfEffectiveEndDate() {
        val occurrence = calculateNextAlarmOccurrenceEntity(
            alarmEntity = relativeAlarm(relativeTo = TriggerRelationship.END),
            referenceEntity = allDayEvent(),
            lastAlarmOccurrenceIndex = null,
            timeZone = ZoneOffset.ofHours(14)
        )

        assertEquals(Instant.parse("2026-07-20T09:59:59.999999999Z"), occurrence?.triggerAt)
    }

    @Test
    fun repetition_advancesIndexAndStopsAtRepeatCount() {
        val alarm = relativeAlarm(interval = Duration.ofMinutes(5), repeat = 2)

        val firstRepeat = calculateNextAlarmOccurrenceEntity(
            alarm,
            timedEvent(),
            lastAlarmOccurrenceIndex = 0,
            timeZone = ZoneOffset.UTC
        )
        val lastRepeat = calculateNextAlarmOccurrenceEntity(
            alarm,
            timedEvent(),
            lastAlarmOccurrenceIndex = 1,
            timeZone = ZoneOffset.UTC
        )
        val exhausted = calculateNextAlarmOccurrenceEntity(
            alarm,
            timedEvent(),
            lastAlarmOccurrenceIndex = 2,
            timeZone = ZoneOffset.UTC
        )

        assertEquals(1L, firstRepeat?.index)
        assertEquals(START.plusSeconds(5 * 60), firstRepeat?.triggerAt)
        assertEquals(2L, lastRepeat?.index)
        assertEquals(START.plusSeconds(10 * 60), lastRepeat?.triggerAt)
        assertNull(exhausted)
    }

    @Test
    fun alarmWithoutRepetition_hasNoOccurrenceAfterFirst() {
        assertNull(
            calculateNextAlarmOccurrenceEntity(
                relativeAlarm(),
                timedEvent(),
                lastAlarmOccurrenceIndex = 0,
                timeZone = ZoneOffset.UTC
            )
        )
    }

    private fun relativeAlarm(
        relativeTo: TriggerRelationship = TriggerRelationship.START,
        offset: Duration = Duration.ZERO,
        interval: Duration? = null,
        repeat: Int? = null
    ) = AlarmEntity(
        id = 7,
        refUid = "event-1",
        action = ActionType.DISPLAY,
        summary = null,
        description = "Reminder",
        triggerType = TriggerType.RELATIVE,
        triggerRelativeTo = relativeTo,
        triggerOffset = offset,
        triggerAt = null,
        interval = interval,
        repeat = repeat
    )

    private fun absoluteAlarm(triggerAt: Instant) = AlarmEntity(
        id = 7,
        refUid = "event-1",
        action = ActionType.DISPLAY,
        summary = null,
        description = "Reminder",
        triggerType = TriggerType.ABSOLUTE,
        triggerRelativeTo = null,
        triggerOffset = null,
        triggerAt = triggerAt,
        interval = null,
        repeat = null
    )

    private fun timedEvent() = EventEntity(
        uid = "event-1",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        summary = null,
        description = null,
        location = null,
        isAllDayEvent = false,
        startAt = START,
        endAt = END,
        duration = null,
        startDate = null,
        endDate = null,
        period = null,
        actualEndAt = END,
        actualEndDate = null,
        priority = null,
        transparency = null,
        recurrenceRule = null,
        status = null
    )

    private fun allDayEvent() = EventEntity(
        uid = "event-1",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        summary = null,
        description = null,
        location = null,
        isAllDayEvent = true,
        startAt = null,
        endAt = null,
        duration = null,
        startDate = DATE,
        endDate = null,
        period = null,
        actualEndAt = null,
        actualEndDate = DATE,
        priority = null,
        transparency = null,
        recurrenceRule = null,
        status = null
    )

    private companion object {
        val START: Instant = Instant.parse("2026-07-20T01:00:00Z")
        val END: Instant = Instant.parse("2026-07-20T03:00:00Z")
        val DATE: LocalDate = LocalDate.of(2026, 7, 20)
    }
}
