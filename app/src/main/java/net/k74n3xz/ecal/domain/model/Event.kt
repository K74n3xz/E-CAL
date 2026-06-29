package net.k74n3xz.ecal.domain.model

import androidx.annotation.IntRange
import net.k74n3xz.ecal.domain.model.enumeration.event.TimeTransparency
import net.k74n3xz.ecal.domain.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.data.calendar.utils.generateEventUid
import java.time.Instant

data class Event(
    val uid: String = generateEventUid(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startAt: Instant = Instant.now(),
    val isAllDayEvent: Boolean = false,
    val endAt: Instant? = null,
    @field:IntRange(from = 0, to = 9) val priority: Int? = null,
    val transparency: TimeTransparency? = null,  // Default value is OPAQUE.
    val recurrenceRule: String? = null,
    val status: EventStatus? = null,
    val alarms: List<Alarm> = emptyList()
) {
    init {
        require(priority == null || priority in 0..9) { "The priority must be specified in the range 0 to 9." }
    }
}