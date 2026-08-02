package net.k74n3xz.ecal.ui.module.eventedit

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import java.io.FileInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.k74n3xz.ecal.ECALModule
import net.k74n3xz.ecal.HiltTestActivity
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.application.ApplicationFactory
import net.k74n3xz.ecal.core.application.port.`in`.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.`in`.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.`in`.service.EventQueryService
import net.k74n3xz.ecal.core.application.port.`in`.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.CleanupRemovingAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.ReconcileAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.UpdateAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.AddAttachmentCommand
import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.UpdateAttachmentCommand
import net.k74n3xz.ecal.core.application.port.out.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.out.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.application.port.out.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.out.repository.EventRepository
import net.k74n3xz.ecal.core.application.port.out.repository.result.AlarmOccurrenceNeedingReconciliation
import net.k74n3xz.ecal.core.data.RepositoryModule
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.ui.root.AppRoot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-420dpi")
@HiltAndroidTest
@UninstallModules(ECALModule::class, RepositoryModule::class)
class EventEditFlowTest {
    @BindValue
    @JvmField
    val eventRepository: EventRepository = ControllableEventRepository()

    private val fakeEventRepository: ControllableEventRepository
        get() = eventRepository as ControllableEventRepository

    @BindValue
    @JvmField
    val alarmRepository: AlarmRepository = UnusedAlarmRepository()

    @BindValue
    @JvmField
    val reconciler: AlarmReconciler = CountingReconciler()

    private val timeZoneProvider = FixedTimeZoneProvider()

    @BindValue
    @JvmField
    val eventQueryService: EventQueryService = ApplicationFactory.createEventQueryService(eventRepository)

    @BindValue
    @JvmField
    val handleDueAlarmsUseCase: HandleDueAlarmsUseCase =
        object : HandleDueAlarmsUseCase {
            override suspend fun invoke(now: Instant) = Unit
        }

    @BindValue
    @JvmField
    val reconcileAlarmOccurrencesUseCase: ReconcileAlarmOccurrencesUseCase =
        object : ReconcileAlarmOccurrencesUseCase {
            override suspend fun invoke() = Unit
        }

    @BindValue
    @JvmField
    val saveEventUseCase: SaveEventUseCase =
        ApplicationFactory.createSaveEventUseCase(eventRepository, reconciler, timeZoneProvider)

    @BindValue
    @JvmField
    val deleteEventUseCase: DeleteEventUseCase =
        ApplicationFactory.createDeleteEventUseCase(eventRepository, reconciler)

    private val attachmentGateway = ControllableAttachmentGateway()

    @BindValue
    @JvmField
    val attachmentQueryService: AttachmentQueryService = attachmentGateway

    @BindValue
    @JvmField
    val attendeeQueryService: AttendeeQueryService =
        object : AttendeeQueryService {
            override suspend fun findAttendeeById(attendeeId: Long): Attendee? = null

            override fun observeAllAttendees(): Flow<List<Attendee>> = MutableStateFlow(emptyList())
        }

    @BindValue
    @JvmField
    val addAttachmentUseCase: AddAttachmentUseCase = attachmentGateway

    @BindValue
    @JvmField
    val updateAttachmentUseCase: UpdateAttachmentUseCase = attachmentGateway

    @BindValue
    @JvmField
    val deleteAttachmentUseCase: DeleteAttachmentUseCase = attachmentGateway

    @BindValue
    @JvmField
    val cleanupRemovingAttachmentUseCase: CleanupRemovingAttachmentUseCase =
        object : CleanupRemovingAttachmentUseCase {
            override suspend fun invoke() = Unit
        }

    @BindValue
    @JvmField
    val saveAttendeeUseCase: SaveAttendeeUseCase = object : SaveAttendeeUseCase {
        override suspend fun invoke(attendee: Attendee) = Unit
    }

