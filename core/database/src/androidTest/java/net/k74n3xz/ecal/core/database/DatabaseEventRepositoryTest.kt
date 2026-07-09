package net.k74n3xz.ecal.core.database

import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.database.calendar.CalendarDatabase
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.core.database.repository.DatabaseEventRepository
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatabaseEventRepositoryTest {
    private lateinit var database: CalendarDatabase
    private lateinit var repository: DatabaseEventRepository

    @Before
    fun setUp() {
        database = createInMemoryCalendarDatabase()
        repository = DatabaseEventRepository(
            database,
            database.eventComponentDao(),
            database.alarmComponentDao(),
            database.alarmInstanceDao(),
            database.eventDao()
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveEvent_insertsEventAndCreatesFirstOccurrenceForAbsoluteAlarm() = runTest {
        val triggerAt = TestInstant.plus(Duration.ofMinutes(30))
        val event = eventModel(
            uid = "event",
            alarms = listOf(absoluteDisplayAlarm(description = "absolute", at = triggerAt))
        )

        repository.saveEvent(event)

        val saved = repository.getEventByUid("event") ?: error("Saved event was not found.")
        assertEquals("event", saved.uid)
        assertEquals(listOf("absolute"), saved.alarms.displayAlarmDescriptions())
        val alarm = saved.alarms.single()
        assertTrue(alarm.id != null)
        assertEquals(Alarm.Trigger.AbsoluteTrigger(triggerAt), alarm.trigger)
        assertEquals(
            listOf(
                AlarmInstanceSnapshot(
                    id = 1,
                    alarmComponentId = alarm.id,
                    triggerAt = triggerAt,
                    desiredState = DesiredState.ACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            ),
            database.queryAlarmInstanceSnapshots()
        )
    }

    @Test
    fun saveEvent_createsRelativeOccurrenceFromEventStartAndEnd() = runTest {
        val start = TestInstant.plus(Duration.ofHours(2))
        val end = start.plus(Duration.ofHours(1))
        val event = eventModel(
            uid = "event",
            startAt = start,
            endAt = end,
            alarms = listOf(
                relativeDisplayAlarm(
                    description = "start",
                    relativeTo = TriggerRelationship.START,
                    offset = Duration.ofMinutes(-10)
                ),
                relativeDisplayAlarm(
                    description = "end",
                    relativeTo = TriggerRelationship.END,
                    offset = Duration.ofMinutes(5)
                )
            )
        )

        repository.saveEvent(event)

        val triggerTimes = database.queryAlarmInstanceSnapshots()
            .map { it.triggerAt }
            .toSet()
        assertEquals(
            setOf(start.minus(Duration.ofMinutes(10)), end.plus(Duration.ofMinutes(5))),
            triggerTimes
        )
    }

    @Test
    fun saveEvent_updatesExistingEventAndReplacesAlarmSet() = runTest {
        repository.saveEvent(
            eventModel(
                uid = "event",
                alarms = listOf(
                    absoluteDisplayAlarm(description = "keep", at = TestInstant.plusSeconds(10)),
                    absoluteDisplayAlarm(description = "remove", at = TestInstant.plusSeconds(20))
                )
            )
        )
        val originalAlarms = repository.getEventByUid("event")!!.alarms
            .associateBy { (it.action as Alarm.Action.Display).description }
        val keptAlarmId = originalAlarms.getValue("keep").id!!
        val removedAlarmId = originalAlarms.getValue("remove").id!!

        repository.saveEvent(
            eventModel(
                uid = "event",
                startAt = TestInstant.plus(Duration.ofDays(1)),
                alarms = listOf(
                    absoluteDisplayAlarm(
                        id = keptAlarmId,
                        description = "updated",
                        at = TestInstant.plusSeconds(30)
                    ),
                    absoluteDisplayAlarm(description = "new", at = TestInstant.plusSeconds(40))
                )
            )
        )

        val saved = repository.getEventByUid("event") ?: error("Updated event was not found.")
        assertEquals(TestInstant.plus(Duration.ofDays(1)), saved.startAt)
        assertEquals(setOf("updated", "new"), saved.alarms.displayAlarmDescriptions().toSet())
        val currentAlarmIds = database.alarmComponentDao().queryIdsByRefUid("event").toSet()
        assertTrue(keptAlarmId in currentAlarmIds)
        assertFalse(removedAlarmId in currentAlarmIds)
        val instances = database.queryAlarmInstanceSnapshots()
        assertEquals(4, instances.size)
        assertEquals(2, instances.count { it.desiredState == DesiredState.ACTIVE })
        assertTrue(
            instances
                .filter { it.desiredState == DesiredState.INACTIVE }
                .all { it.alarmComponentId == null }
        )
    }

    @Test
    fun saveEvent_ignoresNewAlarmWithUnknownNonNullId() = runTest {
        repository.saveEvent(
            eventModel(
                uid = "event",
                alarms = listOf(
                    absoluteDisplayAlarm(
                        id = 999,
                        description = "unknown",
                        at = TestInstant
                    )
                )
            )
        )

        val saved = repository.getEventByUid("event") ?: error("Saved event was not found.")
        assertTrue(saved.alarms.isEmpty())
        assertTrue(database.queryAlarmInstanceSnapshots().isEmpty())
    }

    @Test
    fun deleteEventByUid_deactivatesAndUnlinksOccurrencesThenDeletesEventAndAlarms() = runTest {
        repository.saveEvent(
            eventModel(
                uid = "event",
                alarms = listOf(absoluteDisplayAlarm(description = "delete", at = TestInstant))
            )
        )

        repository.deleteEventByUid("event")

        assertNull(repository.getEventByUid("event"))
        assertTrue(database.alarmComponentDao().queryIdsByRefUid("event").isEmpty())
        assertEquals(
            listOf(
                AlarmInstanceSnapshot(
                    id = 1,
                    alarmComponentId = null,
                    triggerAt = TestInstant,
                    desiredState = DesiredState.INACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            ),
            database.queryAlarmInstanceSnapshots()
        )
    }

    @Test
    fun observeEventsOverlappingRange_filtersPointAndIntervalEvents() = runTest {
        val rangeStart = TestInstant
        val rangeEnd = TestInstant.plus(Duration.ofHours(1))
        database.insertEventComponent(
            "point-inside",
            startAt = rangeStart.plus(Duration.ofMinutes(30)),
            endAt = null
        )
        database.insertEventComponent(
            "point-outside",
            startAt = rangeEnd.plus(Duration.ofMinutes(1)),
            endAt = null
        )
        database.insertEventComponent(
            "touches-left-boundary",
            startAt = rangeStart.minus(Duration.ofHours(1)),
            endAt = rangeStart
        )
        database.insertEventComponent(
            "spans-range",
            startAt = rangeStart.minus(Duration.ofMinutes(1)),
            endAt = rangeEnd.plus(Duration.ofMinutes(1))
        )
        database.insertEventComponent(
            "after-range",
            startAt = rangeEnd.plus(Duration.ofSeconds(1)),
            endAt = rangeEnd.plus(Duration.ofHours(1))
        )

        val observed = repository
            .observeEventsOverlappingRange(
                rangeStart.atZone(java.time.ZoneOffset.UTC),
                rangeEnd.atZone(java.time.ZoneOffset.UTC)
            )
            .first()

        assertEquals(
            setOf("point-inside", "touches-left-boundary", "spans-range"),
            observed.map { it.uid }.toSet()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun observeEventsOverlappingRange_emitsAfterSaveAndDelete() = runTest {
        val emissions = async(UnconfinedTestDispatcher(testScheduler)) {
            repository
                .observeEventsOverlappingRange(
                    TestInstant.minusSeconds(1).atZone(java.time.ZoneOffset.UTC),
                    TestInstant.plusSeconds(1).atZone(java.time.ZoneOffset.UTC)
                )
                .take(2)
                .toList()
        }

        repository.saveEvent(eventModel(uid = "event", startAt = TestInstant, endAt = null))
        repository.deleteEventByUid("event")

        assertEquals(
            listOf(listOf("event"), emptyList()),
            emissions.await().map { events -> events.map { it.uid } }
        )
    }

    private fun List<Alarm>.displayAlarmDescriptions(): List<String> = map {
        (it.action as Alarm.Action.Display).description
    }
}
