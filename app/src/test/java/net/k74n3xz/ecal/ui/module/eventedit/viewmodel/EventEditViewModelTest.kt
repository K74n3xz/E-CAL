package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import java.io.FileInputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.inbound.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.inbound.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.inbound.service.EventQueryService
import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository
import net.k74n3xz.ecal.testutils.MainDispatcherRule
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EventEditOperationState
import net.k74n3xz.ecal.ui.presentation.form.enumeration.alarm.ActionType
import net.k74n3xz.ecal.ui.presentation.form.replaceText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var queryService: FakeEventQueryService
    private lateinit var attachmentQueryService: FakeAttachmentQueryService
    private lateinit var attendeeQueryService: FakeAttendeeQueryService
    private lateinit var saveUseCase: RecordingSaveEventUseCase
    private lateinit var deleteUseCase: RecordingDeleteEventUseCase
    private lateinit var preferences: FakePreferenceRepository
    private lateinit var viewModel: EventEditViewModel

    @Before
    fun setUp() {
        createFixture()
    }

    @Test
    fun initialState_isUninitialized() {
        assertTrue(viewModel.uiState.value.operationState is EventEditOperationState.InitializationState.Uninitialized)
        assertTrue(viewModel.alarmForms.value.isEmpty())
    }

    @Test
    fun initializeWithoutUid_createsNewTimedEventAndIsIdempotent() = runTest(mainDispatcherRule.dispatcher) {
        runCurrent()
        val before = Instant.now()

        viewModel.initialize(null)
        viewModel.initialize("ignored")

        val state = viewModel.uiState.value.operationState
        assertEquals(EventEditOperationState.Idle(null), state)
        assertEquals(0, queryService.attempts)
        assertTrue(viewModel.alarmForms.value.isEmpty())
        val resolved = viewModel.eventForm.resolve(
            originalEvent = Event(
                uid = "placeholder",
                schedule = EventTiming.Timed.InstantTiming(Instant.EPOCH)
            ),
            newAlarms = emptyList(),
            timeZone = preferences.timeZone.value
        ).getOrThrow()
        val at = (resolved.schedule as EventTiming.Timed.InstantTiming).at
        assertFalse(at.isBefore(before))
    }

    @Test
    fun initializeWithoutUid_generatesUuidV7EventUidWhenSaved() = runTest(mainDispatcherRule.dispatcher) {
        runCurrent()
        viewModel.initialize(null)

        viewModel.saveEvent()
        runCurrent()

        val uid = saveUseCase.saved.single().uid
        assertTrue(uid.endsWith("-ECAL_event"))
        assertEquals(7, UUID.fromString(uid.removeSuffix("-ECAL_event")).version())
    }

    @Test
    fun initializeWithUid_loadsEventAndBuildsFormsUsingCurrentTimeZone() = runTest(mainDispatcherRule.dispatcher) {
        preferences.timeZone.value = ZoneId.of("Asia/Hong_Kong")
        val alarm = Alarm(
            id = 5,
            action = Action.Display("Reminder"),
            trigger = Trigger.AbsoluteTrigger(Instant.parse("2026-07-20T03:00:00Z"))
        )
        queryService.event = Event(
            uid = "event-1",
            summary = "Loaded",
            schedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-07-20T03:00:00Z")),
            alarms = listOf(alarm)
        )
        runCurrent()

        viewModel.initialize("event-1")
        assertTrue(viewModel.uiState.value.operationState is EventEditOperationState.InitializationState.Initializing)
        runCurrent()

        assertEquals(EventEditOperationState.Idle(null), viewModel.uiState.value.operationState)
        assertEquals("Loaded", viewModel.eventForm.summary.text.toString())
        assertEquals(11, viewModel.eventForm.startTime.hour)
        assertEquals(1, viewModel.alarmForms.value.size)
        assertEquals(11, viewModel.alarmForms.value.single().atTime.hour)
    }

    @Test
    fun missingEvent_entersInitializationFailedState() = runTest(mainDispatcherRule.dispatcher) {
        runCurrent()

        viewModel.initialize("missing")
        runCurrent()

        val state =
            viewModel.uiState.value.operationState as EventEditOperationState.InitializationState.InitializationFailed
        assertTrue(state.cause is IllegalArgumentException)
        assertEquals("Event(uid=missing) doesn't exist.", state.cause.message)
    }

    @Test
    fun queryFailure_isPreservedAndCancellationLeavesInitializingState() = runTest(mainDispatcherRule.dispatcher) {
        val failure = IllegalStateException("load failed")
        queryService.failure = failure
        runCurrent()

        viewModel.initialize("event")
        runCurrent()

        val failed =
            viewModel.uiState.value.operationState as EventEditOperationState.InitializationState.InitializationFailed
        assertSame(failure, failed.cause)

        createFixture()
        queryService.failure = CancellationException("cancelled")
        runCurrent()
        viewModel.initialize("event")
        runCurrent()
        assertTrue(viewModel.uiState.value.operationState is EventEditOperationState.InitializationState.Initializing)
    }

    @Test
    fun repeatedOrConcurrentInitialization_queriesAtMostOnce() = runTest(mainDispatcherRule.dispatcher) {
        queryService.event = testEvent("loaded")
        runCurrent()

        runConcurrently(
            { viewModel.initialize("loaded") },
            { viewModel.initialize("other") },
            { viewModel.initialize(null) }
        )
        runCurrent()

        assertTrue(queryService.attempts in 0..1)
        assertTrue(viewModel.uiState.value.operationState is EventEditOperationState.Idle)
    }

    @Test
    fun addAndRemoveAlarm_updatesPublishedForms() = runTest(mainDispatcherRule.dispatcher) {
        runCurrent()
        viewModel.initialize(null)

        viewModel.addAlarm()

        val alarm = viewModel.alarmForms.value.single()
        assertEquals(Action.Display(""), alarm.resolve(null, preferences.timeZone.value).getOrThrow().action)
        assertEquals(Duration.ofMinutes(-15), alarm.offset)

        viewModel.removeAlarm(0)
        assertTrue(viewModel.alarmForms.value.isEmpty())
    }

    @Test
    fun audioAttachments_areFilteredAndSelectedAttachmentIsSaved() = runTest(mainDispatcherRule.dispatcher) {
        val audio = Attachment(7, null, "alarm.mp3", "audio/mpeg", 42)
        val document = Attachment(8, null, "agenda.pdf", "application/pdf", 84)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.audioAttachments.collect {}
        }
        attachmentQueryService.attachments.value = listOf(audio, document)
        runCurrent()

        viewModel.initialize(null)
        viewModel.addAlarm()
        viewModel.alarmForms.value.single().apply {
            actionType = ActionType.AUDIO
            audioAttachment = audio
        }
        viewModel.saveEvent()
        runCurrent()

        assertEquals(listOf(audio), viewModel.audioAttachments.value)
        assertEquals(listOf("audio"), attachmentQueryService.requestedMimeTopLevelTypes)
        assertEquals(Action.Audio(audio), saveUseCase.saved.single().alarms.single().action)
    }

    @Test
    fun attendeesAndAllAttachments_areExposedAndSavedByEmailAction() = runTest(mainDispatcherRule.dispatcher) {
        val attendee = Attendee(5, "User", null, "user@example.com")
        val audio = Attachment(7, null, "alarm.mp3", "audio/mpeg", 42)
        val document = Attachment(8, null, "agenda.pdf", "application/pdf", 84)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.attendees.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.attachments.collect {}
        }
        attendeeQueryService.attendees.value = listOf(attendee)
        attachmentQueryService.attachments.value = listOf(audio, document)
        runCurrent()

        viewModel.initialize(null)
        viewModel.addAlarm()
        viewModel.alarmForms.value.single().apply {
            actionType = ActionType.EMAIL
            summary.replaceText("Subject")
            descriptionEmail.replaceText("Body")
            attendees += attendee
            hasAttachments = true
            attachments += document
        }
        viewModel.saveEvent()
        runCurrent()

        assertEquals(listOf(attendee), viewModel.attendees.value)
        assertEquals(listOf(audio, document), viewModel.attachments.value)
        assertEquals(
            Action.Email("Body", "Subject", listOf(attendee), listOf(document)),
            saveUseCase.saved.single().alarms.single().action
        )
    }

    @Test
    fun saveEvent_resolvesEditedFormAndAlarmsThenExits() = runTest(mainDispatcherRule.dispatcher) {
        runCurrent()
        viewModel.initialize(null)
        viewModel.eventForm.summary.replaceText("New event")
        viewModel.eventForm.isSummaryClear = false
        viewModel.addAlarm()

        viewModel.saveEvent()
        assertEquals(EventEditOperationState.Saving, viewModel.uiState.value.operationState)
        runCurrent()

        val saved = saveUseCase.saved.single()
        assertEquals("New event", saved.summary)
        assertEquals(1, saved.alarms.size)
        assertEquals(EventEditOperationState.Exit, viewModel.uiState.value.operationState)
    }

    @Test
    fun invalidAlarm_preventsUseCaseAndReturnsErrorToIdle() = runTest(mainDispatcherRule.dispatcher) {
        runCurrent()
        viewModel.initialize(null)
        viewModel.addAlarm()
        viewModel.alarmForms.value.single().apply {
            isRepetitionEnabled = true
            repeat.replaceText("")
        }

        viewModel.saveEvent()
        runCurrent()

        val state = viewModel.uiState.value.operationState as EventEditOperationState.Idle
        assertTrue(state.failure is IllegalArgumentException)
        assertTrue(saveUseCase.saved.isEmpty())
    }

    @Test
    fun saveFailure_returnsToIdleAndCanRetry() = runTest(mainDispatcherRule.dispatcher) {
        val failure = IllegalStateException("save failed")
        saveUseCase.failure = failure
        runCurrent()
        viewModel.initialize(null)

        viewModel.saveEvent()
        runCurrent()

        assertSame(failure, (viewModel.uiState.value.operationState as EventEditOperationState.Idle).failure)
        saveUseCase.failure = null
        viewModel.saveEvent()
        runCurrent()
        assertEquals(2, saveUseCase.attempts)
        assertEquals(EventEditOperationState.Exit, viewModel.uiState.value.operationState)
    }

    @Test
    fun saveCancellation_staysSavingAndConcurrentCallsInvokeOnce() = runTest(mainDispatcherRule.dispatcher) {
        saveUseCase.failure = CancellationException("cancelled")
        runCurrent()
        viewModel.initialize(null)

        runConcurrently(*Array(12) { { viewModel.saveEvent() } })
        runCurrent()

        assertEquals(1, saveUseCase.attempts)
        assertEquals(EventEditOperationState.Saving, viewModel.uiState.value.operationState)
    }

    @Test
    fun deleteEvent_forwardsLoadedUidAndCanRetryAfterFailure() = runTest(mainDispatcherRule.dispatcher) {
        queryService.event = testEvent("event-7")
        val failure = IllegalStateException("delete failed")
        deleteUseCase.failure = failure
        runCurrent()
        viewModel.initialize("event-7")
        runCurrent()

        viewModel.deleteEvent()
        assertEquals(EventEditOperationState.Deleting, viewModel.uiState.value.operationState)
        runCurrent()
        assertSame(failure, (viewModel.uiState.value.operationState as EventEditOperationState.Idle).failure)

        deleteUseCase.failure = null
        viewModel.deleteEvent()
        runCurrent()
        assertEquals(listOf("event-7"), deleteUseCase.deleted)
        assertEquals(2, deleteUseCase.attempts)
        assertEquals(EventEditOperationState.Exit, viewModel.uiState.value.operationState)
    }

    @Test
    fun deleteCancellation_staysDeletingAndConcurrentCallsInvokeOnce() = runTest(mainDispatcherRule.dispatcher) {
        deleteUseCase.failure = CancellationException("cancelled")
        runCurrent()
        viewModel.initialize(null)

        runConcurrently(*Array(12) { { viewModel.deleteEvent() } })
        runCurrent()

        assertEquals(1, deleteUseCase.attempts)
        assertEquals(EventEditOperationState.Deleting, viewModel.uiState.value.operationState)
    }

    @Test
    fun operationsAreRejectedUntilInitializedAndWhileBusy() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.saveEvent()
        viewModel.deleteEvent()
        assertEquals(0, saveUseCase.attempts)
        assertEquals(0, deleteUseCase.attempts)

        runCurrent()
        viewModel.initialize(null)
        saveUseCase.failure = CancellationException("busy")
        viewModel.saveEvent()
        viewModel.deleteEvent()
        runCurrent()

        assertEquals(1, saveUseCase.attempts)
        assertEquals(0, deleteUseCase.attempts)
    }

    @Test
    fun requestAndConsumeExit_followStateContract() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.requestExit()
        assertEquals(EventEditOperationState.Exit, viewModel.uiState.value.operationState)
        assertTrue(viewModel.consumeExitState())
        assertEquals(EventEditOperationState.Exited, viewModel.uiState.value.operationState)
        assertFalse(viewModel.consumeExitState())

        createFixture()
        runCurrent()
        viewModel.initialize(null)
        saveUseCase.failure = CancellationException("busy")
        viewModel.saveEvent()
        viewModel.requestExit()
        assertEquals(EventEditOperationState.Saving, viewModel.uiState.value.operationState)
    }

    private fun createFixture() {
        queryService = FakeEventQueryService()
        attachmentQueryService = FakeAttachmentQueryService()
        attendeeQueryService = FakeAttendeeQueryService()
        saveUseCase = RecordingSaveEventUseCase()
        deleteUseCase = RecordingDeleteEventUseCase()
        preferences = FakePreferenceRepository()
        viewModel = EventEditViewModel(
            queryService,
            attachmentQueryService,
            attendeeQueryService,
            saveUseCase,
            deleteUseCase,
            preferences
        )
    }

    private fun runConcurrently(vararg actions: () -> Unit) {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val workers = actions.map { action ->
            thread {
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

private class FakeAttachmentQueryService : AttachmentQueryService {
    val attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val requestedMimeTopLevelTypes = mutableListOf<String>()

    override suspend fun findAttachmentById(attachmentId: Long): Attachment? =
        attachments.value.firstOrNull { it.id == attachmentId }

    override fun observeAvailableAttachments(): Flow<List<Attachment>> = attachments

    override fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>> {
        requestedMimeTopLevelTypes += topLevelType
        return attachments.map { values ->
            values.filter { it.mimeType.startsWith("$topLevelType/") }
        }
    }

    override suspend fun openAttachmentById(attachmentId: Long): FileInputStream? = null
}

private class FakeAttendeeQueryService : AttendeeQueryService {
    val attendees = MutableStateFlow<List<Attendee>>(emptyList())

    override suspend fun findAttendeeById(attendeeId: Long): Attendee? =
        attendees.value.firstOrNull { it.id == attendeeId }

    override fun observeAllAttendees(): Flow<List<Attendee>> = attendees
}

private fun testEvent(uid: String) = Event(
    uid = uid,
    schedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-07-20T03:00:00Z"))
)

private class FakeEventQueryService : EventQueryService {
    var event: Event? = null
    var failure: Exception? = null
    var attempts: Int = 0

    override suspend fun findEventByUid(uid: String): Event? {
        attempts++
        failure?.let { throw it }
        return event
    }

    override fun observeEventsOverlappingRange(rangeStart: ZonedDateTime, rangeEnd: ZonedDateTime): Flow<List<Event>> =
        emptyFlow()
}

private class RecordingSaveEventUseCase : SaveEventUseCase {
    var failure: Exception? = null
    var attempts: Int = 0
    val saved = mutableListOf<Event>()

    override suspend fun invoke(event: Event) {
        attempts++
        failure?.let { throw it }
        saved += event
    }
}

private class RecordingDeleteEventUseCase : DeleteEventUseCase {
    var failure: Exception? = null
    var attempts: Int = 0
    val deleted = mutableListOf<String>()

    override suspend fun invoke(eventUid: String) {
        attempts++
        failure?.let { throw it }
        deleted += eventUid
    }
}

private class FakePreferenceRepository : PreferenceRepository {
    override val timeZone = MutableStateFlow(ZoneId.of("UTC"))
}
