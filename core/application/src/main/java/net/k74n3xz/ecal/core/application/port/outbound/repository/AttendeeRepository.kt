package net.k74n3xz.ecal.core.application.port.outbound.repository

import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Attendee

interface AttendeeRepository {
    suspend fun getAttendeeById(attendeeId: Long): Attendee?
    fun observeAllAttendees(): Flow<List<Attendee>>
    suspend fun saveAttendee(attendee: Attendee)
    suspend fun deleteAttendeeById(attendeeId: Long)
}
