package net.k74n3xz.ecal.android.work.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.android.work.DueAlarmHandlingWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DueAlarmHandlingScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    private companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME: String = "due_alarm_handler_periodic"
        private const val UNIQUE_ONE_TIME_WORK_NAME: String = "due_alarm_handler_one_time"
    }

    fun schedule() {
        ensurePeriodicWork()
        enqueueOneTimeWork()
    }

    private fun ensurePeriodicWork() {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DueAlarmHandlingWorker>(
                    PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS
                ).build()
            )
    }

    private fun enqueueOneTimeWork() {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(DueAlarmHandlingWorker::class.java)
            )
    }
}