    @BindValue
    @JvmField
    val deleteAttendeeUseCase: DeleteAttendeeUseCase = object : DeleteAttendeeUseCase {
        override suspend fun invoke(attendeeId: Long) = Unit
    }

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
    fun attachmentTopBarEntry_andSystemBack_roundTripToCalendar() {
        composeRule.onNodeWithContentDescription(attachmentEntryDescription()).performClick()
        composeRule.onNodeWithText(attachmentPageTitle()).assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
    }

    @Test
    fun attachmentPage_displaysRepositoryAttachmentsAndActions() {
        val attachment = Attachment(
            id = 7,
            description = "Old description",
            name = "agenda.pdf",
            mimeType = "application/pdf",
            sizeBytes = 2048
        )
        attachmentGateway.attachments.value = listOf(attachment)
        composeRule.onNodeWithContentDescription(attachmentEntryDescription()).performClick()
        composeRule.onNodeWithText(attachment.name).assertIsDisplayed()
        composeRule.onNodeWithText(attachment.mimeType, substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(editAttachmentDescription(attachment.name)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(deleteAttachmentDescription(attachment.name)).assertIsDisplayed()
    }

    @Test
    fun saveWhilePending_blocksBackThenReturnsAfterSuccess() {
        openAddEvent()
        fakeEventRepository.saveGate = CompletableDeferred()

        clickSave()
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

        clickSave()

        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()
        composeRule.onNodeWithText(newEventTitle()).assertIsDisplayed()
    }

    @Test
    fun saveFailure_canRetryAndReturnToCalendar() {
        openAddEvent()
        fakeEventRepository.saveFailure = IllegalStateException("save failed")
        clickSave()
        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()

        fakeEventRepository.saveFailure = null
        composeRule.onNodeWithContentDescription(snackbarDismissDescription()).performClick()
        clickSave()

        composeRule.waitUntil { fakeEventRepository.savedEvents.size == 1 }
        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        assertEquals(1, (reconciler as CountingReconciler).calls)
    }

    @Test
    fun saveOnTwoConsecutiveEditorVisits_returnsToCalendarOncePerSave() {
        repeat(2) { expectedSaveCount ->
            openAddEvent()
            clickSave()

            composeRule.waitUntil { fakeEventRepository.savedEvents.size == expectedSaveCount + 1 }
            composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        }

        assertEquals(2, fakeEventRepository.savedEvents.size)
    }

    @Test
    fun editExistingEvent_loadsStoredContent() {
        val summary = "Existing event"
        val event = testEvent(uid = "existing", summary = summary)
        fakeEventRepository.events.value = listOf(event)

        composeRule.onNodeWithText(summary).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(editDescription()).performClick()

        composeRule.onNodeWithText(editEventTitle()).assertIsDisplayed()
        composeRule.onNodeWithText(summary).assertIsDisplayed()
    }

    @Test
    fun loadingExistingEvent_allowsBackBeforeLoadCompletes() {
        val event = testEvent(uid = "existing", summary = "Existing event")
        fakeEventRepository.events.value = listOf(event)
        fakeEventRepository.loadGate = CompletableDeferred()

        composeRule.onNodeWithContentDescription(editDescription()).performClick()
        composeRule.waitUntil { fakeEventRepository.loadStarted.isCompleted }
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
        fakeEventRepository.loadGate.complete(Unit)
    }

    @Test
    fun loadFailure_showsErrorAndAllowsBack() {
        val event = testEvent(uid = "existing", summary = "Existing event")
        fakeEventRepository.events.value = listOf(event)
        fakeEventRepository.loadFailure = IllegalStateException("load failed")

        composeRule.onNodeWithContentDescription(editDescription()).performClick()
        composeRule.onNodeWithText(operationFailedText()).assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithContentDescription(addEventDescription()).assertIsDisplayed()
    }

    @Test
    fun deleteWhilePending_blocksBackThenReturnsAfterSuccess() {
        val event = testEvent(uid = "existing", summary = "Existing event")
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
        val event = testEvent(uid = "existing", summary = "Existing event")
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
            testEvent(uid = "first", summary = "First event"),
            testEvent(uid = "second", summary = "Second event")
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

    private fun clickSave() {
        composeRule.onNodeWithText(saveText()).performScrollTo().performClick()
    }

    private fun resource(id: Int, vararg arguments: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *arguments)

    private fun addEventDescription() = resource(R.string.fab_content_description_add_new_event)
    private fun newEventTitle() = resource(R.string.topbar_title_text_new_event)
    private fun saveText() = resource(R.string.text_save)
    private fun operationFailedText() = resource(R.string.error_event_operation_failed)
    private fun snackbarDismissDescription() = resource(androidx.compose.material3.R.string.m3c_snackbar_dismiss)
    private fun editEventTitle() = resource(R.string.topbar_title_text_edit_event)
    private fun editDescription() = resource(R.string.button_content_description_edit)
    private fun deleteText() = resource(R.string.text_delete)
    private fun attachmentEntryDescription() = resource(R.string.button_content_description_manage_attachments)
    private fun attachmentPageTitle() = resource(R.string.topbar_title_attachments)
    private fun editAttachmentDescription(name: String) =
        resource(R.string.button_content_description_edit_attachment, name)

    private fun deleteAttachmentDescription(name: String) =
        resource(R.string.button_content_description_delete_attachment, name)
}

private fun testEvent(uid: String, summary: String): Event = Event(
    uid = uid,
    summary = summary,
    schedule = EventTiming.Timed.InstantTiming(Instant.now())
)

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

    override fun observeEventsOverlappingRange(start: ZonedDateTime, end: ZonedDateTime): Flow<List<Event>> = events

    override suspend fun saveEvent(event: Event, timeZone: ZoneId) {
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

private class CountingReconciler : AlarmReconciler {
    var calls = 0

    override fun request() {
        calls++
    }
}

private class FixedTimeZoneProvider : TimeZoneProvider {
    override val timeZone = MutableStateFlow(ZoneId.of("UTC"))
}

private class UnusedAlarmRepository : AlarmRepository {
    private fun unused(): Nothing = error("AlarmRepository is not used by EventEditFlowTest")

    override suspend fun getAlarmOccurrenceNeedingReconciliation(): AlarmOccurrenceNeedingReconciliation = unused()
    override suspend fun getDueAlarmOccurrenceIdsAndActions(now: Instant): List<Pair<LongArray, Action>> = unused()
    override suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long) = unused()
    override suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long) = unused()
    override suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long) = unused()
    override suspend fun markAllAlarmOccurrencesAsCancelled() = unused()
    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long, hasHandled: Boolean, timeZone: ZoneId) =
        unused()

