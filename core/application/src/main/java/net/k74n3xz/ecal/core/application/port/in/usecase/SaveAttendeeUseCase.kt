package net.k74n3xz.ecal.core.application.port.`in`.usecase

import net.k74n3xz.ecal.core.model.Attendee

interface SaveAttendeeUseCase {
    suspend operator fun invoke(attendee: Attendee)
}
