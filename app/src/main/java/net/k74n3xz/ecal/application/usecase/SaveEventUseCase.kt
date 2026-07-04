package net.k74n3xz.ecal.application.usecase

import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.domain.repository.EventRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val alarmOccurrenceReconciler: AlarmOccurrenceReconciler
) {
    suspend operator fun invoke(event: Event) {
        eventRepository.saveEvent(event)
        alarmOccurrenceReconciler.request()
    }
}