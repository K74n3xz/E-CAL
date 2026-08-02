package net.k74n3xz.ecal.platform.android.components.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.core.application.port.`in`.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.platform.android.work.scheduler.DueAlarmHandlingScheduler

@AndroidEntryPoint
internal class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var handleDueAlarms: HandleDueAlarmsUseCase

    @Inject
    lateinit var dueAlarmHandlingScheduler: DueAlarmHandlingScheduler

    private val receiverScope = CoroutineScope(Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                handleDueAlarms(Instant.now())
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                dueAlarmHandlingScheduler.schedule()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
