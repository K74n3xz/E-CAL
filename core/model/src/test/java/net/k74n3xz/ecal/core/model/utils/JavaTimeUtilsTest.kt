package net.k74n3xz.ecal.core.model.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class JavaTimeUtilsTest {
    @Test
    fun utcZoneId_isUtc() {
        assertEquals(ZoneId.of("UTC"), UtcZoneId)
    }

    @Test
    fun atEndOfDay_withoutZoneReturnsLastNanosecondBeforeNextDay() {
        assertEquals(
            LocalDateTime.parse("2026-02-01T23:59:59.999999999"),
            LocalDate.of(2026, 2, 1).atEndOfDay()
        )
    }

    @Test
    fun atEndOfDay_handlesMonthAndLeapYearBoundary() {
        assertEquals(
            LocalDateTime.parse("2024-02-29T23:59:59.999999999"),
            LocalDate.of(2024, 2, 29).atEndOfDay()
        )
    }

    @Test
    fun atEndOfDay_withZoneReturnsLastNanosecondBeforeNextMidnight() {
        assertEquals(
            ZonedDateTime.parse("2026-02-01T23:59:59.999999999Z[UTC]"),
            LocalDate.of(2026, 2, 1).atEndOfDay(UtcZoneId)
        )
    }

    @Test
    fun atEndOfDay_usesPostTransitionOffsetAcrossDstStart() {
        val zone = ZoneId.of("America/New_York")

        assertEquals(
            ZonedDateTime.parse("2026-03-08T23:59:59.999999999-04:00[America/New_York]"),
            LocalDate.of(2026, 3, 8).atEndOfDay(zone)
        )
    }

    @Test
    fun atEndOfDay_usesPostTransitionOffsetAcrossDstEnd() {
        val zone = ZoneId.of("America/New_York")

        assertEquals(
            ZonedDateTime.parse("2026-11-01T23:59:59.999999999-05:00[America/New_York]"),
            LocalDate.of(2026, 11, 1).atEndOfDay(zone)
        )
    }
}
