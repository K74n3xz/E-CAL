package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.application.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.application.usecase.SaveEventUseCase
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.domain.repository.EventRepository
import net.k74n3xz.ecal.testutils.MainDispatcherRule
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditMode
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditOperationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

@OptIn(ExperimentalCoroutinesApi::class)
class EventEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeEventRepository
    private lateinit var reconciler: FakeReconciler
    private lateinit var viewModel: EventEditViewModel

    @Before
    fun setUp() {
        createFixture()
    }

    private fun createFixture() {
        repository = FakeEventRepository()
        reconciler = FakeReconciler()
        viewModel = EventEditViewModel(
            eventRepository = repository,
            saveEventUseCase = SaveEventUseCase(repository, reconciler),
            deleteEventUseCase = DeleteEventUseCase(repository, reconciler),
            ioDispatcher = mainDispatcherRule.dispatcher
        )
    }

    @Test
    fun initialState_isUninitialized() {
        assertNull(viewModel.uiState.value.editMode)
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Uninitialized)
    }

    @Test
    fun uninitialized_rejectsSaveDeleteAndBack() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.saveEvent(Event(uid = "save"))
        viewModel.deleteEvent(Event(uid = "delete"))
        viewModel.requestBack()
        runCurrent()

        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Uninitialized)
        assertEquals(0, repository.saveAttempts)
        assertEquals(0, repository.deleteAttempts)
    }

    @Test
    fun switchToAddMode_entersAddModeAndIsIdempotent() {
        viewModel.switchToAddMode()
        viewModel.switchToAddMode()

        assertSame(EditMode.AddEventMode, viewModel.uiState.value.editMode)
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Idle)
    }

    @Test
    fun switchToEditMode_whenEventExists_loadsEvent() = runTest(mainDispatcherRule.dispatcher) {
        val event = Event(uid = "event-1", summary = "Loaded")
        repository.eventToLoad = event

        viewModel.switchToEditMode(event.uid)
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Initializing)
        runCurrent()

        assertEquals(EditMode.EditEventMode(event), viewModel.uiState.value.editMode)
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Idle)
    }

    @Test
    fun switchToEditMode_whenEventIsMissing_entersFailedState() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToEditMode("missing")
            runCurrent()

            assertNull(viewModel.uiState.value.editMode)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Failed)
        }

    @Test
    fun switchToEditMode_whenRepositoryFails_preservesCause() =
        runTest(mainDispatcherRule.dispatcher) {
            val failure = IllegalStateException("load failed")
            repository.loadFailure = failure

            viewModel.switchToEditMode("event-1")
            runCurrent()

            val state = viewModel.uiState.value.operationState as EditOperationState.Failed
            assertSame(failure, state.cause)
        }

    @Test
    fun switchToEditMode_whenCancelled_doesNotConvertCancellationToFailure() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loadFailure = CancellationException("cancelled")

            viewModel.switchToEditMode("event-1")
            runCurrent()

            assertNull(viewModel.uiState.value.editMode)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Initializing)
        }

    @Test
    fun switchToEditMode_whenCalledTwiceWhileLoading_queriesOnlyOnce() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.eventToLoad = Event(uid = "event-1")

            viewModel.switchToEditMode("event-1")
            viewModel.switchToEditMode("event-2")
            runCurrent()

            assertEquals(1, repository.loadAttempts)
            assertEquals(
                EditMode.EditEventMode(repository.eventToLoad!!),
                viewModel.uiState.value.editMode
            )
        }

    @Test
    fun initialization_whenCalledConcurrently_onlyOneModeWins() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.eventToLoad = Event(uid = "event-1")

            runConcurrently(
                { viewModel.switchToAddMode() },
                { viewModel.switchToEditMode("event-1") }
            )
            runCurrent()

            when (viewModel.uiState.value.editMode) {
                EditMode.AddEventMode -> assertEquals(0, repository.loadAttempts)
                is EditMode.EditEventMode -> assertEquals(1, repository.loadAttempts)
                null -> throw AssertionError("One initialization attempt must succeed")
            }
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Idle)
        }

    @Test
    fun loading_rejectsSaveDeleteAndBack() = runTest(mainDispatcherRule.dispatcher) {
        repository.eventToLoad = Event(uid = "loaded")
        viewModel.switchToEditMode("loaded")

        viewModel.saveEvent(Event(uid = "save"))
        viewModel.deleteEvent(Event(uid = "delete"))
        viewModel.requestBack()

        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Initializing)
        assertEquals(0, repository.saveAttempts)
        assertEquals(0, repository.deleteAttempts)
        runCurrent()
        assertEquals(
            EditMode.EditEventMode(repository.eventToLoad!!),
            viewModel.uiState.value.editMode
        )
    }

    @Test
    fun initializedMode_cannotBeReplaced() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.switchToAddMode()
        repository.eventToLoad = Event(uid = "event-1")

        viewModel.switchToEditMode("event-1")
        runCurrent()

        assertSame(EditMode.AddEventMode, viewModel.uiState.value.editMode)
        assertEquals(0, repository.loadAttempts)
    }

    @Test
    fun saveEvent_transitionsToSavingThenSuccessAndReconciles() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val event = Event(uid = "event-1")

            viewModel.saveEvent(event)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Saving)
            runCurrent()

            assertEquals(listOf(event), repository.savedEvents)
            assertEquals(1, reconciler.calls)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
        }

    @Test
    fun saveEvent_whenCalledAgainWhileSaving_ignoresDuplicate() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val first = Event(uid = "first")
            val second = Event(uid = "second")

            viewModel.saveEvent(first)
            viewModel.saveEvent(second)
            runCurrent()

            assertEquals(listOf(first), repository.savedEvents)
            assertEquals(1, reconciler.calls)
        }

    @Test
    fun saveEvent_whenCalledConcurrently_savesOnlyOnce() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.switchToAddMode()
        val events = List(16) { Event(uid = "event-$it") }

        runConcurrently(*events.map { event -> { viewModel.saveEvent(event) } }.toTypedArray())
        runCurrent()

        assertEquals(1, repository.saveAttempts)
        assertEquals(1, repository.savedEvents.size)
        assertEquals(1, reconciler.calls)
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
    }

    @Test
    fun saveEvent_afterFailure_canRetry() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.switchToAddMode()
        val failure = IllegalStateException("save failed")
        repository.saveFailure = failure
        val event = Event(uid = "event-1")

        viewModel.saveEvent(event)
        runCurrent()
        assertSame(
            failure,
            (viewModel.uiState.value.operationState as EditOperationState.Failed).cause
        )

        repository.saveFailure = null
        viewModel.saveEvent(event)
        runCurrent()

        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
        assertEquals(2, repository.saveAttempts)
        assertEquals(1, reconciler.calls)
    }

    @Test
    fun saveEvent_whenCancelled_staysSavingUntilScopeIsDisposed() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            repository.saveFailure = CancellationException("cancelled")

            viewModel.saveEvent(Event(uid = "event-1"))
            runCurrent()

            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Saving)
        }

    @Test
    fun saveEvent_whenReconciliationFails_preservesFailureAfterSaving() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val failure = IllegalStateException("reconciliation failed")
            reconciler.failure = failure
            val event = Event(uid = "event-1")

            viewModel.saveEvent(event)
            runCurrent()

            assertEquals(listOf(event), repository.savedEvents)
            assertSame(
                failure,
                (viewModel.uiState.value.operationState as EditOperationState.Failed).cause
            )
        }

    @Test
    fun deleteEvent_transitionsToDeletingThenSuccessAndReconciles() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val event = Event(uid = "event-1")

            viewModel.deleteEvent(event)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Deleting)
            runCurrent()

            assertEquals(listOf(event.uid), repository.deletedUids)
            assertEquals(1, reconciler.calls)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
        }

    @Test
    fun deleteEvent_afterFailure_canRetry() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.switchToAddMode()
        repository.deleteFailure = IllegalStateException("delete failed")
        val event = Event(uid = "event-1")

        viewModel.deleteEvent(event)
        runCurrent()
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Failed)

        repository.deleteFailure = null
        viewModel.deleteEvent(event)
        runCurrent()

        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
        assertEquals(2, repository.deleteAttempts)
        assertEquals(1, reconciler.calls)
    }

    @Test
    fun deleteEvent_whenCalledAgainWhileDeleting_ignoresDuplicate() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val first = Event(uid = "first")
            val second = Event(uid = "second")

            viewModel.deleteEvent(first)
            viewModel.deleteEvent(second)
            runCurrent()

            assertEquals(listOf(first.uid), repository.deletedUids)
            assertEquals(1, reconciler.calls)
        }

    @Test
    fun deleteEvent_whenCalledConcurrently_deletesOnlyOnce() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val events = List(16) { Event(uid = "event-$it") }

            runConcurrently(*events.map { event -> { viewModel.deleteEvent(event) } }
                .toTypedArray())
            runCurrent()

            assertEquals(1, repository.deleteAttempts)
            assertEquals(1, repository.deletedUids.size)
            assertEquals(1, reconciler.calls)
            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
        }

    @Test
    fun deleteEvent_whenCancelled_staysDeletingUntilScopeIsDisposed() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            repository.deleteFailure = CancellationException("cancelled")

            viewModel.deleteEvent(Event(uid = "event-1"))
            runCurrent()

            assertTrue(viewModel.uiState.value.operationState is EditOperationState.Deleting)
        }

    @Test
    fun deleteEvent_whenReconciliationFails_preservesFailureAfterDeleting() =
        runTest(mainDispatcherRule.dispatcher) {
            viewModel.switchToAddMode()
            val failure = IllegalStateException("reconciliation failed")
            reconciler.failure = failure
            val event = Event(uid = "event-1")

            viewModel.deleteEvent(event)
            runCurrent()

            assertEquals(listOf(event.uid), repository.deletedUids)
            assertSame(
                failure,
                (viewModel.uiState.value.operationState as EditOperationState.Failed).cause
            )
        }

    @Test
    fun requestBack_isAllowedOnlyWhenIdleOrFailed() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.switchToAddMode()
        viewModel.requestBack()
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)

        createFixture()
        viewModel.switchToAddMode()
        repository.saveFailure = IllegalStateException("save failed")
        viewModel.saveEvent(Event(uid = "event-1"))
        viewModel.requestBack()
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Saving)
        runCurrent()
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Failed)
        viewModel.requestBack()
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Success)
    }

    @Test
    fun consumeOperationSuccess_consumesEachSuccessExactlyOnce() {
        viewModel.switchToAddMode()
        viewModel.requestBack()

        assertTrue(viewModel.consumeOperationSuccess())
        assertTrue(viewModel.uiState.value.operationState is EditOperationState.Idle)
        assertTrue(!viewModel.consumeOperationSuccess())
    }

    private fun runConcurrently(vararg actions: () -> Unit) {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val workers = actions.map { action ->
            thread(start = true) {
                ready.countDown()
                start.await()
                action()
            }
        }

        ready.await()
        start.countDown()
        workers.forEach(Thread::join)
    }
}

private class FakeEventRepository : EventRepository {
    var eventToLoad: Event? = null
    var loadFailure: Exception? = null
    var saveFailure: Exception? = null
    var deleteFailure: Exception? = null
    var saveAttempts = 0
    var deleteAttempts = 0
    var loadAttempts = 0
    val savedEvents = mutableListOf<Event>()
    val deletedUids = mutableListOf<String>()

    override suspend fun getEventByUid(uid: String): Event? {
        loadAttempts++
        loadFailure?.let { throw it }
        return eventToLoad
    }

    override suspend fun observeEventsOverlappingRange(
        rangeStart: ZonedDateTime,
        rangeEnd: ZonedDateTime
    ): Flow<List<Event>> = emptyFlow()

    override suspend fun saveEvent(event: Event) {
        saveAttempts++
        saveFailure?.let { throw it }
        savedEvents += event
    }

    override suspend fun deleteEventByUid(uid: String) {
        deleteAttempts++
        deleteFailure?.let { throw it }
        deletedUids += uid
    }
}

private class FakeReconciler : AlarmOccurrenceReconciler {
    var calls = 0
    var failure: Exception? = null

    override fun reconcileAlarmOccurrences() {
        calls++
        failure?.let { throw it }
    }
}