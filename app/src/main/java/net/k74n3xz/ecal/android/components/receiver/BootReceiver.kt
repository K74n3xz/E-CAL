package net.k74n3xz.ecal.android.components.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmOccurrenceReconciler: AlarmOccurrenceReconciler

    private val receiverScope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                alarmRepository.markAllAlarmOccurrencesAsCancelled()
                alarmOccurrenceReconciler.request()
            } finally {
                pendingResult.finish()
            }
        }
    }
}