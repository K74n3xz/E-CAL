package net.k74n3xz.ecal.core.application.port.inbound.service

import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Attendee

interface AttendeeQueryService {
    suspend fun findAttendeeById(attendeeId: Long): Attendee?
    fun observeAllAttendees(): Flow<List<Attendee>>
}
