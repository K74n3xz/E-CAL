package net.k74n3xz.ecal.platform.android.components

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.platform.android.work.scheduler.DueAlarmHandlingScheduler

@HiltAndroidApp
internal class ECalApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var alarmReconciler: AlarmReconciler

    @Inject
    lateinit var dueAlarmHandlingScheduler: DueAlarmHandlingScheduler

    override fun onCreate() {
        super.onCreate()
        // TODO: Observe time zone changes when time zone switching is supported.
        alarmReconciler.request()
        dueAlarmHandlingScheduler.schedule()
    }
}
