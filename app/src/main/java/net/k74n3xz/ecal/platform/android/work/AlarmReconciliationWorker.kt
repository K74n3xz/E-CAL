package net.k74n3xz.ecal.platform.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.k74n3xz.ecal.platform.android.work.runner.AlarmReconciliationRunner

@HiltWorker
internal class AlarmReconciliationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val alarmReconciliationRunner: AlarmReconciliationRunner
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = alarmReconciliationRunner.run(runAttemptCount)
}
