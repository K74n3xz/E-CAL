package net.k74n3xz.ecal.android.components.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.android.components.service.AlarmReconciliationService
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var alarmRepository: AlarmRepository

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()

        receiverScope.launch {
            alarmRepository.markAllAlarmsAsCancelled()

            val startServiceIntent =
                Intent(context.applicationContext, AlarmReconciliationService::class.java)
            context.applicationContext.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.startForegroundService(startServiceIntent)
                } else {
                    it.startService(startServiceIntent)
                }
            }

            pendingResult.finish()
        }
    }
}