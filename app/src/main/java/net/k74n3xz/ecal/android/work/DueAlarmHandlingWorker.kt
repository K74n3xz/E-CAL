package net.k74n3xz.ecal.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import net.k74n3xz.ecal.application.usecase.HandleDueAlarmsUseCase
import java.time.Instant
import java.util.concurrent.CancellationException

@HiltWorker
class DueAlarmHandlingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val handleDueAlarms: HandleDueAlarmsUseCase
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        try {
            handleDueAlarms(Instant.now())
            Result.success()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            retryOrFail(runAttemptCount)
        }
}