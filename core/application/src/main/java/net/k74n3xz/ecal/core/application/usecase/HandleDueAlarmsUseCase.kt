package net.k74n3xz.ecal.core.application.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.port.NotificationPublisher
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.model.Alarm
import java.time.Instant

class HandleDueAlarmsUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmOccurrenceReconciler: AlarmOccurrenceReconciler,
    private val notificationPublisher: NotificationPublisher
) {
    private val mutex: Mutex = Mutex()

    suspend operator fun invoke(triggerAt: Instant) = mutex.withLock {
        val dueAlarmOccurrences = alarmRepository.getDueAlarmOccurrenceIdsAndActions(triggerAt)

        dueAlarmOccurrences.forEach { (ids, action) ->
            ids.forEach {
                when (action) {
                    is Alarm.Action.Display -> {
                        notificationPublisher.publish(it, action.description)
                    }

                    else -> Unit
                }
                alarmRepository.processDueAlarmOccurrence(it)
            }
        }
        if (dueAlarmOccurrences.any { it.first.isNotEmpty() }) {
            alarmOccurrenceReconciler.request()
        }
    }
}