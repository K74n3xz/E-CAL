package net.k74n3xz.ecal.core.model.property.event

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EventTimingTest {
    @Test
    fun instantTiming_usesSameInstantForBothRangeBounds() {
        val at = Instant.parse("2026-07-09T03:00:00Z")

        val timing = EventTiming.Timed.InstantTiming(at)

        assertEquals(TimeRange(at, at), timing.timeRange)
    }

    @Test
    fun rangeTiming_derivesTimeRange() {
        val start = Instant.parse("2026-07-09T03:00:00Z")
        val end = Instant.parse("2026-07-09T04:00:00Z")

        val timing = EventTiming.Timed.RangeTiming(startAt = start, endAt = end)

        assertEquals(TimeRange(start, end), timing.timeRange)
    }

    @Test
    fun rangeTiming_rejectsEqualOrReversedBounds() {
        val start = Instant.parse("2026-07-09T03:00:00Z")

        listOf(start, start.minusSeconds(1)).forEach { invalidEnd ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                EventTiming.Timed.RangeTiming(startAt = start, endAt = invalidEnd)
            }

            assertEquals("The end time must be later than the start time.", error.message)
        }
    }

    @Test
    fun durationTiming_derivesEndFromPositiveDuration() {
        val start = Instant.parse("2026-07-09T03:00:00Z")

        val timing = EventTiming.Timed.DurationTiming(
            startAt = start,
            duration = Duration.ofMinutes(90)
        )

        assertEquals(TimeRange(start, Instant.parse("2026-07-09T04:30:00Z")), timing.timeRange)
    }

    @Test
    fun durationTiming_rejectsZeroOrNegativeDuration() {
        listOf(Duration.ZERO, Duration.ofNanos(-1)).forEach { invalidDuration ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                EventTiming.Timed.DurationTiming(
                    startAt = Instant.parse("2026-07-09T03:00:00Z"),
                    duration = invalidDuration
                )
            }

            assertEquals("The duration must be positive.", error.message)
        }
    }

    @Test
    fun singleDateTiming_usesInclusiveUtcDayBounds() {
        val date = LocalDate.of(2026, 7, 9)

        val timing = EventTiming.AllDay.SingleDateTiming(date)

        assertEquals(DateRange(date, date), timing.dateRange)
        assertEquals(
            TimeRange(
                Instant.parse("2026-07-09T00:00:00Z"),
                Instant.parse("2026-07-09T23:59:59.999999999Z")
            ),
            timing.timeRange
        )
    }

    @Test
    fun rangeDateTiming_usesInclusiveUtcDateBounds() {
        val start = LocalDate.of(2026, 7, 9)
        val end = LocalDate.of(2026, 7, 11)

        val timing = EventTiming.AllDay.RangeDateTiming(startDate = start, endDate = end)

        assertEquals(DateRange(start, end), timing.dateRange)
        assertEquals(
            TimeRange(
                Instant.parse("2026-07-09T00:00:00Z"),
                Instant.parse("2026-07-11T23:59:59.999999999Z")
            ),
            timing.timeRange
        )
    }

    @Test
    fun rangeDateTiming_rejectsEqualOrReversedBounds() {
        val start = LocalDate.of(2026, 7, 9)

        listOf(start, start.minusDays(1)).forEach { invalidEnd ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                EventTiming.AllDay.RangeDateTiming(startDate = start, endDate = invalidEnd)
            }

            assertEquals("The end time must be later than the start time.", error.message)
        }
    }

    @Test
    fun periodDateTiming_convertsPositivePeriodToInclusiveDateRange() {
        val start = LocalDate.of(2026, 7, 9)

        val timing = EventTiming.AllDay.PeriodDateTiming(startDate = start, period = Period.ofDays(3))

        assertEquals(DateRange(start, LocalDate.of(2026, 7, 11)), timing.dateRange)
        assertEquals(
            TimeRange(
                Instant.parse("2026-07-09T00:00:00Z"),
                Instant.parse("2026-07-11T23:59:59.999999999Z")
            ),
            timing.timeRange
        )
    }

    @Test
    fun periodDateTiming_treatsZeroPeriodAsSingleDay() {
        val start = LocalDate.of(2026, 7, 9)

        val timing = EventTiming.AllDay.PeriodDateTiming(startDate = start, period = Period.ZERO)

        assertEquals(DateRange(start, start), timing.dateRange)
    }

    @Test
    fun periodDateTiming_acceptsMixedPeriodWhenEffectiveEndIsAfterStart() {
        val start = LocalDate.of(2026, 7, 9)

        val timing = EventTiming.AllDay.PeriodDateTiming(
            startDate = start,
            period = Period.of(0, 1, -1)
        )

        assertEquals(DateRange(start, LocalDate.of(2026, 8, 7)), timing.dateRange)
    }

    @Test
    fun periodDateTiming_rejectsMixedPeriodWhenEffectiveEndIsBeforeStart() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            EventTiming.AllDay.PeriodDateTiming(
                startDate = LocalDate.of(2026, 7, 9),
                period = Period.of(0, 1, -40)
            )
        }

        assertEquals("The period can't be negative.", error.message)
    }

    @Test
    fun periodDateTiming_rejectsNegativePeriod() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            EventTiming.AllDay.PeriodDateTiming(
                startDate = LocalDate.of(2026, 7, 9),
                period = Period.ofDays(-1)
            )
        }

        assertEquals("The period can't be negative.", error.message)
    }
}
