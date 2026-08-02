package net.k74n3xz.ecal.platform.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import net.k74n3xz.ecal.platform.android.work.runner.DueAlarmHandlingRunner

@HiltWorker
internal class DueAlarmHandlingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dueAlarmHandlingRunner: DueAlarmHandlingRunner
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = dueAlarmHandlingRunner.run(Instant.now(), runAttemptCount)
}
