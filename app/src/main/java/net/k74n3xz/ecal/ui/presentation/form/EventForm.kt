package net.k74n3xz.ecal.ui.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.event.EventStatus
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.model.property.event.TimeTransparency
import net.k74n3xz.ecal.ui.presentation.form.enumeration.event.TimingMode
import net.k74n3xz.ecal.ui.presentation.form.error.DateTimeFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.NumberFieldError
import org.jetbrains.annotations.Range

@Stable
internal class EventForm(
    initialSummary: String?,
    initialDescription: String?,
    initialLocation: String?,
    initialSchedule: EventTiming,
    initialPriority:
    @Range(from = 0, to = 9)
    Int?,
    initialTimeTransparency: TimeTransparency?,
    initialStatus: EventStatus?,
    timeZone: ZoneId
) {
    val summary: TextFieldState = TextFieldState(initialSummary ?: "")
    var isSummaryClear: Boolean by mutableStateOf(initialSummary == null)

    val description: TextFieldState = TextFieldState(initialDescription ?: "")
    var isDescriptionClear: Boolean by mutableStateOf(initialDescription == null)

    val location: TextFieldState = TextFieldState(initialLocation ?: "")
    var isLocationClear: Boolean by mutableStateOf(initialLocation == null)

    var timingMode: TimingMode by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.AllDay.PeriodDateTiming -> TimingMode.DURATION
            is EventTiming.AllDay.RangeDateTiming -> TimingMode.RANGE
            is EventTiming.AllDay.SingleDateTiming -> TimingMode.POINT
            is EventTiming.Timed.DurationTiming -> TimingMode.DURATION
            is EventTiming.Timed.InstantTiming -> TimingMode.POINT
            is EventTiming.Timed.RangeTiming -> TimingMode.RANGE
        }
    )
    var isAllDay: Boolean by mutableStateOf(initialSchedule is EventTiming.AllDay)
    var startDate: LocalDate by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.AllDay -> initialSchedule.dateRange.startDate
            is EventTiming.Timed -> initialSchedule.timeRange.start.atZone(timeZone).toLocalDate()
        }
    )
    var startTime: LocalTime by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.Timed -> initialSchedule.timeRange.start.atZone(timeZone).toLocalTime()
            is EventTiming.AllDay -> LocalTime.MIDNIGHT
        }
    )
    var endDate: LocalDate by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.AllDay -> initialSchedule.dateRange.endDate
            is EventTiming.Timed -> initialSchedule.timeRange.end.atZone(timeZone).toLocalDate()
        }
    )
    var endTime: LocalTime by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.Timed -> initialSchedule.timeRange.end.atZone(timeZone).toLocalTime()
            is EventTiming.AllDay -> LocalTime.MIDNIGHT
        }
    )
    var duration: Duration by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.Timed.DurationTiming -> initialSchedule.duration
            else -> Duration.ofMinutes(15)
        }
    )
    var period: Period by mutableStateOf(
        when (initialSchedule) {
            is EventTiming.AllDay.PeriodDateTiming -> initialSchedule.period
            else -> Period.ofDays(1)
        }
    )
    val timingFieldError: DateTimeFieldError?
        get() = when (timingMode) {
            TimingMode.POINT -> null

            TimingMode.RANGE -> if (isAllDay) {
                if (endDate <= startDate) {
                    DateTimeFieldError.EndNotAfterStart
                } else {
                    null
                }
            } else {
                if (LocalDateTime.of(endDate, endTime) <= LocalDateTime.of(startDate, startTime)) {
                    DateTimeFieldError.EndNotAfterStart
                } else {
                    null
                }
            }

            TimingMode.DURATION -> {
                if (isAllDay) {
                    try {
                        if ((startDate + period).isBefore(startDate)) {
                            DateTimeFieldError.EndBeforeStart
                        } else {
                            null
                        }
                    } catch (_: DateTimeException) {
                        DateTimeFieldError.TooLongPeriod
                    }
                } else {
                    if (!duration.isPositive) {
                        DateTimeFieldError.EndNotAfterStart
                    } else {
                        null
                    }
                }
            }
        }

    var priority:
        @Range(from = 0, to = 9)
        Int? by mutableStateOf(initialPriority)
    val priorityFieldError: NumberFieldError?
        get() = if (priority == null || priority in 0..9) {
            null
        } else {
            NumberFieldError.OutOfRange
        }

    // Default value is OPAQUE, if exists.
    var timeTransparency: TimeTransparency? by mutableStateOf(initialTimeTransparency)

    // recurrenceRule

    var status: EventStatus? by mutableStateOf(initialStatus)

    val isValid: Boolean
        get() = timingFieldError == null && priorityFieldError == null

    fun resolve(originalEvent: Event, newAlarms: List<Alarm>, timeZone: ZoneId): Result<Event> = if (isValid) {
        Result.success(
            originalEvent.copy(
                updatedAt = Instant.now(),
                summary = if (isSummaryClear) null else summary.text.toString(),
                description = if (isDescriptionClear) null else description.text.toString(),
                location = if (isLocationClear) null else location.text.toString(),
                schedule = if (isAllDay) {
                    when (timingMode) {
                        TimingMode.POINT -> EventTiming.AllDay.SingleDateTiming(startDate)
                        TimingMode.RANGE -> EventTiming.AllDay.RangeDateTiming(startDate, endDate)
                        TimingMode.DURATION -> EventTiming.AllDay.PeriodDateTiming(startDate, period)
                    }
                } else {
                    when (timingMode) {
                        TimingMode.POINT -> EventTiming.Timed.InstantTiming(
                            ZonedDateTime.of(startDate, startTime, timeZone).toInstant()
                        )

                        TimingMode.RANGE -> EventTiming.Timed.RangeTiming(
                            ZonedDateTime.of(startDate, startTime, timeZone).toInstant(),
                            ZonedDateTime.of(endDate, endTime, timeZone).toInstant()
                        )

                        TimingMode.DURATION -> EventTiming.Timed.DurationTiming(
                            ZonedDateTime.of(startDate, startTime, timeZone).toInstant(),
                            duration
                        )
                    }
                },
                priority = priority,
                transparency = timeTransparency,
                status = status,
                alarms = newAlarms
            )
        )
    } else {
        Result.failure(IllegalArgumentException("Unresolvable input."))
    }
}
