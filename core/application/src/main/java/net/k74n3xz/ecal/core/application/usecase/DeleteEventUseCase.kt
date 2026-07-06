package net.k74n3xz.ecal.core.application.usecase

import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.repository.EventRepository

class DeleteEventUseCase(
    private val eventRepository: EventRepository,
    private val alarmOccurrenceReconciler: AlarmOccurrenceReconciler
) {
    suspend operator fun invoke(eventUid: String) {
        eventRepository.deleteEventByUid(eventUid)
        alarmOccurrenceReconciler.request()
    }
}