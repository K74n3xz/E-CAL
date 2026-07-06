package net.k74n3xz.ecal.core.application.usecase

import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.AlarmScheduler
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReconcileAlarmOccurrencesUseCaseTest {
    private val triggerAt = Instant.parse("2026-07-04T01:00:00Z")

    @Test
    fun cancellation_marksUnknownBeforeSystemOperation_thenCancelled() = runTest {
        val repository = ReconcileRepository(
            reconciliation = listOf(AlarmOccurrence(1, 10, triggerAt)) to emptyList()
        )
        val scheduler = RecordingScheduler(repository.transitions)

        ReconcileAlarmOccurrencesUseCase(repository, scheduler)()

        assertEquals(listOf("unknown:1", "cancel:1", "cancelled:1"), repository.transitions)
    }

    @Test
    fun scheduling_marksUnknownBeforeSystemOperation_thenScheduled() = runTest {
        val repository = ReconcileRepository(
            emptyList<AlarmOccurrence>() to listOf(AlarmOccurrence(2, 20, triggerAt))
        )
        val scheduler = RecordingScheduler(repository.transitions)

        ReconcileAlarmOccurrencesUseCase(repository, scheduler)()

        assertEquals(
            listOf("unknown:2", "schedule:2:$triggerAt", "scheduled:2"),
            repository.transitions
        )
    }

    @Test
    fun schedulerFailure_leavesOccurrenceUnknownAndPropagates() = runTest {
        val failure = IllegalStateException("scheduler failed")
        val repository = ReconcileRepository(
            emptyList<AlarmOccurrence>() to listOf(AlarmOccurrence(3, 30, triggerAt))
        )
        val scheduler = RecordingScheduler(repository.transitions, failure)

        val thrown = runCatching { ReconcileAlarmOccurrencesUseCase(repository, scheduler)() }
            .exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("unknown:3", "schedule:3:$triggerAt"), repository.transitions)
    }
}

private class ReconcileRepository(
    private val reconciliation: Pair<List<AlarmOccurrence>, List<AlarmOccurrence>>
) : AlarmRepository {
    val transitions = mutableListOf<String>()

    override suspend fun getDueAlarmOccurrenceIdsAndActions(triggerAt: Instant) =
        emptyList<Pair<LongArray, Alarm.Action>>()

    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long) = Unit
    override suspend fun getAlarmOccurrenceNeedingReconciliation() = reconciliation
    override suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long) {
        transitions += "cancelled:$alarmOccurrenceId"
    }

    override suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long) {
        transitions += "scheduled:$alarmOccurrenceId"
    }

    override suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long) {
        transitions += "unknown:$alarmOccurrenceId"
    }

    override suspend fun markAllAlarmOccurrencesAsCancelled() = Unit
}

private class RecordingScheduler(
    private val transitions: MutableList<String>,
    private val failure: Exception? = null
) : AlarmScheduler {
    override fun schedule(id: Long, triggerAt: Instant) {
        transitions += "schedule:$id:$triggerAt"
        failure?.let { throw it }
    }

    override fun cancel(id: Long) {
        transitions += "cancel:$id"
        failure?.let { throw it }
    }
}
