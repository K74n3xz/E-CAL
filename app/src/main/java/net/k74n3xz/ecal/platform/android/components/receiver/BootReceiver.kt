package net.k74n3xz.ecal.platform.android.components.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.outbound.repository.AlarmRepository

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmReconciler: AlarmReconciler

    private val receiverScope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                alarmRepository.markAllAlarmOccurrencesAsCancelled()
                alarmReconciler.request()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
