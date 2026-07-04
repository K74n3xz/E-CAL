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
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.android.helper.notification.ForegroundServiceNotificationHelper
import net.k74n3xz.ecal.application.usecase.ReconcileAlarmOccurrencesUseCase
import javax.inject.Inject
import net.k74n3xz.ecal.android.constant.Notification as NotificationConstant

@AndroidEntryPoint
class AlarmReconciliationService : Service() {
    private companion object {
        private const val FOREGROUND_SERVICE_TYPE_NONE_COMPAT: Int = 0
    }

    @Inject
    lateinit var foregroundServiceNotificationHelper: ForegroundServiceNotificationHelper

    @Inject
    lateinit var reconcileAlarmOccurrencesUseCase: ReconcileAlarmOccurrencesUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            try {
                reconcileAlarmOccurrencesUseCase()
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null
}