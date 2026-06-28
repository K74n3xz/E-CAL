package net.k74n3xz.ecal.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class JavaTimeUtilsTest {
    @Test
    fun atEndOfDay_returnsLastNanosecondBeforeNextDay() {
        val zone = ZoneId.of("UTC")

        assertEquals(
            ZonedDateTime.parse("2026-02-01T23:59:59.999999999Z[UTC]"),
            LocalDate.of(2026, 2, 1).atEndOfDay(zone)
        )
    }

    @Test
    fun atEndOfDay_handlesMonthBoundary() {
        val zone = ZoneId.of("UTC")

        assertEquals(
            ZonedDateTime.parse("2026-02-28T23:59:59.999999999Z[UTC]"),
            LocalDate.of(2026, 2, 28).atEndOfDay(zone)
        )
    }

    @Test
    fun atEndOfDay_usesActualNextMidnightAcrossDstTransition() {
        val zone = ZoneId.of("America/New_York")

        assertEquals(
            ZonedDateTime.parse("2026-03-08T23:59:59.999999999-04:00[America/New_York]"),
            LocalDate.of(2026, 3, 8).atEndOfDay(zone)
        )
    }

    @Test
    fun atEndOfDay_handlesLeapDay() {
        val zone = ZoneId.of("UTC")

        assertEquals(
            ZonedDateTime.parse("2024-02-29T23:59:59.999999999Z[UTC]"),
            LocalDate.of(2024, 2, 29).atEndOfDay(zone)
        )
    }

    @Test
    fun atEndOfDay_usesPostTransitionOffsetAcrossDstFallback() {
        val zone = ZoneId.of("America/New_York")

        assertEquals(
            ZonedDateTime.parse("2026-11-01T23:59:59.999999999-05:00[America/New_York]"),
            LocalDate.of(2026, 11, 1).atEndOfDay(zone)
        )
    }
}