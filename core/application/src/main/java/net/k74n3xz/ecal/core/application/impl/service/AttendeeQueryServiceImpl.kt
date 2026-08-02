package net.k74n3xz.ecal.core.application.impl.service

import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.application.port.`in`.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.out.repository.AttendeeRepository
import net.k74n3xz.ecal.core.model.Attendee

internal class AttendeeQueryServiceImpl(private val attendeeRepository: AttendeeRepository) : AttendeeQueryService {
    override suspend fun findAttendeeById(attendeeId: Long): Attendee? = attendeeRepository.getAttendeeById(attendeeId)

    override fun observeAllAttendees(): Flow<List<Attendee>> = attendeeRepository.observeAllAttendees()
}
