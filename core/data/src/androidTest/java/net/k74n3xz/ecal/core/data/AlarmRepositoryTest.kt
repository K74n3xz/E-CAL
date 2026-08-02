package net.k74n3xz.ecal.core.data

import java.time.Duration
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.data.repository.AlarmRepositoryImpl
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlarmRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: AlarmRepositoryImpl

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        repository = AlarmRepositoryImpl(
            database,
            database.eventDao(),
            database.alarmDao(),
            database.alarmOccurrenceDao()
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun dueOccurrences_groupIdsByAlarmAndReturnCompleteActions() = runTest {
        database.insertEvent(timedEvent("event-a"))
        database.insertEvent(timedEvent("event-b"))
        val alarmA = database.insertAlarm("event-a", absoluteDisplayAlarm(description = "A"))
        val attendee = net.k74n3xz.ecal.core.model.Attendee(
            id = 31,
            name = "Alex",
            email = "alex@example.com"
        )
        val emailAlarm = Alarm(
            action = Action.Email("Body", "Subject", listOf(attendee)),
            trigger = Trigger.AbsoluteTrigger(TestInstant)
        )
        val alarmB = database.insertAlarm("event-b", emailAlarm)
        database.insertAttendee(attendee)
        database.linkAttendee(alarmB, 31)
        database.insertOccurrence(alarmA, id = 101, triggerAt = TestInstant.minusSeconds(2))
        database.insertOccurrence(alarmA, id = 102, triggerAt = TestInstant.minusSeconds(1))
        database.insertOccurrence(alarmB, id = 201, triggerAt = TestInstant)

        val due = repository.getDueAlarmOccurrenceIdsAndActions(TestInstant)

        assertEquals(2, due.size)
        val display = due.single { it.second is Action.Display }
        val email = due.single { it.second is Action.Email }
        assertEquals(setOf(101L, 102L), display.first.toSet())
        assertEquals(Action.Display("A"), display.second)
        assertEquals(setOf(201L), email.first.toSet())
        assertEquals(
            Action.Email("Body", "Subject", listOf(attendee)),
            email.second
        )
    }

    @Test
    fun dueOccurrences_excludeFutureAndInactiveRows() = runTest {
        database.insertEvent()
        val alarmId = database.insertAlarm("event", absoluteDisplayAlarm())
        database.insertOccurrence(alarmId, id = 101, triggerAt = TestInstant.minusSeconds(1))
        database.insertOccurrence(alarmId, id = 102, triggerAt = TestInstant.plusSeconds(1))
        database.insertOccurrence(
            alarmId,
            id = 103,
            triggerAt = TestInstant.minusSeconds(2),
            desiredState = DesiredState.INACTIVE
        )

        val due = repository.getDueAlarmOccurrenceIdsAndActions(TestInstant)

        assertEquals(listOf(101L), due.single().first.toList())
    }

    @Test
    fun processDueOccurrence_retiresNonRepeatingOccurrence() = runTest {
        database.insertEvent()
        val alarmId = database.insertAlarm("event", absoluteDisplayAlarm())
        database.insertOccurrence(alarmId, id = 101, triggerAt = TestInstant.minusSeconds(1))

        repository.processDueAlarmOccurrence(101, hasHandled = true, timeZone = TestTimeZone)

        val occurrence = database.queryOccurrenceSnapshots().single()
        assertEquals(DesiredState.INACTIVE, occurrence.desiredState)
        assertEquals(ReconcileResult.CANCELLED, occurrence.lastReconcileResult)
        assertTrue(repository.getDueAlarmOccurrenceIdsAndActions(TestInstant.plusSeconds(60)).isEmpty())
    }

    @Test
    fun processDueOccurrence_createsRepeatsAndStopsAtLimit() = runTest {
        database.insertEvent()
        val alarmId = database.insertAlarm(
            "event",
            absoluteDisplayAlarm(repetition = Repetition(Duration.ofMinutes(5), 2))
        )
        database.insertOccurrence(alarmId, id = 101, index = 0, triggerAt = TestInstant)

        repository.processDueAlarmOccurrence(101, hasHandled = true, timeZone = TestTimeZone)
        val firstRepeat = database.queryOccurrenceSnapshots().single { it.index == 1L }
        assertEquals(TestInstant.plus(Duration.ofMinutes(5)), firstRepeat.triggerAt)
        assertEquals(DesiredState.ACTIVE, firstRepeat.desiredState)

        repository.processDueAlarmOccurrence(firstRepeat.id, hasHandled = true, timeZone = TestTimeZone)
        val lastRepeat = database.queryOccurrenceSnapshots().single { it.index == 2L }
        assertEquals(TestInstant.plus(Duration.ofMinutes(10)), lastRepeat.triggerAt)

        repository.processDueAlarmOccurrence(lastRepeat.id, hasHandled = true, timeZone = TestTimeZone)
        val occurrences = database.queryOccurrenceSnapshots()
        assertEquals(3, occurrences.size)
        assertTrue(occurrences.all { it.desiredState == DesiredState.INACTIVE })
    }

    @Test
    fun reconciliation_splitsCancellationAndSchedulingQueues() = runTest {
        database.insertEvent()
        val alarmId = database.insertAlarm("event", absoluteDisplayAlarm())
        database.insertOccurrence(
            alarmId,
            id = 101,
            desiredState = DesiredState.INACTIVE,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )
        database.insertOccurrence(
            alarmId,
            id = 102,
            desiredState = DesiredState.INACTIVE,
            lastReconcileResult = ReconcileResult.CANCELLED
        )
        database.insertOccurrence(
            alarmId,
            id = 103,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.CANCELLED
        )
        database.insertOccurrence(
            alarmId,
            id = 104,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )

        val result = repository.getAlarmOccurrenceNeedingReconciliation()

        assertEquals(listOf(101L), result.alarmOccurrenceNeedingCancellation.map { it.id })
        assertEquals(listOf(103L), result.alarmOccurrenceNeedingScheduling.map { it.id })
    }

    @Test
    fun reconciliationResultUpdates_targetRowsAndAllRows() = runTest {
        database.insertEvent()
        val alarmId = database.insertAlarm("event", absoluteDisplayAlarm())
        database.insertOccurrence(
            alarmId,
            id = 101,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )
        database.insertOccurrence(
            alarmId,
            id = 102,
            lastReconcileResult = ReconcileResult.UNKNOWN
        )

        repository.markAlarmOccurrenceAsUnknown(101)
        repository.markAlarmOccurrenceAsScheduled(102)
        var occurrences = database.queryOccurrenceSnapshots().associateBy { it.id }
        assertEquals(ReconcileResult.UNKNOWN, occurrences.getValue(101).lastReconcileResult)
        assertEquals(ReconcileResult.SCHEDULED, occurrences.getValue(102).lastReconcileResult)

        repository.markAlarmOccurrenceAsCancelled(101)
        repository.markAllAlarmOccurrencesAsCancelled()
        occurrences = database.queryOccurrenceSnapshots().associateBy { it.id }
        assertTrue(occurrences.values.all { it.lastReconcileResult == ReconcileResult.CANCELLED })
    }

    @Test
    fun processDueOccurrence_handlesUnlinkedRowWithoutCreatingAnother() = runTest {
        database.insertOccurrence(
            alarmId = null,
            id = 101,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )

        repository.processDueAlarmOccurrence(101, hasHandled = true, timeZone = TestTimeZone)

        val occurrence = database.queryOccurrenceSnapshots().single()
        assertNull(occurrence.alarmId)
        assertEquals(DesiredState.INACTIVE, occurrence.desiredState)
        assertEquals(ReconcileResult.CANCELLED, occurrence.lastReconcileResult)
    }

    @Test
    fun rebuildAllDayOccurrences_replacesOnlyActiveRelativeAllDayRows() = runTest {
        database.insertEvent(allDayEvent("all-day-relative"))
        database.insertEvent(allDayEvent("all-day-absolute"))
        database.insertEvent(timedEvent("timed-relative"))
        val relativeAllDayAlarm = database.insertAlarm(
            "all-day-relative",
            relativeDisplayAlarm()
        )
        val absoluteAllDayAlarm = database.insertAlarm(
            "all-day-absolute",
            absoluteDisplayAlarm()
        )
        val relativeTimedAlarm = database.insertAlarm(
            "timed-relative",
            relativeDisplayAlarm()
        )
        database.insertOccurrence(relativeAllDayAlarm, id = 101, triggerAt = TestInstant)
        database.insertOccurrence(absoluteAllDayAlarm, id = 102, triggerAt = TestInstant)
        database.insertOccurrence(relativeTimedAlarm, id = 103, triggerAt = TestInstant)
        val rebuiltTimeZone = ZoneId.of("Pacific/Kiritimati")

        repository.rebuildAllDayAlarmOccurrences(rebuiltTimeZone)

        val occurrences = database.queryOccurrenceSnapshots()
        val oldRelative = occurrences.single { it.id == 101L }
        assertNull(oldRelative.alarmId)
        assertEquals(DesiredState.INACTIVE, oldRelative.desiredState)
        val rebuilt = occurrences.single { it.alarmId == relativeAllDayAlarm }
        assertEquals(0L, rebuilt.index)
        assertEquals(TestDate.atStartOfDay(rebuiltTimeZone).toInstant(), rebuilt.triggerAt)
        assertEquals(DesiredState.ACTIVE, rebuilt.desiredState)
        assertEquals(absoluteAllDayAlarm, occurrences.single { it.id == 102L }.alarmId)
        assertEquals(relativeTimedAlarm, occurrences.single { it.id == 103L }.alarmId)
    }
}
