package net.k74n3xz.ecal.data.calendar.database.entity

import androidx.annotation.IntRange
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.eventcomponent.TimeTransparency
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.eventcomponent.EventStatus
import java.time.Instant

@Entity(tableName = "event_component")
data class EventComponent(
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
    val startAt: Instant,
    val isAllDayEvent: Boolean = false,  // the Value Type of DTSTART, false = DATE-TIME and true = DATE.
    val endAt: Instant?,
    // TODO: Duration (3.8.2.5)
    @field:IntRange(from = 0, to = 9) val priority: Int?,
    val transparency: TimeTransparency?,  // Default value is OPAQUE.
    val recurrenceRule: String?,

    /* State */
    // TODO: Sequence Number (3.8.7.4), and see also Component Revisions (2.1.4) in RFC 5546
    val status: EventStatus?,

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

    /* Source Copy */
    val rawIcs: String
) {
    init {
        require(priority == null || priority in 0..9) { "The priority must be specified in the range 0 to 9." }
    }
}