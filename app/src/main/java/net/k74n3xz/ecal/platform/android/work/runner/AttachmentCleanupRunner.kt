package net.k74n3xz.ecal.platform.android.work.runner

import androidx.work.ListenableWorker
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import net.k74n3xz.ecal.core.application.port.inbound.usecase.CleanupRemovingAttachmentUseCase

@Singleton
internal class AttachmentCleanupRunner @Inject constructor(
    private val cleanupRemovingAttachmentUseCase: CleanupRemovingAttachmentUseCase
) {
    private companion object {
        private const val MAX_WORKER_RETRY: Int = 7
    }

    suspend fun run(runAttemptCount: Int) = try {
        cleanupRemovingAttachmentUseCase()
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
