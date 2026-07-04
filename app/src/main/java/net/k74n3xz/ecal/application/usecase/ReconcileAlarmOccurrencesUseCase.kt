package net.k74n3xz.ecal.application.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.application.port.AlarmScheduler
import net.k74n3xz.ecal.domain.repository.AlarmRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconcileAlarmOccurrencesUseCase @Inject constructor(
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