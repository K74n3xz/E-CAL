package net.k74n3xz.ecal.ui.module.eventedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.k74n3xz.ecal.ECALModule
import net.k74n3xz.ecal.HiltTestActivity
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.port.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.NotificationPublisher
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.repository.EventRepository
import net.k74n3xz.ecal.core.application.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.usecase.ReconcileAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.database.DatabaseModule
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.ui.root.AppRoot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList

@HiltAndroidTest
@UninstallModules(ECALModule::class, DatabaseModule::class)
class EventEditFlowTest {
    @BindValue
    @JvmField
    val eventRepository: EventRepository = ControllableEventRepository()

    private val fakeEventRepository get() = eventRepository as ControllableEventRepository

    @BindValue
    @JvmField
    val alarmRepository: AlarmRepository = NoOpAlarmRepository()

    @BindValue
    @JvmField
    val reconciler: AlarmOccurrenceReconciler = CountingReconciler()

    @BindValue
    @JvmField
    val alarmScheduler: AlarmScheduler = NoOpAlarmScheduler()

    @BindValue
    @JvmField
    val notificationPublisher: NotificationPublisher = NoOpNotificationPublisher()

    @BindValue
    @JvmField
    val saveEventUseCase = SaveEventUseCase(eventRepository, reconciler)

    @BindValue
    @JvmField
    val deleteEventUseCase = DeleteEventUseCase(eventRepository, reconciler)

    @BindValue
    @JvmField
    val handleDueAlarmsUseCase =
        HandleDueAlarmsUseCase(alarmRepository, reconciler, notificationPublisher)

