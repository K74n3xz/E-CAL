package net.k74n3xz.ecal.platform.android.work.runner

import androidx.work.ListenableWorker
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import net.k74n3xz.ecal.core.application.port.inbound.usecase.HandleDueAlarmsUseCase

@Singleton
internal class DueAlarmHandlingRunner @Inject constructor(private val handleDueAlarms: HandleDueAlarmsUseCase) {
    private companion object {
        private const val MAX_WORKER_RETRY: Int = 7
    }

    suspend fun run(now: Instant, runAttemptCount: Int) = try {
        handleDueAlarms(now)
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
