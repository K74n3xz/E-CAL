package net.k74n3xz.ecal.core.application.impl.usecase

import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.testing.RecordingAlarmRepository
import net.k74n3xz.ecal.core.application.testing.RecordingReconciler
import org.junit.Assert.assertEquals
import org.junit.Test

class RebuildAllDayAlarmOccurrencesUseCaseTest {
    @Test
    fun rebuildForwardsChangedTimeZoneThenRequestsReconciliation() = runTest {
        val calls = mutableListOf<String>()
        val repository = RecordingAlarmRepository(calls)
        val reconciler = RecordingReconciler(calls)
        val useCase = RebuildAllDayAlarmOccurrencesUseCaseImpl(repository, reconciler)
        val timeZone = ZoneId.of("America/Los_Angeles")

        useCase(timeZone)

        assertEquals(listOf(timeZone), repository.rebuiltTimeZones)
        assertEquals(listOf("rebuild:$timeZone", "reconcile"), calls)
    }

    @Test
    fun repositoryFailurePreventsReconciliation() = runTest {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("rebuild failed")
        val repository = RecordingAlarmRepository(calls, rebuildFailure = failure)
        val reconciler = RecordingReconciler(calls)
        val useCase = RebuildAllDayAlarmOccurrencesUseCaseImpl(repository, reconciler)
        val timeZone = ZoneId.of("Asia/Hong_Kong")

        val thrown = runCatching { useCase(timeZone) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("rebuild:$timeZone"), calls)
        assertEquals(0, reconciler.callCount)
    }

    @Test
    fun reconciliationFailureIsPropagatedAfterRebuild() = runTest {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("reconcile failed")
        val repository = RecordingAlarmRepository(calls)
        val reconciler = RecordingReconciler(calls, failure)
        val useCase = RebuildAllDayAlarmOccurrencesUseCaseImpl(repository, reconciler)
        val timeZone = ZoneId.of("Asia/Hong_Kong")

        val thrown = runCatching { useCase(timeZone) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("rebuild:$timeZone", "reconcile"), calls)
        assertEquals(listOf(timeZone), repository.rebuiltTimeZones)
    }
}
