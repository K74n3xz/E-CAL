package net.k74n3xz.ecal.core.data.database

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun supportedTypes_roundTrip() {
        val instant = Instant.parse("2026-06-28T12:34:56.123456789Z")
        val localDate = LocalDate.of(2026, 6, 28)
        val duration = Duration.ofDays(2).plusHours(3).plusNanos(456)
        val period = Period.of(1, 2, 3)

        assertEquals(instant, converters.toInstant(converters.fromInstant(instant)))
        assertEquals(localDate, converters.toLocalDate(converters.fromLocalDate(localDate)))
        assertEquals(duration, converters.toDuration(converters.fromDuration(duration)))
        assertEquals(period, converters.toPeriod(converters.fromPeriod(period)))
    }

    @Test
    fun nullValues_remainNull() {
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
        assertNull(converters.fromDuration(null))
        assertNull(converters.toDuration(null))
        assertNull(converters.fromPeriod(null))
        assertNull(converters.toPeriod(null))
    }

    @Test
    fun zeroAndNegativeTemporalAmounts_roundTrip() {
        listOf(Duration.ZERO, Duration.ofMinutes(-90)).forEach { value ->
            assertEquals(value, converters.toDuration(converters.fromDuration(value)))
        }
        listOf(Period.ZERO, Period.ofDays(-2)).forEach { value ->
            assertEquals(value, converters.toPeriod(converters.fromPeriod(value)))
        }
    }

    @Test
    fun malformedValues_areRejected() {
        assertThrows(DateTimeParseException::class.java) {
            converters.toInstant("not-an-instant")
        }
        assertThrows(DateTimeParseException::class.java) {
            converters.toDuration("90 minutes")
        }
        assertThrows(DateTimeParseException::class.java) {
            converters.toPeriod("one month")
        }
    }
}
