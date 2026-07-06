package net.k74n3xz.ecal.core.application.usecase

import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.repository.EventRepository
import net.k74n3xz.ecal.core.model.Event

class SaveEventUseCase(
    private val eventRepository: EventRepository,
    private val alarmOccurrenceReconciler: AlarmOccurrenceReconciler
) {
    suspend operator fun invoke(event: Event) {
        eventRepository.saveEvent(event)
        alarmOccurrenceReconciler.request()
    }
}