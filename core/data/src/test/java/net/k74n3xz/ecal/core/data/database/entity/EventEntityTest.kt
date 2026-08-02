package net.k74n3xz.ecal.core.data.database.entity

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EventEntityTest {
    @Test
    fun priority_acceptsBoundsAndRejectsValuesOutsideRange() {
        assertEquals(0, timedEvent(priority = 0).priority)
        assertEquals(9, timedEvent(priority = 9).priority)

        listOf(-1, 10).forEach { priority ->
            assertThrows(IllegalArgumentException::class.java) {
                timedEvent(priority = priority)
            }
        }
    }

    @Test
    fun timedEvent_requiresStartAndEffectiveEnd() {
        assertThrows(IllegalArgumentException::class.java) {
            timedEvent(startAt = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            timedEvent(actualEndAt = null)
        }
    }

    @Test
    fun timedEvent_rejectsDateOnlyValues() {
        assertThrows(IllegalArgumentException::class.java) {
            timedEvent(startDate = LocalDate.of(2026, 7, 20))
        }
    }

    @Test
    fun timedEvent_rejectsEndAndDurationTogether() {
        assertThrows(IllegalArgumentException::class.java) {
            timedEvent(endAt = END, duration = Duration.ofHours(1), actualEndAt = END)
        }
    }

    @Test
    fun timedEvent_rejectsInvalidEndDurationAndEffectiveEnd() {
        assertThrows(IllegalArgumentException::class.java) {
            timedEvent(endAt = START, actualEndAt = START)
        }
        listOf(Duration.ZERO, Duration.ofMinutes(-1)).forEach { duration ->
            assertThrows(IllegalArgumentException::class.java) {
                timedEvent(duration = duration)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            timedEvent(actualEndAt = START.minusSeconds(1))
        }
    }

    @Test
    fun allDayEvent_requiresStartAndEffectiveEndDates() {
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(startDate = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(actualEndDate = null)
        }
    }

    @Test
    fun allDayEvent_rejectsTimeBasedValues() {
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(startAt = START)
        }
    }

    @Test
    fun allDayEvent_rejectsEndDateAndPeriodTogether() {
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(endDate = END_DATE, period = Period.ofDays(1), actualEndDate = END_DATE)
        }
    }

    @Test
    fun allDayEvent_rejectsInvalidEndPeriodAndEffectiveEnd() {
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(endDate = START_DATE, actualEndDate = START_DATE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(period = Period.ofDays(-1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(actualEndDate = START_DATE.minusDays(1))
        }
    }

    @Test
    fun allDayEvent_acceptsMixedPeriodWhenEffectiveEndIsAfterStart() {
        val period = Period.of(0, 1, -1)

        val entity = allDayEvent(
            period = period,
            actualEndDate = START_DATE.plus(period).minusDays(1)
        )

        assertEquals(period, entity.period)
    }

    @Test
    fun allDayEvent_rejectsMixedPeriodWhenEffectiveEndIsBeforeStart() {
        assertThrows(IllegalArgumentException::class.java) {
            allDayEvent(period = Period.of(0, 1, -40))
        }
    }

    private fun timedEvent(
        startAt: Instant? = START,
        endAt: Instant? = null,
        duration: Duration? = null,
        startDate: LocalDate? = null,
        actualEndAt: Instant? = endAt ?: startAt,
        priority: Int? = null
    ) = EventEntity(
        uid = "event-1",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        summary = null,
        description = null,
        location = null,
        isAllDayEvent = false,
        startAt = startAt,
        endAt = endAt,
        duration = duration,
        startDate = startDate,
        endDate = null,
        period = null,
        actualEndAt = actualEndAt,
        actualEndDate = null,
        priority = priority,
        transparency = null,
        recurrenceRule = null,
        status = null
    )

    private fun allDayEvent(
        startDate: LocalDate? = START_DATE,
        endDate: LocalDate? = null,
        period: Period? = null,
        actualEndDate: LocalDate? = endDate ?: startDate,
        startAt: Instant? = null
    ) = EventEntity(
        uid = "event-1",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        summary = null,
        description = null,
        location = null,
        isAllDayEvent = true,
        startAt = startAt,
        endAt = null,
        duration = null,
        startDate = startDate,
        endDate = endDate,
        period = period,
        actualEndAt = null,
        actualEndDate = actualEndDate,
        priority = null,
        transparency = null,
        recurrenceRule = null,
        status = null
    )

    private companion object {
        val START: Instant = Instant.parse("2026-07-20T01:00:00Z")
        val END: Instant = START.plusSeconds(3600)
        val START_DATE: LocalDate = LocalDate.of(2026, 7, 20)
        val END_DATE: LocalDate = START_DATE.plusDays(1)
    }
}
