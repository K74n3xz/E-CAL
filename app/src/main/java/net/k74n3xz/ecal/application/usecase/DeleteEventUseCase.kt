package net.k74n3xz.ecal.application.usecase

import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.domain.repository.EventRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val reconciler: AlarmOccurrenceReconciler
) {
    suspend operator fun invoke(eventUid: String) {
        eventRepository.deleteEventByUid(eventUid)
        reconciler.reconcileAlarmOccurrences()
    }
}