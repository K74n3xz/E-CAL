package net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.`in`.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveAttendeeUseCase
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.testutils.MainDispatcherRule
import net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel.state.AttendeeOperationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AttendeeManagementViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val attendeeFlow = MutableStateFlow<List<Attendee>>(emptyList())
    private val saveUseCase = RecordingSaveAttendeeUseCase()
    private val deleteUseCase = RecordingDeleteAttendeeUseCase()

    @Test
    fun attendeeEmissions_arePublished() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.attendees.collect {} }
        val attendee = attendee()

        attendeeFlow.value = listOf(attendee)
        runCurrent()

        assertEquals(listOf(attendee), viewModel.attendees.value)
    }

    @Test
    fun save_forwardsAttendeeAndBlocksConcurrentOperations() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        saveUseCase.gate = CompletableDeferred()

        viewModel.saveAttendee(attendee())
        viewModel.deleteAttendee(7)

        saveUseCase.started.await()
        assertEquals(AttendeeOperationState.Saving, viewModel.uiState.value.operationState)
        assertEquals(listOf(attendee()), saveUseCase.attendees)
        assertTrue(deleteUseCase.ids.isEmpty())

        saveUseCase.gate.complete(Unit)
        assertEquals(AttendeeOperationState.Idle(null), viewModel.awaitIdle())
    }

    @Test
    fun delete_forwardsId() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.deleteAttendee(7)
        runCurrent()

        assertEquals(listOf(7L), deleteUseCase.ids)
    }

    @Test
    fun failureIsExposedAndSuccessfulRetryClearsIt() = runTest(mainDispatcherRule.dispatcher) {
        val failure = IllegalStateException("save failed")
        saveUseCase.failure = failure
        val viewModel = createViewModel()

        viewModel.saveAttendee(attendee())
        assertEquals(failure, viewModel.awaitIdle().failure)

        saveUseCase.failure = null
        saveUseCase.resetCompletionSignal()
        viewModel.saveAttendee(attendee())
        assertEquals(AttendeeOperationState.Idle(null), viewModel.awaitIdle())
    }

    @Test
    fun exitIsBlockedWhileBusyAndConsumedOnce() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        saveUseCase.gate = CompletableDeferred()
        viewModel.saveAttendee(attendee())

        viewModel.requestExit()
        assertEquals(AttendeeOperationState.Saving, viewModel.uiState.value.operationState)

        saveUseCase.started.await()
        saveUseCase.gate.complete(Unit)
        viewModel.awaitIdle()
        viewModel.requestExit()

        assertTrue(viewModel.consumeExitState())
        assertEquals(AttendeeOperationState.Exited, viewModel.uiState.value.operationState)
        assertFalse(viewModel.consumeExitState())
    }

    private fun createViewModel() = AttendeeManagementViewModel(
        attendeeQueryService = FakeAttendeeQueryService(attendeeFlow),
        saveAttendeeUseCase = saveUseCase,
        deleteAttendeeUseCase = deleteUseCase
    )

    private suspend fun AttendeeManagementViewModel.awaitIdle(): AttendeeOperationState.Idle =
        uiState.first { it.operationState is AttendeeOperationState.Idle }
            .operationState as AttendeeOperationState.Idle
}

private fun attendee() = Attendee(7, "Ada", "Organizer", "ada@example.com")

private class FakeAttendeeQueryService(private val attendeeFlow: Flow<List<Attendee>>) : AttendeeQueryService {
    override suspend fun findAttendeeById(attendeeId: Long): Attendee? = null
    override fun observeAllAttendees(): Flow<List<Attendee>> = attendeeFlow
}

private class RecordingSaveAttendeeUseCase : SaveAttendeeUseCase {
    val attendees = mutableListOf<Attendee>()
    var gate = CompletableDeferred(Unit)
    var failure: Exception? = null
    var started = CompletableDeferred<Unit>()
        private set

    override suspend fun invoke(attendee: Attendee) {
        attendees += attendee
        started.complete(Unit)
        gate.await()
        failure?.let { throw it }
    }

    fun resetCompletionSignal() {
        started = CompletableDeferred()
    }
}

private class RecordingDeleteAttendeeUseCase : DeleteAttendeeUseCase {
    val ids = mutableListOf<Long>()
    override suspend fun invoke(attendeeId: Long) {
        ids += attendeeId
    }
}
