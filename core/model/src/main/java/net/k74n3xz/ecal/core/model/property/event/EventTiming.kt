package net.k74n3xz.ecal.core.model.property.event

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import net.k74n3xz.ecal.core.model.utils.UtcZoneId
import net.k74n3xz.ecal.core.model.utils.atEndOfDay

sealed interface EventTiming {
    val timeRange: TimeRange

    sealed interface Timed : EventTiming {
        data class InstantTiming(val at: Instant) : Timed {
            override val timeRange: TimeRange
                get() = TimeRange(at, at)
        }

        data class RangeTiming(val startAt: Instant, val endAt: Instant) : Timed {
            override val timeRange: TimeRange
                get() = TimeRange(startAt, endAt)

            init {
                require(endAt > startAt) {
                    "The end time must be later than the start time."
                }
            }
        }

        data class DurationTiming(val startAt: Instant, val duration: Duration) : Timed {
            override val timeRange: TimeRange
                get() = TimeRange(startAt, startAt + duration)

            init {
                require(!(duration.isZero || duration.isNegative)) {
                    "The duration must be positive."
                }
            }
        }
    }

    sealed interface AllDay : EventTiming {
        val dateRange: DateRange

        data class SingleDateTiming(val date: LocalDate) : AllDay {
            override val dateRange: DateRange
                get() = DateRange(date, date)

            override val timeRange: TimeRange
                get() = dateRange.toTimeRange()
        }

        data class RangeDateTiming(val startDate: LocalDate, val endDate: LocalDate) : AllDay {
            override val dateRange: DateRange
                get() = DateRange(startDate, endDate)

            override val timeRange: TimeRange
                get() = dateRange.toTimeRange()

            init {
                require(endDate > startDate) {
                    "The end time must be later than the start time."
                }
            }
        }

        data class PeriodDateTiming(val startDate: LocalDate, val period: Period) : AllDay {
            override val dateRange: DateRange
                get() = DateRange(startDate, (startDate + period).minusDays(1).coerceAtLeast(startDate))

            override val timeRange: TimeRange
                get() = dateRange.toTimeRange()

            init {
                require(!(startDate + period).isBefore(startDate)) {
                    "The period can't be negative."
                }
            }
        }
    }
}

private fun DateRange.toTimeRange(): TimeRange = TimeRange(
    start = startDate.atStartOfDay(UtcZoneId).toInstant(),
    end = endDate.atEndOfDay(UtcZoneId).toInstant()
)
