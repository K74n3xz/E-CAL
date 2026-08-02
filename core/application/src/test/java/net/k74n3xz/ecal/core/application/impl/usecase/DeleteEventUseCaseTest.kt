package net.k74n3xz.ecal.core.application.impl.usecase

import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.testing.RecordingEventRepository
import net.k74n3xz.ecal.core.application.testing.RecordingReconciler
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteEventUseCaseTest {
    private class Fixture {
        val calls = mutableListOf<String>()
        val repository = RecordingEventRepository(calls)
        val reconciler = RecordingReconciler(calls)
        val useCase = DeleteEventUseCaseImpl(repository, reconciler)
    }

    @Test
    fun deleteEvent_thenRequestsReconciliation() = runTest {
        val fixture = Fixture()

        fixture.useCase("event-1")

        assertEquals(listOf("event-1"), fixture.repository.deletedUids)
        assertEquals(listOf("delete:event-1", "reconcile"), fixture.calls)
        assertEquals(1, fixture.reconciler.callCount)
    }

    @Test
    fun repositoryFailure_isPropagatedAndDoesNotRequestReconciliation() = runTest {
        val failure = IllegalStateException("delete failed")
        val fixture = Fixture()
        fixture.repository.deleteFailure = failure

        val thrown = runCatching { fixture.useCase("event-2") }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("delete:event-2"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun reconciliationFailure_isPropagatedAfterDelete() = runTest {
        val failure = IllegalStateException("reconcile failed")
        val fixture = Fixture()
        fixture.reconciler.failure = failure

        val thrown = runCatching { fixture.useCase("event-3") }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("event-3"), fixture.repository.deletedUids)
        assertEquals(listOf("delete:event-3", "reconcile"), fixture.calls)
    }
}
