package net.k74n3xz.ecal.platform.android.work.runner

import androidx.work.ListenableWorker
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.inbound.usecase.CleanupRemovingAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.ReconcileAlarmOccurrencesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AlarmReconciliationRunnerTest {
    @Test
    fun successfulUseCase_returnsSuccess() = runTest {
        var calls = 0
        val runner = AlarmReconciliationRunner(object : ReconcileAlarmOccurrencesUseCase {
            override suspend fun invoke() {
                calls++
            }
        })

        val result = runner.run(runAttemptCount = 0)

        assertEquals(1, calls)
        assertResultEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun failure_retriesBeforeLimitAndFailsAtLimit() = runTest {
        val failure = IllegalStateException("reconcile failed")
        val runner = AlarmReconciliationRunner(failingReconciler(failure))

        assertResultEquals(ListenableWorker.Result.retry(), runner.run(runAttemptCount = 6))
        assertResultEquals(ListenableWorker.Result.failure(), runner.run(runAttemptCount = 7))
    }

    @Test
    fun cancellation_isRethrown() = runTest {
        val cancellation = CancellationException("cancelled")
        val runner = AlarmReconciliationRunner(failingReconciler(cancellation))

        assertSame(cancellation, runCatching { runner.run(0) }.exceptionOrNull())
    }
}

class AttachmentCleanupRunnerTest {
    @Test
    fun successfulUseCase_returnsSuccess() = runTest {
        var calls = 0
        val runner = AttachmentCleanupRunner(object : CleanupRemovingAttachmentUseCase {
            override suspend fun invoke() {
                calls++
            }
        })

        val result = runner.run(runAttemptCount = 0)

        assertEquals(1, calls)
        assertResultEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun failure_retriesBeforeLimitAndFailsAtLimit() = runTest {
        val failure = IllegalStateException("cleanup failed")
        val runner = AttachmentCleanupRunner(failingCleanup(failure))

        assertResultEquals(ListenableWorker.Result.retry(), runner.run(runAttemptCount = 6))
        assertResultEquals(ListenableWorker.Result.failure(), runner.run(runAttemptCount = 7))
    }

    @Test
    fun cancellation_isRethrown() = runTest {
        val cancellation = CancellationException("cancelled")
        val runner = AttachmentCleanupRunner(failingCleanup(cancellation))

        assertSame(cancellation, runCatching { runner.run(0) }.exceptionOrNull())
    }
}

class DueAlarmHandlingRunnerTest {
    @Test
    fun successfulUseCase_receivesNowAndReturnsSuccess() = runTest {
        val received = mutableListOf<Instant>()
        val runner = DueAlarmHandlingRunner(object : HandleDueAlarmsUseCase {
            override suspend fun invoke(now: Instant) {
                received += now
            }
        })
        val now = Instant.parse("2026-07-20T03:04:05Z")

        val result = runner.run(now, runAttemptCount = 0)

        assertEquals(listOf(now), received)
        assertResultEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun failure_retriesBeforeLimitAndFailsAtLimit() = runTest {
        val failure = IllegalStateException("handling failed")
        val runner = DueAlarmHandlingRunner(failingHandler(failure))
        val now = Instant.EPOCH

        assertResultEquals(ListenableWorker.Result.retry(), runner.run(now, runAttemptCount = 6))
        assertResultEquals(ListenableWorker.Result.failure(), runner.run(now, runAttemptCount = 7))
    }

    @Test
    fun cancellation_isRethrown() = runTest {
        val cancellation = CancellationException("cancelled")
        val runner = DueAlarmHandlingRunner(failingHandler(cancellation))

        assertSame(cancellation, runCatching { runner.run(Instant.EPOCH, 0) }.exceptionOrNull())
    }
}

private fun failingReconciler(failure: Exception) = object : ReconcileAlarmOccurrencesUseCase {
    override suspend fun invoke(): Unit = throw failure
}

private fun failingCleanup(failure: Exception) = object : CleanupRemovingAttachmentUseCase {
    override suspend fun invoke(): Unit = throw failure
}

private fun failingHandler(failure: Exception) = object : HandleDueAlarmsUseCase {
    override suspend fun invoke(now: Instant): Unit = throw failure
}

private fun assertResultEquals(expected: ListenableWorker.Result, actual: ListenableWorker.Result) {
    assertEquals(expected.toString(), actual.toString())
}
