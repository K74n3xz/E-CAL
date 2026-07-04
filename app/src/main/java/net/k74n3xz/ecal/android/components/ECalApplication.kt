package net.k74n3xz.ecal.android.components

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import net.k74n3xz.ecal.android.work.scheduler.DueAlarmHandlingScheduler
import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import javax.inject.Inject

@HiltAndroidApp
class ECalApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var alarmOccurrenceReconciler: AlarmOccurrenceReconciler

    @Inject
    lateinit var dueAlarmHandlingScheduler: DueAlarmHandlingScheduler

    override fun onCreate() {
        super.onCreate()
        alarmOccurrenceReconciler.request()
        dueAlarmHandlingScheduler.schedule()
    }
}