package net.k74n3xz.ecal.core.database

import java.time.Duration
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.database.calendar.CalendarDatabase
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.core.database.repository.DatabaseAlarmRepository
import net.k74n3xz.ecal.core.model.Alarm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatabaseAlarmRepositoryTest {
    private lateinit var database: CalendarDatabase
    private lateinit var repository: DatabaseAlarmRepository

    @Before
    fun setUp() {
        database = createInMemoryCalendarDatabase()
        repository = DatabaseAlarmRepository(
            database,
            database.eventComponentDao(),
            database.alarmComponentDao(),
            database.alarmInstanceDao(),
            database.alarmDao()
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun getDueAlarmOccurrenceIdsAndActions_groupsMultipleDueInstancesByAlarmComponent() = runTest {
        database.insertEventComponent("event-a")
        database.insertEventComponent("event-b")
        val alarmA = database.insertAlarmComponent("event-a", description = "A")
        val alarmB = database.insertAlarmComponent("event-b", description = "B")
        database.insertAlarmInstance(alarmA, id = 101, triggerAt = TestInstant.minusSeconds(2))
        database.insertAlarmInstance(alarmA, id = 102, triggerAt = TestInstant.minusSeconds(1))
        database.insertAlarmInstance(alarmB, id = 201, triggerAt = TestInstant)

        val due = repository.getDueAlarmOccurrenceIdsAndActions(TestInstant)

        assertEquals(2, due.size)
        val dueByDescription = due.associateBy { (_, action) ->
            (action as Alarm.Action.Display).description
        }
        assertEquals(setOf(101L, 102L), dueByDescription.getValue("A").first.toSet())
        assertEquals(setOf(201L), dueByDescription.getValue("B").first.toSet())
    }

    @Test
    fun getDueAlarmOccurrenceIdsAndActions_excludesFutureAndInactiveOccurrences() = runTest {
        database.insertEventComponent("event")
        val alarm = database.insertAlarmComponent("event", description = "due")
        database.insertAlarmInstance(alarm, id = 101, triggerAt = TestInstant.minusSeconds(1))
        database.insertAlarmInstance(alarm, id = 102, triggerAt = TestInstant.plusSeconds(1))
        database.insertAlarmInstance(
            alarm,
            id = 103,
            triggerAt = TestInstant.minusSeconds(2),
            desiredState = DesiredState.INACTIVE
        )

        val due = repository.getDueAlarmOccurrenceIdsAndActions(TestInstant)

        assertEquals(1, due.size)
        assertEquals(listOf(101L), due.single().first.toList())
    }

    @Test
    fun processDueOccurrence_marksItInactiveAndDoesNotCreateNextForNonRepeatingAlarm() = runTest {
        database.insertEventComponent("event")
        val alarmId = database.insertAlarmComponent("event", description = "once")
        database.insertAlarmInstance(alarmId, id = 101, triggerAt = TestInstant.minusSeconds(1))

        repository.processDueAlarmOccurrence(101)

        assertTrue(repository.getDueAlarmOccurrenceIdsAndActions(TestInstant.plusSeconds(60)).isEmpty())
        assertEquals(
            listOf(
                AlarmInstanceSnapshot(
                    id = 101,
                    alarmComponentId = alarmId,
                    triggerAt = TestInstant.minusSeconds(1),
                    desiredState = DesiredState.INACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            ),
            database.queryAlarmInstanceSnapshots()
        )
    }

    @Test
    fun processDueOccurrence_createsNextOccurrenceForRepeatingAlarm() = runTest {
        database.insertEventComponent("event")
        val alarmId = database.insertAlarmComponent(
            "event",
            description = "repeat",
            interval = Duration.ofMinutes(5),
            repeat = 2
        )
        database.insertAlarmInstance(alarmId, id = 101, triggerAt = TestInstant)

        repository.processDueAlarmOccurrence(101)

        assertTrue(repository.getDueAlarmOccurrenceIdsAndActions(TestInstant).isEmpty())
        val instances = database.queryAlarmInstanceSnapshots()
        assertEquals(2, instances.size)
        assertEquals(DesiredState.INACTIVE, instances.first { it.id == 101L }.desiredState)
        assertEquals(TestInstant.plus(Duration.ofMinutes(5)), instances.last().triggerAt)
        assertEquals(DesiredState.ACTIVE, instances.last().desiredState)
        assertEquals(ReconcileResult.CANCELLED, instances.last().lastReconcileResult)
    }

    @Test
    fun processDueOccurrence_repeatingAlarmStopsAtRepeatLimit() = runTest {
        database.insertEventComponent("event")
        val alarmId = database.insertAlarmComponent(
            "event",
            description = "repeat",
            interval = Duration.ofMinutes(5),
            repeat = 2
        )
        database.insertAlarmInstance(
            alarmId,
            id = 101,
            triggerAt = TestInstant,
            desiredState = DesiredState.INACTIVE
        )
        database.insertAlarmInstance(
            alarmId,
            id = 102,
            triggerAt = TestInstant.plus(Duration.ofMinutes(5)),
            desiredState = DesiredState.INACTIVE
        )
        database.insertAlarmInstance(
            alarmId,
            id = 103,
            triggerAt = TestInstant.plus(Duration.ofMinutes(10))
        )

        repository.processDueAlarmOccurrence(103)

        val instances = database.queryAlarmInstanceSnapshots()
        assertEquals(3, instances.size)
        assertTrue(instances.all { it.desiredState == DesiredState.INACTIVE })
    }

    @Test
    fun getAlarmOccurrenceNeedingReconciliation_splitsCancelAndScheduleQueues() = runTest {
        database.insertEventComponent("event")
        val alarm = database.insertAlarmComponent("event")
        database.insertAlarmInstance(
            alarm,
            id = 101,
            desiredState = DesiredState.INACTIVE,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )
        database.insertAlarmInstance(
            alarm,
            id = 102,
            desiredState = DesiredState.INACTIVE,
            lastReconcileResult = ReconcileResult.CANCELLED
        )
        database.insertAlarmInstance(
            alarm,
            id = 103,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.CANCELLED
        )
        database.insertAlarmInstance(
            alarm,
            id = 104,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )

        val (toCancel, toSchedule) = repository.getAlarmOccurrenceNeedingReconciliation()

        assertEquals(listOf(101L), toCancel.map { it.id })
        assertEquals(listOf(103L), toSchedule.map { it.id })
    }

    @Test
    fun markAlarmOccurrenceAsCancelledScheduledUnknown_updatesOnlyTargetOccurrence() = runTest {
        database.insertEventComponent("event")
        val alarm = database.insertAlarmComponent("event")
        database.insertAlarmInstance(alarm, id = 101, lastReconcileResult = ReconcileResult.SCHEDULED)
        database.insertAlarmInstance(alarm, id = 102, lastReconcileResult = ReconcileResult.SCHEDULED)

        repository.markAlarmOccurrenceAsUnknown(101)
        repository.markAlarmOccurrenceAsCancelled(102)
        repository.markAlarmOccurrenceAsScheduled(101)

        val instances = database.queryAlarmInstanceSnapshots().associateBy { it.id }
        assertEquals(ReconcileResult.SCHEDULED, instances.getValue(101).lastReconcileResult)
        assertEquals(ReconcileResult.CANCELLED, instances.getValue(102).lastReconcileResult)
    }

    @Test
    fun markAllAlarmOccurrencesAsCancelled_updatesEveryOccurrence() = runTest {
        database.insertEventComponent("event")
        val alarm = database.insertAlarmComponent("event")
        database.insertAlarmInstance(alarm, id = 101, lastReconcileResult = ReconcileResult.SCHEDULED)
        database.insertAlarmInstance(alarm, id = 102, lastReconcileResult = ReconcileResult.UNKNOWN)

        repository.markAllAlarmOccurrencesAsCancelled()

        assertTrue(
            database.queryAlarmInstanceSnapshots()
                .all { it.lastReconcileResult == ReconcileResult.CANCELLED }
        )
    }

    @Test
    fun processDueOccurrence_onUnlinkedOccurrenceOnlyMarksInactive() = runTest {
        database.insertAlarmInstance(
            alarmId = null,
            id = 101,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )

        repository.processDueAlarmOccurrence(101)

        assertEquals(
            listOf(
                AlarmInstanceSnapshot(
                    id = 101,
                    alarmComponentId = null,
                    triggerAt = TestInstant,
                    desiredState = DesiredState.INACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            ),
            database.queryAlarmInstanceSnapshots()
        )
    }
}
