package net.k74n3xz.ecal.core.application.impl.usecase

import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.testing.ApplicationUseCaseTestData
import net.k74n3xz.ecal.core.application.testing.RecordingEventRepository
import net.k74n3xz.ecal.core.application.testing.RecordingReconciler
import net.k74n3xz.ecal.core.application.testing.RecordingTimeZoneProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveEventUseCaseTest {
    private class Fixture {
        val calls = mutableListOf<String>()
        val repository = RecordingEventRepository(calls)
        val reconciler = RecordingReconciler(calls)
        val timeZone: ZoneId = ZoneId.of("Asia/Hong_Kong")
        val timeZoneProvider = RecordingTimeZoneProvider(timeZone)
        val useCase = SaveEventUseCaseImpl(repository, reconciler, timeZoneProvider)
    }

    @Test
    fun saveEvent_thenRequestsReconciliation() = runTest {
        val fixture = Fixture()
        val event = ApplicationUseCaseTestData.event("event-1")

        fixture.useCase(event)

        assertEquals(listOf(event), fixture.repository.savedEvents)
        assertEquals(listOf(fixture.timeZone), fixture.repository.savedTimeZones)
        assertEquals(1, fixture.timeZoneProvider.currentReadCount)
        assertEquals(listOf("save:event-1", "reconcile"), fixture.calls)
        assertEquals(1, fixture.reconciler.callCount)
    }

    @Test
    fun repositoryFailure_isPropagatedAndDoesNotRequestReconciliation() = runTest {
        val failure = IllegalStateException("save failed")
        val fixture = Fixture()
        fixture.repository.saveFailure = failure

        val thrown = runCatching { fixture.useCase(ApplicationUseCaseTestData.event("event-2")) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("save:event-2"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun reconciliationFailure_isPropagatedAfterSave() = runTest {
        val failure = IllegalStateException("reconcile failed")
        val fixture = Fixture()
        val event = ApplicationUseCaseTestData.event("event-3")
        fixture.reconciler.failure = failure

        val thrown = runCatching { fixture.useCase(event) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf(event), fixture.repository.savedEvents)
        assertEquals(listOf("save:event-3", "reconcile"), fixture.calls)
    }
}
