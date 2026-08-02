package net.k74n3xz.ecal.core.application.port.`in`.usecase

interface DeleteAttendeeUseCase {
    suspend operator fun invoke(attendeeId: Long)
}
