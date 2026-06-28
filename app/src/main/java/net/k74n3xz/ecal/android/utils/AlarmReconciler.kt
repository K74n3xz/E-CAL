package net.k74n3xz.ecal.android.utils

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.android.helper.alarm.AlarmHelper
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReconciler @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmHelper: AlarmHelper
) {
    companion object {
        private const val TAG: String = "AlarmReconciler"
    }

    private val reconciliationMutex: Mutex = Mutex()

    suspend fun reconcileAlarmInstances() {
        reconciliationMutex.withLock {
            val (alarmsToCancel, alarmsToSchedule) = alarmRepository.getAlarmInstancesNeedingReconciliation()
            alarmsToCancel.forEach {
                if (it.id == null) {
                    Log.w(
                        TAG,
                        "reconcileAlarmInstances: The `id` of AlarmInstance to cancel is null, ignored."
                    )
                } else {
                    alarmHelper.cancel(it.id)
                    alarmRepository.markAlarmAsCancelled(it.id)
                }
            }
            alarmsToSchedule.forEach {
                if (it.id == null) {
                    Log.w(
                        TAG,
                        "reconcileAlarmInstances: The `id` of AlarmInstance to schedule is null, ignored."
                    )
                } else {
                    alarmHelper.schedule(it.id, it.triggerAt.toEpochMilli())
                    alarmRepository.markAlarmAsScheduled(it.id)
                }
            }
        }
    }
}