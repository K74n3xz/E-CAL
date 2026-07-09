package net.k74n3xz.ecal.core.application.usecase

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HandleDueAlarmsUseCaseTest {
    private val now = ApplicationUseCaseTestData.now

    @Test
    fun emptyBatch_queriesRepositoryOnly() = runTest {
        val fixture = Fixture()

        fixture.useCase(now)

        assertEquals(listOf("getDue:$now"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun displayOccurrences_arePublishedAndProcessed_thenReconciledOnce() = runTest {
        val fixture = Fixture(
            listOf(
                longArrayOf(1, 2) to Alarm.Action.Display("first"),
                longArrayOf(3) to Alarm.Action.Display("second")
            )
        )

        fixture.useCase(now)

        assertEquals(
            listOf(
                "getDue:$now",
                "publish:1:first",
                "process:1",
                "publish:2:first",
                "process:2",
                "publish:3:second",
                "process:3",
                "reconcile"
            ),
            fixture.calls
        )
        assertEquals(1, fixture.reconciler.callCount)
    }

    @Test
    fun emptyGroupsAreIgnoredButNonEmptyBatchReconcilesOnce() = runTest {
        val fixture = Fixture(
            listOf(
                longArrayOf() to Alarm.Action.Display("unused"),
                longArrayOf(4) to Alarm.Action.Display("actual"),
                longArrayOf() to Alarm.Action.Display("also-unused")
            )
        )

        fixture.useCase(now)

        assertEquals(listOf("getDue:$now", "publish:4:actual", "process:4", "reconcile"), fixture.calls)
        assertEquals(1, fixture.reconciler.callCount)
    }

    @Test
    fun groupWithoutOccurrences_doesNotReconcile() = runTest {
        val fixture = Fixture(listOf(longArrayOf() to Alarm.Action.Display("unused")))

        fixture.useCase(now)

        assertEquals(listOf("getDue:$now"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun publishFailure_isPropagatedAndOccurrenceIsNotProcessed() = runTest {
        val failure = IllegalStateException("notification failed")
        val fixture = Fixture(listOf(longArrayOf(1) to Alarm.Action.Display("text")))
        fixture.publisher.failure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getDue:$now", "publish:1:text"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun processFailure_isPropagatedAndStopsBatchBeforeReconcile() = runTest {
        val failure = IllegalStateException("process failed")
        val fixture = Fixture(listOf(longArrayOf(1, 2) to Alarm.Action.Display("text")))
        fixture.repository.processFailure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getDue:$now", "publish:1:text", "process:1"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun concurrentInvocations_areSerialized() = runTest {
        val releaseFirstQuery = CompletableDeferred<Unit>()
        val fixture = Fixture()
        fixture.repository.firstDueQueryRelease = releaseFirstQuery

        val first = async { fixture.useCase(now) }
        runCurrent()
        assertEquals(1, fixture.repository.dueQueryCount)

        val second = async { fixture.useCase(now) }
        runCurrent()
        assertEquals(1, fixture.repository.dueQueryCount)

        releaseFirstQuery.complete(Unit)
        first.await()
        second.await()

        assertEquals(2, fixture.repository.dueQueryCount)
        assertEquals(listOf("getDue:$now", "getDue:$now"), fixture.calls)
    }

    private class Fixture(due: List<Pair<LongArray, Alarm.Action>> = emptyList()) {
        val calls = mutableListOf<String>()
        val repository = RecordingAlarmRepository(calls, due)
        val reconciler = RecordingReconciler(calls)
        val publisher = RecordingPublisher(calls)
        val useCase = HandleDueAlarmsUseCase(repository, reconciler, publisher)
    }
}
