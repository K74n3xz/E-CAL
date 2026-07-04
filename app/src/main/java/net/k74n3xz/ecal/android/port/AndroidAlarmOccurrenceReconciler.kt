package net.k74n3xz.ecal.android.port

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.android.components.service.AlarmReconciliationService
import net.k74n3xz.ecal.android.work.AlarmReconciliationWorker
import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmOccurrenceReconciler @Inject constructor(@param:ApplicationContext private val context: Context) :
    AlarmOccurrenceReconciler {
    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME: String = "alarm_reconciliation_periodic"
        const val UNIQUE_ONE_TIME_WORK_NAME: String = "alarm_reconciliation_one_time"
    }

    override fun request() {
        ensurePeriodicWork()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startForegroundService()
            } catch (_: ForegroundServiceStartNotAllowedException) {
                enqueueOneTimeWork()
            }
        } else {
            startForegroundService()
        }
    }

    private fun ensurePeriodicWork() {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AlarmReconciliationWorker>(
                    PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS
                ).build()
            )
    }

    private fun startForegroundService() {
        val intent = Intent(context.applicationContext, AlarmReconciliationService::class.java)
        context.applicationContext.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }
    }

    private fun enqueueOneTimeWork() {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(AlarmReconciliationWorker::class.java)
            )
    }
}