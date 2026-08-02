package net.k74n3xz.ecal.platform.port

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.core.application.port.outbound.platform.AttachmentCleanupScheduler
import net.k74n3xz.ecal.platform.android.work.AttachmentCleanupWorker

@Singleton
internal class AndroidAttachmentCleanupScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AttachmentCleanupScheduler {
    private companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME: String = "attachment_cleanup_periodic"
        private const val UNIQUE_ONE_TIME_WORK_NAME: String = "attachment_cleanup_one_time"
    }

    override fun request() {
        ensurePeriodicWork()
        enqueueOneTimeWork()
    }

    private fun ensurePeriodicWork() {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AttachmentCleanupWorker>(
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
                OneTimeWorkRequest.from(AttachmentCleanupWorker::class.java)
            )
    }
}