    @BindValue
    @JvmField
    val reconcileAlarmOccurrencesUseCase =
        ReconcileAlarmOccurrencesUseCase(alarmRepository, alarmScheduler)

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.setContent { AppRoot() }
    }

    @Test
    fun backWhileIdle_returnsToCalendar() {
        openAddEvent()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
    }

    @Test
    fun saveWhilePending_blocksBackThenReturnsAfterSuccess() {
        openAddEvent()
        fakeEventRepository.saveGate = CompletableDeferred()

        composeRule.onNodeWithText(saveText()).performClick()
        composeRule.waitUntil { fakeEventRepository.saveStarted.isCompleted }
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(newEventTitle()).assertIsDisplayed()

        fakeEventRepository.saveGate.complete(Unit)
        composeRule.waitUntil { fakeEventRepository.savedEvents.size == 1 }
        composeRule.waitUntil {
            composeRule.onAllNodesWithText(newEventTitle()).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
    }

    @Test
    fun saveFailure_keepsEditorOpenAndShowsLocalizedError() {
        openAddEvent()
        fakeEventRepository.saveFailure = IllegalStateException("save failed")

        composeRule.onNodeWithText(saveText()).performClick()

        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()
        composeRule.onNodeWithText(newEventTitle()).assertIsDisplayed()
    }

    @Test
    fun saveFailure_canRetryAndReturnToCalendar() {
        openAddEvent()
        fakeEventRepository.saveFailure = IllegalStateException("save failed")
        composeRule.onNodeWithText(saveText()).performClick()
        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()

        fakeEventRepository.saveFailure = null
        composeRule.onNodeWithContentDescription(snackbarDismissDescription()).performClick()
        composeRule.onNodeWithText(saveText()).performClick()

        composeRule.waitUntil { fakeEventRepository.savedEvents.size == 1 }
        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        assertEquals(1, (reconciler as CountingReconciler).calls)
    }

    @Test
    fun saveOnTwoConsecutiveEditorVisits_returnsToCalendarOncePerSave() {
        repeat(2) { expectedSaveCount ->
            openAddEvent()
            composeRule.onNodeWithText(saveText()).performClick()

            composeRule.waitUntil { fakeEventRepository.savedEvents.size == expectedSaveCount + 1 }
            composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        }

        assertEquals(2, fakeEventRepository.savedEvents.size)
    }

    @Test
    fun editExistingEvent_loadsStoredContent() {
        val summary = "Existing event"
        val event = Event(uid = "existing", summary = summary, startAt = Instant.now())
        fakeEventRepository.events.value = listOf(event)

        composeRule.onNodeWithText(summary).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(editDescription()).performClick()

        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()
        composeRule.onNodeWithText(summary).assertIsDisplayed()
    }

    @Test
    fun loadingExistingEvent_blocksBackUntilLoadCompletes() {
        val event = Event(uid = "existing", summary = "Existing event", startAt = Instant.now())
        fakeEventRepository.events.value = listOf(event)
        fakeEventRepository.loadGate = CompletableDeferred()

        composeRule.onNodeWithContentDescription(editDescription()).performClick()
        composeRule.waitUntil { fakeEventRepository.loadStarted.isCompleted }
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(loadingText()).assertIsDisplayed()

        fakeEventRepository.loadGate.complete(Unit)
        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()
    }

    @Test
    fun loadFailure_showsErrorAndAllowsBack() {
        val event = Event(uid = "existing", summary = "Existing event", startAt = Instant.now())
        fakeEventRepository.events.value = listOf(event)
        fakeEventRepository.loadFailure = IllegalStateException("load failed")

        composeRule.onNodeWithContentDescription(editDescription()).performClick()
        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
    }

    @Test
    fun deleteWhilePending_blocksBackThenReturnsAfterSuccess() {
        val event = Event(uid = "existing", summary = "Existing event", startAt = Instant.now())
        fakeEventRepository.events.value = listOf(event)
        fakeEventRepository.deleteGate = CompletableDeferred()
        composeRule.onNodeWithContentDescription(editDescription()).performClick()
        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()

        composeRule.onNodeWithText(deleteText()).performClick()
        composeRule.onAllNodesWithText(deleteText())[1].performClick()
        composeRule.waitUntil { fakeEventRepository.deleteStarted.isCompleted }
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()

        fakeEventRepository.deleteGate.complete(Unit)
        composeRule.waitUntil { fakeEventRepository.deletedUids.contains(event.uid) }
        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        assertEquals(1, (reconciler as CountingReconciler).calls)
    }

    @Test
    fun deleteFailure_keepsEditorOpenAndShowsLocalizedError() {
        val event = Event(uid = "existing", summary = "Existing event", startAt = Instant.now())
        fakeEventRepository.events.value = listOf(event)
        fakeEventRepository.deleteFailure = IllegalStateException("delete failed")
        composeRule.onNodeWithContentDescription(editDescription()).performClick()
        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()

        composeRule.onNodeWithText(deleteText()).performClick()
        composeRule.onAllNodesWithText(deleteText())[1].performClick()

        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()
        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()
    }

    @Test
    fun deleteOnTwoConsecutiveEditorVisits_returnsToCalendarOncePerDelete() {
        val events = listOf(
            Event(uid = "first", summary = "First event", startAt = Instant.now()),
            Event(uid = "second", summary = "Second event", startAt = Instant.now())
        )
        fakeEventRepository.events.value = events

        events.forEachIndexed { index, event ->
            composeRule.onNodeWithText(event.summary!!).assertIsDisplayed()
            composeRule.onAllNodesWithContentDescription(editDescription())[0].performClick()
            composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()
            composeRule.onNodeWithText(deleteText()).performClick()
            composeRule.onAllNodesWithText(deleteText())[1].performClick()

            composeRule.waitUntil { fakeEventRepository.deletedUids.size == index + 1 }
            composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        }

        assertEquals(listOf("first", "second"), fakeEventRepository.deletedUids)
    }

    private fun openAddEvent() {
        composeRule.onNodeWithContentDescription(addEventDescription()).performClick()
        composeRule.onNodeWithText(newEventTitle()).assertIsDisplayed()
    }

    private fun resource(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun addEventDescription() = resource(R.string.fab_content_description_add_new_event)
    private fun newEventTitle() = resource(R.string.topbar_title_text_new_event)
    private fun saveText() = resource(R.string.text_save)
    private fun operationFailedText() = resource(R.string.error_event_operation_failed)
    private fun snackbarDismissDescription() =
        resource(androidx.compose.material3.R.string.m3c_snackbar_dismiss)

    private fun editEventTitle() = resource(R.string.topbar_title_text_edit_event)
    private fun editDescription() = resource(R.string.button_content_description_edit)
    private fun loadingText() = resource(R.string.text_loading)
    private fun deleteText() = resource(R.string.text_delete)
}

private class ControllableEventRepository : EventRepository {
    val events = MutableStateFlow<List<Event>>(emptyList())
    val saveStarted = CompletableDeferred<Unit>()
    val loadStarted = CompletableDeferred<Unit>()
    val deleteStarted = CompletableDeferred<Unit>()
    var saveGate = CompletableDeferred(Unit)
    var loadGate = CompletableDeferred(Unit)
    var deleteGate = CompletableDeferred(Unit)
    var saveFailure: Exception? = null
    var loadFailure: Exception? = null
    var deleteFailure: Exception? = null
    val savedEvents = CopyOnWriteArrayList<Event>()
    val deletedUids = CopyOnWriteArrayList<String>()

    override suspend fun getEventByUid(uid: String): Event? {
        loadStarted.complete(Unit)
        loadGate.await()
        loadFailure?.let { throw it }
        return events.value.firstOrNull { it.uid == uid }
    }

    override fun observeEventsOverlappingRange(
        rangeStart: ZonedDateTime,
        rangeEnd: ZonedDateTime
    ): Flow<List<Event>> = events

    override suspend fun saveEvent(event: Event) {
        saveStarted.complete(Unit)
        saveGate.await()
        saveFailure?.let { throw it }
        savedEvents += event
        events.value += event
    }

    override suspend fun deleteEventByUid(uid: String) {
        deleteStarted.complete(Unit)
        deleteGate.await()
        deleteFailure?.let { throw it }
        deletedUids += uid
        events.value = events.value.filterNot { it.uid == uid }
    }
}

private class CountingReconciler : AlarmOccurrenceReconciler {
    var calls = 0
    override fun request() {
        calls++
    }
}

private class NoOpAlarmScheduler : AlarmScheduler {
    override fun schedule(id: Long, triggerAt: Instant) = Unit
    override fun cancel(id: Long) = Unit
}

private class NoOpNotificationPublisher : NotificationPublisher {
    override fun publish(id: Long, description: String) = Unit
}

private class NoOpAlarmRepository : AlarmRepository {
    override suspend fun getDueAlarmOccurrenceIdsAndActions(triggerAt: Instant) =
        emptyList<Pair<LongArray, Alarm.Action>>()

    override suspend fun getAlarmOccurrenceNeedingReconciliation(): Pair<List<AlarmOccurrence>, List<AlarmOccurrence>> =
        emptyList<AlarmOccurrence>() to emptyList()

    override suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long) = Unit
    override suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long) = Unit
    override suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long) = Unit
    override suspend fun markAllAlarmOccurrencesAsCancelled() = Unit
    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long) = Unit
}
