package net.k74n3xz.ecal.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.CancellationException
import net.k74n3xz.ecal.core.application.usecase.ReconcileAlarmOccurrencesUseCase

@HiltWorker
class AlarmReconciliationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reconcileAlarmOccurrences: ReconcileAlarmOccurrencesUseCase
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        reconcileAlarmOccurrences()
        Result.success()
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (_: Exception) {
        retryOrFail(runAttemptCount)
    }
}
