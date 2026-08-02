package net.k74n3xz.ecal.core.application.port.`in`.usecase

interface DeleteEventUseCase {
    suspend operator fun invoke(eventUid: String)
}
