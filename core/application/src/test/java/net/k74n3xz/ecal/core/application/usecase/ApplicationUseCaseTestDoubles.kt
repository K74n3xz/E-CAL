package net.k74n3xz.ecal.core.application.usecase

import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.port.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.NotificationPublisher
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.repository.EventRepository
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import net.k74n3xz.ecal.core.model.Event

internal object ApplicationUseCaseTestData {
    val now: Instant = Instant.parse("2026-07-04T00:00:00Z")
    val later: Instant = Instant.parse("2026-07-04T01:00:00Z")

    fun event(uid: String = "event-1") = Event(uid = uid, startAt = now, endAt = later)

    fun occurrence(id: Long, alarmId: Long = id * 10, triggerAt: Instant = later) =
        AlarmOccurrence(id = id, alarmId = alarmId, triggerAt = triggerAt)
}

internal class RecordingEventRepository(
    private val calls: MutableList<String>,
    var saveFailure: Exception? = null,
    var deleteFailure: Exception? = null
) : EventRepository {
    val savedEvents = mutableListOf<Event>()
    val deletedUids = mutableListOf<String>()

    override suspend fun getEventByUid(uid: String): Event? = null

    override fun observeEventsOverlappingRange(rangeStart: ZonedDateTime, rangeEnd: ZonedDateTime): Flow<List<Event>> =
        emptyFlow()

    override suspend fun saveEvent(event: Event) {
        calls += "save:${event.uid}"
        saveFailure?.let { throw it }
        savedEvents += event
    }

    override suspend fun deleteEventByUid(uid: String) {
        calls += "delete:$uid"
        deleteFailure?.let { throw it }
        deletedUids += uid
    }
}

internal class RecordingAlarmRepository(
    private val calls: MutableList<String>,
    private val due: List<Pair<LongArray, Alarm.Action>> = emptyList(),
    private val reconciliation: Pair<List<AlarmOccurrence>, List<AlarmOccurrence>> =
        emptyList<AlarmOccurrence>() to emptyList(),
    var processFailure: Exception? = null,
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

    override suspend fun getDueAlarmOccurrenceIdsAndActions(triggerAt: Instant): List<Pair<LongArray, Alarm.Action>> {
        dueQueryCount++
        calls += "getDue:$triggerAt"
        if (dueQueryCount == 1) {
            firstDueQueryRelease?.await()
        }
        return due
    }

    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long) {
        calls += "process:$alarmOccurrenceId"
        processFailure?.let { throw it }
    }

    override suspend fun getAlarmOccurrenceNeedingReconciliation(): Pair<List<AlarmOccurrence>, List<AlarmOccurrence>> {
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
    AlarmOccurrenceReconciler {
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
    override fun publish(id: Long, description: String) {
        calls += "publish:$id:$description"
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
