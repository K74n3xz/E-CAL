package net.k74n3xz.ecal.data.calendar.utils

import net.k74n3xz.ecal.data.calendar.model.Event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.FormatStyle
import java.util.Locale

class ModelFormatterTest {
    private val zone = ZoneId.of("Asia/Hong_Kong")

    @Test
    fun formatTimeRange_formatsTimedEventWithEnd() = withLocale(Locale.US) {
        val event = Event(
            startAt = Instant.parse("2026-06-28T01:30:00Z"),
            endAt = Instant.parse("2026-06-28T03:00:00Z")
        )

        assertEquals(
            "6/28/26, 9:30\u202fAM - 6/28/26, 11:00\u202fAM",
            event.formatTimeRange(FormatStyle.SHORT, zone)
        )
    }

    @Test
    fun formatTimeRange_formatsAllDayEventAsDates() = withLocale(Locale.US) {
        val event = Event(
            startAt = Instant.parse("2026-06-28T00:00:00Z"),
            endAt = Instant.parse("2026-06-29T00:00:00Z"),
            isAllDayEvent = true
        )

        assertEquals("6/28/26 - 6/29/26", event.formatTimeRange(FormatStyle.SHORT, zone))
    }

    @Test
    fun formatTimeRange_omitsSeparatorWhenEndIsAbsent() = withLocale(Locale.US) {
        val event = Event(startAt = Instant.parse("2026-06-28T01:30:00Z"))

        assertEquals("6/28/26, 9:30\u202fAM", event.formatTimeRange(FormatStyle.SHORT, zone))
    }

    @Test
    fun formatTimeRange_appliesZoneWhenInstantCrossesDateBoundary() = withLocale(Locale.US) {
        val event = Event(startAt = Instant.parse("2026-06-28T23:30:00Z"))

        assertEquals("6/29/26, 7:30\u202fAM", event.formatTimeRange(FormatStyle.SHORT, zone))
    }

    @Test
    fun formatTimeRange_supportsLongDateStyle() = withLocale(Locale.US) {
        val event = Event(
            startAt = Instant.parse("2026-06-28T00:00:00Z"),
            isAllDayEvent = true
        )

        assertEquals("June 28, 2026", event.formatTimeRange(FormatStyle.LONG, ZoneId.of("UTC")))
    }

    private fun withLocale(locale: Locale, assertion: () -> Unit) {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.Category.FORMAT, locale)
            assertion()
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original)
        }
    }
}