package net.k74n3xz.ecal.core.application.impl.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.core.application.port.inbound.usecase.ReconcileAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.outbound.repository.AlarmRepository

internal class ReconcileAlarmOccurrencesUseCaseImpl(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ReconcileAlarmOccurrencesUseCase {
    private val mutex: Mutex = Mutex()

    override suspend operator fun invoke() = mutex.withLock {
        val alarmOccurrences = alarmRepository.getAlarmOccurrenceNeedingReconciliation()
        alarmOccurrences.alarmOccurrenceNeedingCancellation.forEach {
            alarmRepository.markAlarmOccurrenceAsUnknown(it.id)
            alarmScheduler.cancel(it.id)
            alarmRepository.markAlarmOccurrenceAsCancelled(it.id)
        }
        alarmOccurrences.alarmOccurrenceNeedingScheduling.forEach {
            alarmRepository.markAlarmOccurrenceAsUnknown(it.id)
            alarmScheduler.schedule(it.id, it.triggerAt)
            alarmRepository.markAlarmOccurrenceAsScheduled(it.id)
        }
    }
}
