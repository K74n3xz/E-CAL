package net.k74n3xz.ecal.core.application.port.`in`.usecase

import net.k74n3xz.ecal.core.model.Event

interface SaveEventUseCase {
    suspend operator fun invoke(event: Event)
}
