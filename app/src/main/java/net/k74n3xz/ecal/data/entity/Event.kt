package net.k74n3xz.ecal.data.entity

import androidx.annotation.IntRange
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.k74n3xz.ecal.data.entity.embedded.RecurrenceRule
import net.k74n3xz.ecal.data.entity.enumeration.event.EventStatus
import net.k74n3xz.ecal.data.entity.enumeration.TimeTransparency
import java.time.Instant

@Entity(tableName = "event")
data class Event(
    /* Metadata */
    @PrimaryKey val uid: String,
    // TODO: Uniform Resource Locator (3.8.4.6)
    val createdAt: Instant,
    val updatedAt: Instant,

    /* Access Control */
    // TODO: Classification (3.8.1.3)

    /* Details */
    val summary: String? = null,
    // TODO: Support for "altrepparam" and "languageparam" in Summary (3.8.1.12)
    val description: String? = null,
    // TODO: Organizer (3.8.4.3)
    // TODO: Geographic Position (3.8.1.6)
    val location: String? = null,

    /* Property */
    val startAt: Instant,
    val isAllDayEvent: Boolean = false,
    val endAt: Instant? = null,
    // TODO: Duration (3.8.2.5)
    @field:IntRange(from = 0, to = 9) val priority: Int = 0,
    val transparency: TimeTransparency = TimeTransparency.OPAQUE,
    @Embedded val recurrenceRule: RecurrenceRule? = null,

    /* State */
    // TODO: Sequence Number (3.8.7.4), and see also Component Revisions (2.1.4) in RFC 5546
    val status: EventStatus

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
    *   "val unresolvedProperties: String?" for x-prop / iana-prop.
    * */
) {
    init {
        require(priority in 0..9) { "The priority must be specified in the range 0 to 9." }
    }
}