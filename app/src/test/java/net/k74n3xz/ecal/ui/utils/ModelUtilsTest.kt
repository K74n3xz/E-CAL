package net.k74n3xz.ecal.ui.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelUtilsTest {
    private val hongKong = ZoneId.of("Asia/Hong_Kong")

    @Test
    fun generateEventUid_returnsUniqueUuidV7ValuesWithEventSuffix() {
        val generated = List(1_000) { generateEventUid() }

        generated.forEach { uid ->
            assertTrue(uid.endsWith("-ECAL_event"))
            assertEquals(7, UUID.fromString(uid.removeSuffix("-ECAL_event")).version())
        }
        assertEquals(generated.size, generated.toSet().size)
    }

    @Test
    fun formatTimeRange_formatsEveryTimingShape() = withFormattingLocale(Locale.US) {
        val cases = listOf(
            EventTiming.Timed.InstantTiming(Instant.parse("2026-06-28T01:30:00Z")) to
                "6/28/26, 9:30\u202fAM",
            EventTiming.Timed.RangeTiming(
                Instant.parse("2026-06-28T01:30:00Z"),
                Instant.parse("2026-06-28T03:00:00Z")
            ) to "6/28/26, 9:30\u202fAM - 6/28/26, 11:00\u202fAM",
            EventTiming.Timed.DurationTiming(
                Instant.parse("2026-06-28T01:30:00Z"),
                Duration.ofMinutes(90)
            ) to "6/28/26, 9:30\u202fAM | PT1H30M",
            EventTiming.AllDay.SingleDateTiming(LocalDate.of(2026, 6, 28)) to "6/28/26",
            EventTiming.AllDay.RangeDateTiming(
                LocalDate.of(2026, 6, 28),
                LocalDate.of(2026, 6, 29)
            ) to "6/28/26 - 6/29/26",
            EventTiming.AllDay.PeriodDateTiming(
                LocalDate.of(2026, 6, 28),
                Period.ofDays(2)
            ) to "6/28/26 | P2D"
        )

        cases.forEachIndexed { index, (timing, expected) ->
            val event = Event(uid = "event-$index", schedule = timing)
            assertEquals(expected, event.formatTimeRange(FormatStyle.SHORT, hongKong))
        }
    }

    @Test
    fun formatTimeRange_appliesZoneAndRequestedDateStyle() = withFormattingLocale(Locale.US) {
        val timed = Event(
            uid = "cross-date",
            schedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-06-28T23:30:00Z"))
        )
        val allDay = Event(
            uid = "long-style",
            schedule = EventTiming.AllDay.SingleDateTiming(LocalDate.of(2026, 6, 28))
        )

        assertEquals("6/29/26, 7:30\u202fAM", timed.formatTimeRange(FormatStyle.SHORT, hongKong))
        assertEquals("June 28, 2026", allDay.formatTimeRange(FormatStyle.LONG, ZoneId.of("UTC")))
    }

    private fun withFormattingLocale(locale: Locale, assertion: () -> Unit) {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.Category.FORMAT, locale)
            assertion()
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original)
        }
    }
}
