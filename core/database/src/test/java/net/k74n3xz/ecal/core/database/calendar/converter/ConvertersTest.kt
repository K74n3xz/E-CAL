package net.k74n3xz.ecal.core.database.calendar.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun instant_roundTrips() {
        val value = Instant.parse("2026-06-28T12:34:56.123456789Z")

        assertEquals(value, converters.toInstant(converters.fromInstant(value)))
    }

    @Test
    fun duration_roundTrips() {
        val value = Duration.ofDays(2).plusHours(3).plusNanos(456)

        assertEquals(value, converters.toDuration(converters.fromDuration(value)))
    }

    @Test
    fun nullValues_remainNull() {
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
        assertNull(converters.fromDuration(null))
        assertNull(converters.toDuration(null))
    }

    @Test
    fun zeroAndNegativeDurations_roundTrip() {
        listOf(Duration.ZERO, Duration.ofMinutes(-90)).forEach { value ->
            assertEquals(value, converters.toDuration(converters.fromDuration(value)))
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
    }
}
