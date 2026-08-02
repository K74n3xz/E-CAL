package net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel

import java.io.FileInputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.inbound.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.inbound.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.UpdateAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.AddAttachmentCommand
import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.UpdateAttachmentCommand
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.testutils.MainDispatcherRule
import net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel.state.AttachmentOperationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentManagementViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val attachments = MutableStateFlow<List<Attachment>>(emptyList())
    private val queryService = FakeAttachmentQueryService(attachments)
    private val addUseCase = RecordingAddAttachmentUseCase()
    private val updateUseCase = RecordingUpdateAttachmentUseCase()
    private val deleteUseCase = RecordingDeleteAttachmentUseCase()

    @Test
    fun repositoryEmissions_arePublishedThroughTheAttachmentsStateFlow() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.attachments.collect {}
        }

        runCurrent()
        val attachment = attachment(id = 1)
        attachments.value = listOf(attachment)
        runCurrent()

        assertEquals(listOf(attachment), viewModel.attachments.value)
        assertEquals(AttachmentOperationState.Idle(null), viewModel.uiState.value.operationState)
    }

    @Test
    fun add_forwardsDescriptionUnchangedAndBlocksConcurrentOperations() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        addUseCase.gate = CompletableDeferred()

        viewModel.addAttachment("   ", "content://document/1")
        viewModel.updateAttachmentDescription(7, "ignored")

        assertEquals(AttachmentOperationState.Adding, viewModel.uiState.value.operationState)
        addUseCase.started.await()
        assertEquals(
            listOf(AddAttachmentCommand("   ", "content://document/1")),
            addUseCase.commands
        )
        assertTrue(updateUseCase.commands.isEmpty())

        addUseCase.gate.complete(Unit)
        assertEquals(AttachmentOperationState.Idle(null), viewModel.awaitIdle())
    }

    @Test
    fun updateAndDelete_forwardIdsAndDescriptions() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.updateAttachmentDescription(3, " ")
        runCurrent()
        viewModel.deleteAttachment(4)
        runCurrent()

        assertEquals(listOf(UpdateAttachmentCommand(3, " ")), updateUseCase.commands)
        assertEquals(listOf(4L), deleteUseCase.ids)
    }

    @Test
    fun operationFailure_isStoredOnIdleAndClearedBySuccessfulRetry() = runTest(mainDispatcherRule.dispatcher) {
        val failure = IllegalStateException("import failed")
        addUseCase.failure = failure
        val viewModel = createViewModel()

        viewModel.addAttachment("Agenda", "content://document/1")

        val failedState = viewModel.awaitIdle()
        assertEquals(failure::class, failedState.failure?.let { it::class })
        assertEquals(failure.message, failedState.failure?.message)

        addUseCase.failure = null
        addUseCase.resetCompletionSignals()
        viewModel.addAttachment("Agenda", "content://document/2")
        val retriedState = viewModel.awaitIdle()

        assertEquals(2, addUseCase.commands.size)
        assertEquals(
            AddAttachmentCommand("Agenda", "content://document/2"),
            addUseCase.commands.last()
        )
        assertEquals(AttachmentOperationState.Idle(null), retriedState)
    }

    @Test
    fun exit_isBlockedWhileBusyAndConsumedExactlyOnce() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        addUseCase.gate = CompletableDeferred()
        viewModel.addAttachment(null, "content://document/1")

        viewModel.requestExit()
        assertEquals(AttachmentOperationState.Adding, viewModel.uiState.value.operationState)

        addUseCase.started.await()
        addUseCase.gate.complete(Unit)
        viewModel.awaitIdle()
        viewModel.requestExit()

        assertEquals(AttachmentOperationState.Exit, viewModel.uiState.value.operationState)
        assertTrue(viewModel.consumeExitState())
        assertEquals(AttachmentOperationState.Exited, viewModel.uiState.value.operationState)
        assertFalse(viewModel.consumeExitState())
    }

    private fun createViewModel() = AttachmentManagementViewModel(
        attachmentQueryService = queryService,
        addAttachmentUseCase = addUseCase,
        updateAttachmentUseCase = updateUseCase,
        deleteAttachmentUseCase = deleteUseCase
    )

    private suspend fun AttachmentManagementViewModel.awaitIdle(): AttachmentOperationState.Idle =
        uiState.first { it.operationState is AttachmentOperationState.Idle }
            .operationState as AttachmentOperationState.Idle
}

private fun attachment(id: Long) = Attachment(
    id = id,
    description = "Agenda",
    name = "agenda.pdf",
    mimeType = "application/pdf",
    sizeBytes = 2048
)

private class FakeAttachmentQueryService(private val attachmentFlow: Flow<List<Attachment>>) : AttachmentQueryService {
    override suspend fun findAttachmentById(attachmentId: Long): Attachment? = null
    override fun observeAvailableAttachments(): Flow<List<Attachment>> = attachmentFlow
    override fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>> =
        attachmentFlow

    override suspend fun openAttachmentById(attachmentId: Long): FileInputStream? = null
}

private class RecordingAddAttachmentUseCase : AddAttachmentUseCase {
    val commands = mutableListOf<AddAttachmentCommand>()
    var gate = CompletableDeferred(Unit)
    var failure: Exception? = null
    var started = CompletableDeferred<Unit>()
        private set

    override suspend fun invoke(addAttachmentCommand: AddAttachmentCommand) {
        commands += addAttachmentCommand
        started.complete(Unit)
        gate.await()
        failure?.let { throw it }
    }

    fun resetCompletionSignals() {
        started = CompletableDeferred()
    }
}

private class RecordingUpdateAttachmentUseCase : UpdateAttachmentUseCase {
    val commands = mutableListOf<UpdateAttachmentCommand>()

    override suspend fun invoke(updateAttachmentCommand: UpdateAttachmentCommand) {
        commands += updateAttachmentCommand
    }
}

private class RecordingDeleteAttachmentUseCase : DeleteAttachmentUseCase {
    val ids = mutableListOf<Long>()

    override suspend fun invoke(attachmentId: Long) {
        ids += attachmentId
    }
}
