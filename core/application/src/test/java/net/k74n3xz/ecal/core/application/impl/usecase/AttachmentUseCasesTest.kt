package net.k74n3xz.ecal.core.application.impl.usecase

import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.AddAttachmentCommand
import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.UpdateAttachmentCommand
import net.k74n3xz.ecal.core.application.testing.RecordingAttachmentCleanupScheduler
import net.k74n3xz.ecal.core.application.testing.RecordingAttachmentRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class AddAttachmentUseCaseTest {
    @Test
    fun commandIsMappedToRepositoryArguments() = runTest {
        val calls = mutableListOf<String>()
        val useCase = AddAttachmentUseCaseImpl(RecordingAttachmentRepository(calls))

        useCase(AddAttachmentCommand(description = null, platformToken = "content://attachment"))

        assertEquals(listOf("addAttachment:null:content://attachment"), calls)
    }
}

class UpdateAttachmentUseCaseTest {
    @Test
    fun commandIsMappedToRepositoryArguments() = runTest {
        val calls = mutableListOf<String>()
        val useCase = UpdateAttachmentUseCaseImpl(RecordingAttachmentRepository(calls))

        useCase(UpdateAttachmentCommand(id = 7, description = "updated"))

        assertEquals(listOf("updateAttachment:7:updated"), calls)
    }
}

class CleanupRemovingAttachmentUseCaseTest {
    @Test
    fun cleanupIsDelegatedToRepository() = runTest {
        val calls = mutableListOf<String>()

        CleanupRemovingAttachmentUseCaseImpl(RecordingAttachmentRepository(calls))()

        assertEquals(listOf("cleanupAttachments"), calls)
    }
}

class DeleteAttachmentUseCaseTest {
    @Test
    fun deleteCompletesBeforeCleanupIsScheduled() = runTest {
        val calls = mutableListOf<String>()
        val useCase = DeleteAttachmentUseCaseImpl(
            RecordingAttachmentRepository(calls),
            RecordingAttachmentCleanupScheduler(calls)
        )

        useCase(9)

        assertEquals(listOf("deleteAttachment:9", "scheduleAttachmentCleanup"), calls)
    }

    @Test
    fun repositoryFailurePreventsCleanupScheduling() = runTest {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("delete failed")
        val repository = RecordingAttachmentRepository(calls).apply { this.failure = failure }
        val useCase = DeleteAttachmentUseCaseImpl(repository, RecordingAttachmentCleanupScheduler(calls))

        val thrown = runCatching { useCase(9) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("deleteAttachment:9"), calls)
    }

    @Test
    fun schedulerFailureIsPropagatedAfterDelete() = runTest {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("schedule failed")
        val scheduler = RecordingAttachmentCleanupScheduler(calls, failure)
        val useCase = DeleteAttachmentUseCaseImpl(RecordingAttachmentRepository(calls), scheduler)

        val thrown = runCatching { useCase(9) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("deleteAttachment:9", "scheduleAttachmentCleanup"), calls)
    }
}
