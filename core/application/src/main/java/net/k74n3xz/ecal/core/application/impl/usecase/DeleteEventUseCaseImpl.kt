package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.outbound.repository.EventRepository

internal class DeleteEventUseCaseImpl(
    private val eventRepository: EventRepository,
    private val alarmReconciler: AlarmReconciler
) : DeleteEventUseCase {
    override suspend operator fun invoke(eventUid: String) {
        eventRepository.deleteEventByUid(eventUid)
        alarmReconciler.request()
    }
}
