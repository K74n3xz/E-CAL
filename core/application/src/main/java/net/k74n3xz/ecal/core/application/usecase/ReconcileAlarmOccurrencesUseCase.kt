package net.k74n3xz.ecal.core.application.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.core.application.port.AlarmScheduler
import net.k74n3xz.ecal.core.application.repository.AlarmRepository

class ReconcileAlarmOccurrencesUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    private val mutex: Mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        val (alarmsToCancel, alarmsToSchedule) = alarmRepository.getAlarmOccurrenceNeedingReconciliation()
        alarmsToCancel.forEach {
            alarmRepository.markAlarmOccurrenceAsUnknown(it.id)
            alarmScheduler.cancel(it.id)
            alarmRepository.markAlarmOccurrenceAsCancelled(it.id)
        }
        alarmsToSchedule.forEach {
            alarmRepository.markAlarmOccurrenceAsUnknown(it.id)
            alarmScheduler.schedule(it.id, it.triggerAt)
            alarmRepository.markAlarmOccurrenceAsScheduled(it.id)
        }
    }
}