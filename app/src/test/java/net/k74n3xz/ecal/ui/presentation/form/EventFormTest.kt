package net.k74n3xz.ecal.ui.presentation.form

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.event.EventStatus
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.model.property.event.TimeTransparency
import net.k74n3xz.ecal.ui.presentation.form.enumeration.event.TimingMode
import net.k74n3xz.ecal.ui.presentation.form.error.DateTimeFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.NumberFieldError
import net.k74n3xz.ecal.ui.presentation.form.utils.toEventForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class EventFormTest {
    private val zone = ZoneId.of("Asia/Hong_Kong")

    @Test
    fun converterAndResolve_roundTripEveryTimingShape() {
        val timings = listOf(
            EventTiming.Timed.InstantTiming(Instant.parse("2026-07-20T03:00:00Z")),
            EventTiming.Timed.RangeTiming(
                Instant.parse("2026-07-20T03:00:00Z"),
                Instant.parse("2026-07-20T04:00:00Z")
            ),
            EventTiming.Timed.DurationTiming(
                Instant.parse("2026-07-20T03:00:00Z"),
                Duration.ofMinutes(30)
            ),
            EventTiming.AllDay.SingleDateTiming(LocalDate.of(2026, 7, 20)),
            EventTiming.AllDay.RangeDateTiming(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22)),
            EventTiming.AllDay.PeriodDateTiming(LocalDate.of(2026, 7, 20), Period.ofDays(3))
        )

        timings.forEachIndexed { index, timing ->
            val event = Event(uid = "event-$index", schedule = timing)
            val resolved = event.toEventForm(zone).resolve(event, emptyList(), zone).getOrThrow()
            assertEquals(timing, resolved.schedule)
        }
    }

    @Test
    fun converter_mapsMetadataAndNullableTextSemantics() {
        val event = Event(
            uid = "metadata",
            summary = null,
            description = "",
            location = "Room",
            schedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-07-20T03:00:00Z")),
            priority = 4,
            transparency = TimeTransparency.TRANSPARENT,
            status = EventStatus.CONFIRMED
        )

        val form = event.toEventForm(zone)

        assertTrue(form.isSummaryClear)
        assertFalse(form.isDescriptionClear)
        assertFalse(form.isLocationClear)
        assertEquals("Room", form.location.text.toString())
        assertEquals(4, form.priority)
        assertEquals(TimeTransparency.TRANSPARENT, form.timeTransparency)
        assertEquals(EventStatus.CONFIRMED, form.status)
        assertEquals(LocalDate.of(2026, 7, 20), form.startDate)
        assertEquals(LocalTime.of(11, 0), form.startTime)
    }

    @Test
    fun resolve_appliesEditsReplacesAlarmsAndPreservesUneditedIdentity() {
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")
        val original = Event(
            uid = "event-1",
            createdAt = createdAt,
            updatedAt = createdAt,
            summary = "old",
            description = "old description",
            location = "old location",
            schedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-07-20T03:00:00Z")),
            recurrenceRule = "FREQ=DAILY"
        )
        val alarm = Alarm(
            id = 2,
            action = Action.Display("new alarm"),
            trigger = Trigger.RelativeTrigger(offset = Duration.ofMinutes(-5))
        )
        val before = Instant.now()
        val form = original.toEventForm(zone).apply {
            summary.replaceText("")
            isSummaryClear = false
            isDescriptionClear = true
            location.replaceText("new location")
            priority = 8
            timeTransparency = TimeTransparency.OPAQUE
            status = EventStatus.TENTATIVE
        }

        val resolved = form.resolve(original, listOf(alarm), zone).getOrThrow()

        assertEquals(original.uid, resolved.uid)
        assertEquals(createdAt, resolved.createdAt)
        assertEquals("FREQ=DAILY", resolved.recurrenceRule)
        assertEquals("", resolved.summary)
        assertNull(resolved.description)
        assertEquals("new location", resolved.location)
        assertEquals(8, resolved.priority)
        assertEquals(TimeTransparency.OPAQUE, resolved.transparency)
        assertEquals(EventStatus.TENTATIVE, resolved.status)
        assertEquals(listOf(alarm), resolved.alarms)
        assertFalse(resolved.updatedAt.isBefore(before))
    }

    @Test
    fun resolve_buildsEditedTimedAndAllDaySchedulesUsingTimeZone() {
        val original = Event(
            uid = "event",
            schedule = EventTiming.Timed.InstantTiming(Instant.EPOCH)
        )
        val form = original.toEventForm(zone).apply {
            startDate = LocalDate.of(2026, 7, 20)
            startTime = LocalTime.of(11, 30)
            endDate = LocalDate.of(2026, 7, 20)
            endTime = LocalTime.of(12, 30)
            timingMode = TimingMode.RANGE
        }

        assertEquals(
            EventTiming.Timed.RangeTiming(
                Instant.parse("2026-07-20T03:30:00Z"),
                Instant.parse("2026-07-20T04:30:00Z")
            ),
            form.resolve(original, emptyList(), zone).getOrThrow().schedule
        )

        form.isAllDay = true
        form.timingMode = TimingMode.DURATION
        form.period = Period.ofDays(2)
        assertEquals(
            EventTiming.AllDay.PeriodDateTiming(LocalDate.of(2026, 7, 20), Period.ofDays(2)),
            form.resolve(original, emptyList(), zone).getOrThrow().schedule
        )
    }

    @Test
    fun rangeAndDurationValidation_matchesSelectedTimingKind() {
        val form = Event(
            uid = "event",
            schedule = EventTiming.Timed.InstantTiming(Instant.EPOCH)
        ).toEventForm(zone)

        form.timingMode = TimingMode.RANGE
        form.endDate = form.startDate
        form.endTime = form.startTime
        assertEquals(DateTimeFieldError.EndNotAfterStart, form.timingFieldError)

        form.timingMode = TimingMode.DURATION
        form.duration = Duration.ZERO
        assertEquals(DateTimeFieldError.EndNotAfterStart, form.timingFieldError)

        form.isAllDay = true
        form.period = Period.ofDays(-1)
        assertEquals(DateTimeFieldError.EndBeforeStart, form.timingFieldError)
        assertFalse(form.isValid)
        assertTrue(
            form.resolve(
                Event("event", schedule = EventTiming.Timed.InstantTiming(Instant.EPOCH)),
                emptyList(),
                zone
            ).isFailure
        )
    }

    @Test
    fun allDayDurationValidation_acceptsNetPositiveMixedPeriodAndRejectsNetNegativePeriod() {
        val form = Event(
            uid = "event",
            schedule = EventTiming.AllDay.SingleDateTiming(LocalDate.of(2026, 7, 20))
        ).toEventForm(zone).apply {
            timingMode = TimingMode.DURATION
        }

        form.period = Period.of(0, 1, -1)
        assertNull(form.timingFieldError)
        assertTrue(form.isValid)

        form.period = Period.of(0, 1, -40)
        assertEquals(DateTimeFieldError.EndBeforeStart, form.timingFieldError)
        assertFalse(form.isValid)
    }

    @Test
    fun allDayDurationValidation_reportsPeriodOutsideLocalDateRange() {
        val original = Event(
            uid = "event",
            schedule = EventTiming.AllDay.SingleDateTiming(LocalDate.of(2026, 7, 20))
        )
        val form = original.toEventForm(zone).apply {
            timingMode = TimingMode.DURATION
            period = Period.ofYears(Int.MAX_VALUE)
        }

        assertEquals(DateTimeFieldError.TooLongPeriod, form.timingFieldError)
        assertFalse(form.isValid)
        assertTrue(form.resolve(original, emptyList(), zone).isFailure)
    }

    @Test
    fun priorityOutsideDomain_isRejected() {
        val form = Event(
            uid = "event",
            schedule = EventTiming.Timed.InstantTiming(Instant.EPOCH)
        ).toEventForm(zone)

        form.priority = 10

        assertEquals(NumberFieldError.OutOfRange, form.priorityFieldError)
        assertFalse(form.isValid)
    }
}
