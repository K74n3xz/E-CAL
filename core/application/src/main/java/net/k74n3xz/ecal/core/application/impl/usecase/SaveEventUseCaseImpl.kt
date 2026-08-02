package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.outbound.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.application.port.outbound.repository.EventRepository
import net.k74n3xz.ecal.core.model.Event

internal class SaveEventUseCaseImpl(
    private val eventRepository: EventRepository,
    private val alarmReconciler: AlarmReconciler,
    private val timeZoneProvider: TimeZoneProvider
) : SaveEventUseCase {
    override suspend operator fun invoke(event: Event) {
        eventRepository.saveEvent(event, timeZoneProvider.currentTimeZone())
        alarmReconciler.request()
    }
}
