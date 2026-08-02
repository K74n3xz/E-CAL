package net.k74n3xz.ecal.core.application.testing

import java.io.FileInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import net.k74n3xz.ecal.core.application.port.out.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.out.platform.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.out.platform.AttachmentCleanupScheduler
import net.k74n3xz.ecal.core.application.port.out.platform.AudioPlayer
import net.k74n3xz.ecal.core.application.port.out.platform.EmailSender
import net.k74n3xz.ecal.core.application.port.out.platform.NotificationPublisher
import net.k74n3xz.ecal.core.application.port.out.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.application.port.out.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttendeeRepository
import net.k74n3xz.ecal.core.application.port.out.repository.EventRepository
import net.k74n3xz.ecal.core.application.port.out.repository.result.AlarmOccurrenceNeedingReconciliation
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.event.EventTiming

internal object ApplicationUseCaseTestData {
    val now: Instant = Instant.parse("2026-07-04T00:00:00Z")
    val later: Instant = Instant.parse("2026-07-04T01:00:00Z")

    fun event(uid: String = "event-1") = Event(uid = uid, schedule = EventTiming.Timed.RangeTiming(now, later))

    fun occurrence(id: Long, alarmId: Long = id * 10, triggerAt: Instant = later) =
        AlarmOccurrence(id = id, alarmId = alarmId, triggerAt = triggerAt)
}

internal class RecordingEventRepository(
    private val calls: MutableList<String>,
    var saveFailure: Exception? = null,
    var deleteFailure: Exception? = null
) : EventRepository {
    var found: Event? = null
    var observed: Flow<List<Event>> = emptyFlow()
    val requestedUids = mutableListOf<String>()
    val observedRanges = mutableListOf<Pair<ZonedDateTime, ZonedDateTime>>()
    val savedEvents = mutableListOf<Event>()
    val savedTimeZones = mutableListOf<ZoneId>()
    val deletedUids = mutableListOf<String>()

    override suspend fun getEventByUid(uid: String): Event? {
        requestedUids += uid
        return found
    }

    override fun observeEventsOverlappingRange(start: ZonedDateTime, end: ZonedDateTime): Flow<List<Event>> {
        observedRanges += start to end
        return observed
    }

    override suspend fun saveEvent(event: Event, timeZone: ZoneId) {
        calls += "save:${event.uid}"
        saveFailure?.let { throw it }
        savedEvents += event
        savedTimeZones += timeZone
    }

    override suspend fun deleteEventByUid(uid: String) {
        calls += "delete:$uid"
        deleteFailure?.let { throw it }
        deletedUids += uid
    }
}

internal class RecordingAlarmRepository(
    private val calls: MutableList<String>,
    private val due: List<Pair<LongArray, Action>> = emptyList(),
    private val reconciliation: AlarmOccurrenceNeedingReconciliation =
        AlarmOccurrenceNeedingReconciliation(emptyList(), emptyList()),
    var processFailure: Exception? = null,
    var rebuildFailure: Exception? = null,
    var markUnknownFailure: Exception? = null,
    var markCancelledFailure: Exception? = null,
    var markScheduledFailure: Exception? = null
) : AlarmRepository {
    var dueQueryCount = 0
        private set
    var reconciliationQueryCount = 0
        private set
    var firstDueQueryRelease: CompletableDeferred<Unit>? = null
    var firstReconciliationQueryRelease: CompletableDeferred<Unit>? = null
    val processedTimeZones = mutableListOf<ZoneId>()
    val processedResults = mutableListOf<Pair<Long, Boolean>>()
    val rebuiltTimeZones = mutableListOf<ZoneId>()

    override suspend fun getDueAlarmOccurrenceIdsAndActions(now: Instant): List<Pair<LongArray, Action>> {
        dueQueryCount++
        calls += "getDue:$now"
        if (dueQueryCount == 1) {
            firstDueQueryRelease?.await()
        }
        return due
    }

    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long, hasHandled: Boolean, timeZone: ZoneId) {
        calls += "process:$alarmOccurrenceId"
        processedTimeZones += timeZone
        processedResults += alarmOccurrenceId to hasHandled
        processFailure?.let { throw it }
    }

    override suspend fun rebuildAllDayAlarmOccurrences(timeZone: ZoneId) {
        calls += "rebuild:$timeZone"
        rebuildFailure?.let { throw it }
        rebuiltTimeZones += timeZone
    }

    override suspend fun getAlarmOccurrenceNeedingReconciliation(): AlarmOccurrenceNeedingReconciliation {
        reconciliationQueryCount++
        calls += "getReconciliation"
        if (reconciliationQueryCount == 1) {
            firstReconciliationQueryRelease?.await()
        }
        return reconciliation
    }

    override suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long) {
        calls += "cancelled:$alarmOccurrenceId"
        markCancelledFailure?.let { throw it }
    }

    override suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long) {
        calls += "scheduled:$alarmOccurrenceId"
        markScheduledFailure?.let { throw it }
    }

    override suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long) {
        calls += "unknown:$alarmOccurrenceId"
        markUnknownFailure?.let { throw it }
    }

    override suspend fun markAllAlarmOccurrencesAsCancelled() {
        calls += "cancelAll"
    }
}

internal class RecordingReconciler(private val calls: MutableList<String>, var failure: Exception? = null) :
    AlarmReconciler {
    var callCount = 0
        private set

    override fun request() {
        callCount++
        calls += "reconcile"
        failure?.let { throw it }
    }
}

