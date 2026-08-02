package net.k74n3xz.ecal.core.application.impl.usecase

import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.testing.RecordingAttendeeRepository
import net.k74n3xz.ecal.core.application.testing.RecordingReconciler
import net.k74n3xz.ecal.core.model.Attendee
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveAttendeeUseCaseTest {
    @Test
    fun attendeeIsForwardedToRepository() = runTest {
        val calls = mutableListOf<String>()
        val attendee = Attendee(id = 3, name = "Ada", email = "ada@example.com")

        SaveAttendeeUseCaseImpl(RecordingAttendeeRepository(calls))(attendee)

        assertEquals(listOf("saveAttendee:ada@example.com"), calls)
    }
}

class DeleteAttendeeUseCaseTest {
    @Test
    fun attendeeIdIsForwardedToRepository_thenRequestsReconciliation() = runTest {
        val calls = mutableListOf<String>()
        val repository = RecordingAttendeeRepository(calls)
        val reconciler = RecordingReconciler(calls)

        DeleteAttendeeUseCaseImpl(repository, reconciler)(3)

        assertEquals(listOf("deleteAttendee:3", "reconcile"), calls)
    }

    @Test
    fun repositoryFailure_doesNotRequestReconciliation() = runTest {
        val calls = mutableListOf<String>()
        val repository = RecordingAttendeeRepository(calls).apply {
            failure = IllegalStateException("delete failed")
        }
        val reconciler = RecordingReconciler(calls)

        runCatching { DeleteAttendeeUseCaseImpl(repository, reconciler)(3) }

        assertEquals(listOf("deleteAttendee:3"), calls)
        assertEquals(0, reconciler.callCount)
    }
}
