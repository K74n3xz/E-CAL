package net.k74n3xz.ecal.core.application.usecase

import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.port.NotificationPublisher
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HandleDueAlarmsUseCaseTest {
    private val now = Instant.parse("2026-07-04T00:00:00Z")

    @Test
    fun emptyBatch_doesNothing() = runTest {
        val fixture = Fixture()

        fixture.useCase(now)

        assertTrue(fixture.repository.processedIds.isEmpty())
        assertTrue(fixture.publisher.published.isEmpty())
        assertEquals(0, fixture.reconciler.calls)
    }

    @Test
    fun displayOccurrences_arePublishedAndProcessed_thenReconciledOnce() = runTest {
        val fixture = Fixture(
            due = listOf(
                longArrayOf(1, 2) to Alarm.Action.Display("first"),
                longArrayOf(3) to Alarm.Action.Display("second")
            )
        )

        fixture.useCase(now)

        assertEquals(
            listOf(1L to "first", 2L to "first", 3L to "second"),
            fixture.publisher.published
        )
        assertEquals(listOf(1L, 2L, 3L), fixture.repository.processedIds)
        assertEquals(1, fixture.reconciler.calls)
    }

    @Test
    fun groupWithoutOccurrences_doesNotReconcile() = runTest {
        val fixture = Fixture(due = listOf(longArrayOf() to Alarm.Action.Display("unused")))

        fixture.useCase(now)

        assertEquals(0, fixture.reconciler.calls)
    }

    @Test
    fun publishFailure_isPropagatedAndOccurrenceIsNotProcessed() = runTest {
        val failure = IllegalStateException("notification failed")
        val fixture = Fixture(due = listOf(longArrayOf(1) to Alarm.Action.Display("text")))
        fixture.publisher.failure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertTrue(fixture.repository.processedIds.isEmpty())
        assertEquals(0, fixture.reconciler.calls)
    }

    private class Fixture(due: List<Pair<LongArray, Alarm.Action>> = emptyList()) {
        val repository = FakeAlarmRepository(due)
        val reconciler = FakeReconciler()
        val publisher = FakePublisher()
        val useCase = HandleDueAlarmsUseCase(repository, reconciler, publisher)
    }
}

private class FakePublisher : NotificationPublisher {
    val published = mutableListOf<Pair<Long, String>>()
    var failure: Exception? = null

    override fun publish(id: Long, description: String) {
        failure?.let { throw it }
        published += id to description
    }
}

private class FakeReconciler : AlarmOccurrenceReconciler {
    var calls = 0
    override fun request() {
        calls++
    }
}

private class FakeAlarmRepository(
    private val due: List<Pair<LongArray, Alarm.Action>> = emptyList(),
    private val reconciliation: Pair<List<AlarmOccurrence>, List<AlarmOccurrence>> = emptyList<AlarmOccurrence>() to emptyList()
) : AlarmRepository {
    val processedIds = mutableListOf<Long>()
    val transitions = mutableListOf<String>()

    override suspend fun getDueAlarmOccurrenceIdsAndActions(triggerAt: Instant) = due
    override suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long) {
        processedIds += alarmOccurrenceId
    }

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
