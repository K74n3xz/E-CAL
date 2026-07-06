package net.k74n3xz.ecal.core.model

import net.k74n3xz.ecal.core.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.core.model.enumeration.event.TimeTransparency
import org.jetbrains.annotations.Range
import java.time.Instant

data class Event(
    val uid: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startAt: Instant = Instant.now(),
    val isAllDayEvent: Boolean = false,
    val endAt: Instant? = null,
    val priority: @Range(from = 0, to = 9) Int? = null,
    val transparency: TimeTransparency? = null,  // Default value is OPAQUE, if exists.
    val recurrenceRule: String? = null,
    val status: EventStatus? = null,
    val alarms: List<Alarm> = emptyList()
) {
    init {
        require(priority == null || priority in 0..9) { "The priority must be specified in the range 0 to 9." }
    }
}