internal class RecordingPublisher(private val calls: MutableList<String>, var failure: Exception? = null) :
    NotificationPublisher {
    var result: Boolean = true

    override fun publish(id: Long, description: String): Boolean {
        calls += "publish:$id:$description"
        failure?.let { throw it }
        return result
    }
}

internal class RecordingAudioPlayer(
    private val calls: MutableList<String> = mutableListOf(),
    var result: Boolean = true,
    var failure: Exception? = null
) : AudioPlayer {
    val attachmentIds = mutableListOf<Long?>()

    override fun play(attachmentId: Long?): Boolean {
        calls += "audio"
        attachmentIds += attachmentId
        failure?.let { throw it }
        return result
    }
}

internal class RecordingEmailSender(
    private val calls: MutableList<String> = mutableListOf(),
    var result: Boolean = true,
    var failure: Exception? = null
) : EmailSender {
    data class Arguments(
        val id: Int,
        val subject: String,
        val text: String,
        val receivers: List<String>,
        val attachments: List<String>
    )

    val arguments = mutableListOf<Arguments>()

    override fun send(
        id: Int,
        subject: String,
        text: String,
        receivers: List<String>,
        attachments: List<String>
    ): Boolean {
        calls += "email"
        arguments += Arguments(id, subject, text, receivers, attachments)
        failure?.let { throw it }
        return result
    }
}

internal class RecordingTimeZoneProvider(
    private val current: ZoneId = ZoneOffset.ofHours(8),
    var failure: Exception? = null
) : TimeZoneProvider {
    override val timeZone = flowOf(current)
    var currentReadCount = 0
        private set

    override suspend fun currentTimeZone(): ZoneId {
        currentReadCount++
        failure?.let { throw it }
        return current
    }
}

internal class RecordingAttachmentRepository(private val calls: MutableList<String> = mutableListOf()) :
    AttachmentRepository {
    var found: Attachment? = null
    var observed: Flow<List<Attachment>> = emptyFlow()
    var observedByMimeTopLevelType: Flow<List<Attachment>> = emptyFlow()
    var openedAttachment: FileInputStream? = null
    var failure: Exception? = null
    val sharingTokens = mutableMapOf<Long, String?>()
    val requestedIds = mutableListOf<Long>()
    val requestedMimeTopLevelTypes = mutableListOf<String>()
    val openedIds = mutableListOf<Long>()
    val sharingTokenIds = mutableListOf<Long>()

    override suspend fun findAttachmentById(id: Long): Attachment? {
        calls += "findAttachment:$id"
        requestedIds += id
        failure?.let { throw it }
        return found
    }

    override fun observeAvailableAttachments(): Flow<List<Attachment>> = observed

    override fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>> {
        requestedMimeTopLevelTypes += topLevelType
        return observedByMimeTopLevelType
    }

    override suspend fun addAttachment(description: String?, platformToken: String) {
        calls += "addAttachment:$description:$platformToken"
        failure?.let { throw it }
    }

    override suspend fun updateAttachmentDescription(id: Long, description: String?) {
        calls += "updateAttachment:$id:$description"
        failure?.let { throw it }
    }

    override suspend fun deleteAttachmentById(id: Long) {
        calls += "deleteAttachment:$id"
        failure?.let { throw it }
    }

    override suspend fun cleanup() {
        calls += "cleanupAttachments"
        failure?.let { throw it }
    }

    override suspend fun openAttachmentById(id: Long): FileInputStream? {
        calls += "openAttachment:$id"
        openedIds += id
        failure?.let { throw it }
        return openedAttachment
    }

    override suspend fun generateAttachmentSharingTokenById(id: Long): String? {
        calls += "shareAttachment:$id"
        sharingTokenIds += id
        failure?.let { throw it }
        return sharingTokens[id]
    }
}

internal class RecordingAttendeeRepository(private val calls: MutableList<String> = mutableListOf()) :
    AttendeeRepository {
    var found: Attendee? = null
    var observed: Flow<List<Attendee>> = emptyFlow()
    var failure: Exception? = null
    val requestedIds = mutableListOf<Long>()

    override suspend fun getAttendeeById(attendeeId: Long): Attendee? {
        calls += "findAttendee:$attendeeId"
        requestedIds += attendeeId
        failure?.let { throw it }
        return found
    }

    override fun observeAllAttendees(): Flow<List<Attendee>> = observed

    override suspend fun saveAttendee(attendee: Attendee) {
        calls += "saveAttendee:${attendee.email}"
        failure?.let { throw it }
    }

    override suspend fun deleteAttendeeById(attendeeId: Long) {
        calls += "deleteAttendee:$attendeeId"
        failure?.let { throw it }
    }
}

internal class RecordingAttachmentCleanupScheduler(
    private val calls: MutableList<String>,
    var failure: Exception? = null
) : AttachmentCleanupScheduler {
    override fun request() {
        calls += "scheduleAttachmentCleanup"
        failure?.let { throw it }
    }
}

internal class RecordingScheduler(
    private val calls: MutableList<String>,
    var scheduleFailure: Exception? = null,
    var cancelFailure: Exception? = null
) : AlarmScheduler {
    override fun schedule(id: Long, triggerAt: Instant) {
        calls += "schedule:$id:$triggerAt"
        scheduleFailure?.let { throw it }
    }

    override fun cancel(id: Long) {
        calls += "cancel:$id"
        cancelFailure?.let { throw it }
    }
}
