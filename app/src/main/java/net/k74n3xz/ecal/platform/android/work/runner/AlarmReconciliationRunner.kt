package net.k74n3xz.ecal.platform.android.work.runner

import androidx.work.ListenableWorker
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import net.k74n3xz.ecal.core.application.port.inbound.usecase.ReconcileAlarmOccurrencesUseCase

@Singleton
internal class AlarmReconciliationRunner @Inject constructor(
    private val reconcileAlarmOccurrences: ReconcileAlarmOccurrencesUseCase
) {
    private companion object {
        private const val MAX_WORKER_RETRY: Int = 7
    }

    suspend fun run(runAttemptCount: Int) = try {
        reconcileAlarmOccurrences()
        ListenableWorker.Result.success()
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (_: Exception) {
        if (runAttemptCount < MAX_WORKER_RETRY) {
            ListenableWorker.Result.retry()
        } else {
            ListenableWorker.Result.failure()
        }
    }
}