    override suspend fun rebuildAllDayAlarmOccurrences(timeZone: ZoneId) = unused()
}

private class ControllableAttachmentGateway :
    AttachmentQueryService,
    AddAttachmentUseCase,
    UpdateAttachmentUseCase,
    DeleteAttachmentUseCase {
    val attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val addCommands = CopyOnWriteArrayList<AddAttachmentCommand>()
    val updateCommands = CopyOnWriteArrayList<UpdateAttachmentCommand>()
    val deletedIds = CopyOnWriteArrayList<Long>()

    override suspend fun findAttachmentById(attachmentId: Long): Attachment? =
        attachments.value.firstOrNull { it.id == attachmentId }

    override fun observeAvailableAttachments(): Flow<List<Attachment>> = attachments

    override fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>> =
        attachments

    override suspend fun openAttachmentById(attachmentId: Long): FileInputStream? = null

    override suspend fun invoke(addAttachmentCommand: AddAttachmentCommand) {
        addCommands += addAttachmentCommand
    }

    override suspend fun invoke(updateAttachmentCommand: UpdateAttachmentCommand) {
        updateCommands += updateAttachmentCommand
        attachments.value = attachments.value.map {
            if (it.id == updateAttachmentCommand.id) {
                it.copy(description = updateAttachmentCommand.description)
            } else {
                it
            }
        }
    }

    override suspend fun invoke(attachmentId: Long) {
        deletedIds += attachmentId
        attachments.value = attachments.value.filterNot { it.id == attachmentId }
    }
}
