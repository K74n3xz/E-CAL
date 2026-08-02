package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.out.repository.AttendeeRepository
import net.k74n3xz.ecal.core.model.Attendee

internal class SaveAttendeeUseCaseImpl(private val attendeeRepository: AttendeeRepository) : SaveAttendeeUseCase {
    override suspend fun invoke(attendee: Attendee) {
        attendeeRepository.saveAttendee(attendee)
    }
}
