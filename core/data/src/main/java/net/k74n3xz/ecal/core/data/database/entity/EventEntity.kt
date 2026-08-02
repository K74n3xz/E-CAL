package net.k74n3xz.ecal.core.data.database.entity

import androidx.annotation.IntRange
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import net.k74n3xz.ecal.core.model.property.event.EventStatus
import net.k74n3xz.ecal.core.model.property.event.TimeTransparency

@Entity(tableName = "event")
internal data class EventEntity(
    /* Metadata */
    @PrimaryKey val uid: String,
    // TODO: Uniform Resource Locator (3.8.4.6)
    val createdAt: Instant,
    val updatedAt: Instant,

    /* Access Control */
    // TODO: Classification (3.8.1.3)

    /* Details */
    val summary: String?,
    // TODO: Support for "altrepparam" and "languageparam" in Summary (3.8.1.12)
    val description: String?,
    // TODO: Organizer (3.8.4.3)
    // TODO: Geographic Position (3.8.1.6)
    val location: String?,

    /* Property */
    val isAllDayEvent: Boolean = false, // the Value Type of DTSTART, false = DATE-TIME and true = DATE.
    val startAt: Instant?,
    val endAt: Instant?,
    val duration: Duration?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val period: Period?,
    val actualEndAt: Instant?,
    val actualEndDate: LocalDate?,
    // TODO: Duration (3.8.2.5)
    @field:IntRange(from = 0, to = 9) val priority: Int?,
    val transparency: TimeTransparency?, // Default value is OPAQUE.
    val recurrenceRule: String?,

    /* State */
    // TODO: Sequence Number (3.8.7.4), and see also Component Revisions (2.1.4) in RFC 5546
    val status: EventStatus?

    /*
     * TODO: The following are OPTIONAL, and MAY occur more than once.
     *   3.8.1.1.  Attachment
     *   3.8.4.1.  Attendee
     *   3.8.1.2.  Categories
     *   3.8.1.4.  Comment
     *   3.8.4.2.  Contact
     *   3.8.5.1.  Exception Date-Times
     *   3.8.8.3.  Request Status
     *   3.8.4.5.  Related To
     *   3.8.1.10.  Resources
     *   3.8.5.2.  Recurrence Date-Times
     * */
) {
    init {
        if (isAllDayEvent) {
            require(startAt == null && endAt == null && duration == null && actualEndAt == null) {
                "All-day events can't define time-based values: startAt, endAt, duration, and actualEndAt must all be null."
            }
            requireNotNull(startDate) {
                "startDate is required for an all-day event."
            }
            require(!(endDate != null && period != null)) {
                "`DTEND` and `DURATION` must not occur simultaneously."
            }
            require(endDate == null || endDate > startDate) {
                "The end date ($endDate) must be later than the start date ($startDate)."
            }
            require(period == null || !(startDate + period).isBefore(startDate)) {
                "The period ($period) must not be negative."
            }
            requireNotNull(actualEndDate) {
                "Unable to determine the effective end date for the all-day event (startDate=$startDate, endDate=$endDate, period=$period)."
            }
            require(actualEndDate >= startDate) {
                "The effective end date ($actualEndDate) must not be earlier than the start date ($startDate)."
            }
        } else {
            require(startDate == null && endDate == null && period == null && actualEndDate == null) {
                "Timed events cannot define date-only values: startDate, endDate, period, and actualEndDate must all be null."
            }
            requireNotNull(startAt) {
                "startAt is required for a timed event."
            }
            require(!(endAt != null && duration != null)) {
                "`DTEND` and `DURATION` must not occur simultaneously."
            }
            require(endAt == null || endAt > startAt) {
                "The end time ($endAt) must be later than the start time ($startAt)."
            }
            require(duration == null || duration.isPositive) {
                "The duration ($duration) must be greater than zero."
            }
            requireNotNull(actualEndAt) {
                "Unable to determine the effective end time for the timed event (startAt=$startAt, endAt=$endAt, duration=$duration)."
            }
            require(actualEndAt >= startAt) {
                "The effective end time ($actualEndAt) must not be earlier than the start time ($startAt)."
            }
        }

        require(priority == null || priority in 0..9) {
            "The priority must be specified in the range 0 to 9."
        }
    }
}
