package net.k74n3xz.ecal.core.application.impl.usecase

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.out.repository.result.AlarmOccurrenceNeedingReconciliation
import net.k74n3xz.ecal.core.application.testing.ApplicationUseCaseTestData
import net.k74n3xz.ecal.core.application.testing.RecordingAlarmRepository
import net.k74n3xz.ecal.core.application.testing.RecordingScheduler
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconcileAlarmOccurrencesUseCaseTest {
    private class Fixture(
        reconciliation: Pair<List<AlarmOccurrence>, List<AlarmOccurrence>> = emptyList<AlarmOccurrence>() to emptyList()
    ) {
        val calls = mutableListOf<String>()
        val repository = RecordingAlarmRepository(
            calls,
            reconciliation = AlarmOccurrenceNeedingReconciliation(reconciliation.first, reconciliation.second)
        )
        val scheduler = RecordingScheduler(calls)
        val useCase = ReconcileAlarmOccurrencesUseCaseImpl(repository, scheduler)
    }

    private val triggerAt = ApplicationUseCaseTestData.later
    private val noOccurrences = emptyList<AlarmOccurrence>()

    @Test
    fun cancellation_marksUnknownBeforeSystemOperation_thenCancelled() = runTest {
        val fixture = Fixture(listOf(ApplicationUseCaseTestData.occurrence(1)) to noOccurrences)

        fixture.useCase()

        assertEquals(listOf("getReconciliation", "unknown:1", "cancel:1", "cancelled:1"), fixture.calls)
    }

    @Test
    fun scheduling_marksUnknownBeforeSystemOperation_thenScheduled() = runTest {
        val fixture = Fixture(noOccurrences to listOf(ApplicationUseCaseTestData.occurrence(2)))

        fixture.useCase()

        assertEquals(listOf("getReconciliation", "unknown:2", "schedule:2:$triggerAt", "scheduled:2"), fixture.calls)
    }

    @Test
    fun mixedCancellationAndScheduling_processesAllCancellationsBeforeSchedules() = runTest {
        val fixture = Fixture(
            listOf(
                ApplicationUseCaseTestData.occurrence(1),
                ApplicationUseCaseTestData.occurrence(2)
            ) to listOf(
                ApplicationUseCaseTestData.occurrence(3),
                ApplicationUseCaseTestData.occurrence(4)
            )
        )

        fixture.useCase()

        assertEquals(
            listOf(
                "getReconciliation",
                "unknown:1",
                "cancel:1",
                "cancelled:1",
                "unknown:2",
                "cancel:2",
                "cancelled:2",
                "unknown:3",
                "schedule:3:$triggerAt",
                "scheduled:3",
                "unknown:4",
                "schedule:4:$triggerAt",
                "scheduled:4"
            ),
            fixture.calls
        )
    }

    @Test
    fun scheduleFailure_leavesOccurrenceUnknownAndPropagates() = runTest {
        val failure = IllegalStateException("scheduler failed")
        val fixture = Fixture(noOccurrences to listOf(ApplicationUseCaseTestData.occurrence(3)))
        fixture.scheduler.scheduleFailure = failure

        val thrown = runCatching { fixture.useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getReconciliation", "unknown:3", "schedule:3:$triggerAt"), fixture.calls)
    }

    @Test
    fun cancelFailure_leavesOccurrenceUnknownAndPropagates() = runTest {
        val failure = IllegalStateException("cancel failed")
        val fixture = Fixture(listOf(ApplicationUseCaseTestData.occurrence(5)) to noOccurrences)
        fixture.scheduler.cancelFailure = failure

        val thrown = runCatching { fixture.useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getReconciliation", "unknown:5", "cancel:5"), fixture.calls)
    }

    @Test
    fun markUnknownFailure_preventsSystemOperationAndPropagates() = runTest {
        val failure = IllegalStateException("unknown failed")
        val fixture = Fixture(listOf(ApplicationUseCaseTestData.occurrence(6)) to noOccurrences)
        fixture.repository.markUnknownFailure = failure

        val thrown = runCatching { fixture.useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getReconciliation", "unknown:6"), fixture.calls)
    }

    @Test
    fun finalCancelledMarkFailure_propagatesAfterSystemCancel() = runTest {
        val failure = IllegalStateException("cancelled mark failed")
        val fixture = Fixture(listOf(ApplicationUseCaseTestData.occurrence(7)) to noOccurrences)
        fixture.repository.markCancelledFailure = failure

        val thrown = runCatching { fixture.useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getReconciliation", "unknown:7", "cancel:7", "cancelled:7"), fixture.calls)
    }

    @Test
    fun finalScheduledMarkFailure_propagatesAfterSystemSchedule() = runTest {
        val failure = IllegalStateException("scheduled mark failed")
        val fixture = Fixture(noOccurrences to listOf(ApplicationUseCaseTestData.occurrence(8)))
        fixture.repository.markScheduledFailure = failure

        val thrown = runCatching { fixture.useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getReconciliation", "unknown:8", "schedule:8:$triggerAt", "scheduled:8"), fixture.calls)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun concurrentInvocations_areSerialized() = runTest {
        val releaseFirstQuery = CompletableDeferred<Unit>()
        val fixture = Fixture()
        fixture.repository.firstReconciliationQueryRelease = releaseFirstQuery

        val first = async { fixture.useCase() }
        runCurrent()
        assertEquals(1, fixture.repository.reconciliationQueryCount)

        val second = async { fixture.useCase() }
        runCurrent()
        assertEquals(1, fixture.repository.reconciliationQueryCount)

        releaseFirstQuery.complete(Unit)
        first.await()
        second.await()

        assertEquals(2, fixture.repository.reconciliationQueryCount)
        assertEquals(listOf("getReconciliation", "getReconciliation"), fixture.calls)
    }
}
