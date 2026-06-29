package net.k74n3xz.ecal.data.calendar.utils

import net.k74n3xz.ecal.domain.model.Event
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun Event.formatTimeRange(dateTimeStyle: FormatStyle, zone: ZoneId): String {
    val formatter = if (isAllDayEvent) {
        DateTimeFormatter.ofLocalizedDate(dateTimeStyle)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(dateTimeStyle)
    }.withZone(zone)

    return if (endAt == null) {
        "${formatter.format(startAt)}"
    } else {
        "${formatter.format(startAt)} - ${formatter.format(endAt)}"
    }
}