package net.k74n3xz.ecal.android.components.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.android.helper.alarm.AlarmHelper
import net.k74n3xz.ecal.android.helper.notification.ForegroundServiceNotificationHelper
import net.k74n3xz.ecal.domain.repository.AlarmRepository
import javax.inject.Inject
import net.k74n3xz.ecal.android.constant.Notification as NotificationConstant

@AndroidEntryPoint
class AlarmReconciliationService : Service() {
    companion object {
        private const val FOREGROUND_SERVICE_TYPE_NONE_COMPAT: Int = 0
    }

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmHelper: AlarmHelper

    @Inject
    lateinit var foregroundServiceNotificationHelper: ForegroundServiceNotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reconciliationMutex: Mutex = Mutex()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            /* service = */
            this,
            /* id = */
            NotificationConstant.Id.FOREGROUND_SERVICE_NOTIFICATION_ID,
            /* notification = */
            foregroundServiceNotificationHelper.buildNotification(
                title = getString(R.string.notification_title_reconciling_alarms),
                text = getString(R.string.notification_text_reconciling_alarms)
            ),
            /* foregroundServiceType = */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            else FOREGROUND_SERVICE_TYPE_NONE_COMPAT
        )
        serviceScope.launch {
            reconciliationMutex.withLock {
                reconcileAlarmOccurrences()
            }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private suspend fun reconcileAlarmOccurrences() {
        val (alarmsToCancel, alarmsToSchedule) = alarmRepository.getAlarmOccurrenceNeedingReconciliation()
        alarmsToCancel.forEach {
            alarmHelper.cancel(it.id)
            alarmRepository.markAlarmOccurrenceAsCancelled(it.id)
        }
        alarmsToSchedule.forEach {
            alarmHelper.schedule(it.id, it.triggerAt.toEpochMilli())
            alarmRepository.markAlarmOccurrenceAsScheduled(it.id)
        }
    }
}