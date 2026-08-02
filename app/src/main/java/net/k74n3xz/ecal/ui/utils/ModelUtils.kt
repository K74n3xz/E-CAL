package net.k74n3xz.ecal.ui.utils

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.event.EventTiming

@OptIn(ExperimentalUuidApi::class)
internal fun generateEventUid(): String = "${Uuid.generateV7()}-ECAL_event"

internal fun Event.formatTimeRange(dateTimeStyle: FormatStyle, zone: ZoneId): String {
    val schedule = schedule

    val formatter = when (schedule) {
        is EventTiming.AllDay -> DateTimeFormatter.ofLocalizedDate(dateTimeStyle)
        is EventTiming.Timed -> DateTimeFormatter.ofLocalizedDateTime(dateTimeStyle)
    }.withZone(zone)

    return when (schedule) {
        is EventTiming.Timed.InstantTiming -> "${formatter.format(schedule.at)}"

        is EventTiming.AllDay.SingleDateTiming -> "${formatter.format(schedule.date)}"

        is EventTiming.Timed.RangeTiming ->
            "${formatter.format(schedule.startAt)} - ${formatter.format(schedule.endAt)}"

        is EventTiming.AllDay.RangeDateTiming ->
            "${formatter.format(schedule.startDate)} - ${formatter.format(schedule.endDate)}"

        is EventTiming.Timed.DurationTiming -> "${formatter.format(schedule.startAt)} | ${schedule.duration}"

        is EventTiming.AllDay.PeriodDateTiming -> "${formatter.format(schedule.startDate)} | ${schedule.period}"
    }
}
