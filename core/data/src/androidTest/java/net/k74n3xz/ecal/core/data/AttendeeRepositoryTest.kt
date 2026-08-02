package net.k74n3xz.ecal.core.data

import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.data.repository.AttendeeRepositoryImpl
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AttendeeRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: AttendeeRepositoryImpl

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        repository = AttendeeRepositoryImpl(
            database,
            database.attendeeDao(),
            database.alarmDao(),
            database.alarmOccurrenceDao()
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveAndGet_upsertsAttendee() = runTest {
        val attendee = attendee()
        repository.saveAttendee(attendee)
        assertEquals(attendee, repository.getAttendeeById(8))

        val updated = attendee.copy(name = "Updated", description = null)
        repository.saveAttendee(updated)
        assertEquals(updated, repository.getAttendeeById(8))
        assertNull(repository.getAttendeeById(999))
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAllAttendees_emitsUpdatesAndDeleteRemovesRow() = runTest {
        repository.saveAttendee(attendee())
        val emissions = mutableListOf<List<String?>>()
        val firstEmission = CompletableDeferred<Unit>()
        val secondEmission = CompletableDeferred<Unit>()
        val thirdEmission = CompletableDeferred<Unit>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeAllAttendees().take(3).collect { attendees ->
                emissions += attendees.map { it.name }
                when (emissions.size) {
                    1 -> firstEmission.complete(Unit)
                    2 -> secondEmission.complete(Unit)
                    else -> thirdEmission.complete(Unit)
                }
            }
        }

        firstEmission.await()
        repository.saveAttendee(attendee().copy(name = "Updated"))
        secondEmission.await()
        repository.deleteAttendeeById(8)
        thirdEmission.await()
        collection.join()

        assertEquals(
            listOf(listOf("Alex"), listOf("Updated"), emptyList()),
            emissions
        )
        assertNull(repository.getAttendeeById(8))
    }

    @Test
    fun deleteAttendee_deletesAlarmsLeftWithoutAttendeesAndPreservesSharedAlarms() = runTest {
        val target = attendee()
        val other = Attendee(id = 9, name = "Blake", email = "blake@example.com")
        database.insertAttendee(target)
        database.insertAttendee(other)
        database.insertEvent()
        val soleAttendeeAlarmId = database.insertAlarm("event", emailAlarm(target))
        val sharedAlarmId = database.insertAlarm("event", emailAlarm(target, other))
        database.linkAttendee(soleAttendeeAlarmId, requireNotNull(target.id))
        database.linkAttendee(sharedAlarmId, requireNotNull(target.id))
        database.linkAttendee(sharedAlarmId, requireNotNull(other.id))
        database.insertOccurrence(
            alarmId = soleAttendeeAlarmId,
            id = 101,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )
        database.insertOccurrence(
            alarmId = sharedAlarmId,
            id = 102,
            lastReconcileResult = ReconcileResult.SCHEDULED
        )

        repository.deleteAttendeeById(requireNotNull(target.id))

        assertNull(database.alarmDao().queryById(soleAttendeeAlarmId))
        assertNotNull(database.alarmDao().queryById(sharedAlarmId))
        assertEquals(listOf(sharedAlarmId to requireNotNull(other.id)), database.queryAlarmAttendeeLinks())
        assertEquals(
            listOf(
                AlarmOccurrenceSnapshot(
                    id = 101,
                    alarmId = null,
                    index = 0,
                    triggerAt = TestInstant,
                    desiredState = DesiredState.INACTIVE,
                    lastReconcileResult = ReconcileResult.SCHEDULED
                ),
                AlarmOccurrenceSnapshot(
                    id = 102,
                    alarmId = sharedAlarmId,
                    index = 0,
                    triggerAt = TestInstant,
                    desiredState = DesiredState.ACTIVE,
                    lastReconcileResult = ReconcileResult.SCHEDULED
                )
            ),
            database.queryOccurrenceSnapshots()
        )
        assertNull(repository.getAttendeeById(requireNotNull(target.id)))
        assertEquals(other, repository.getAttendeeById(requireNotNull(other.id)))
    }

    private fun attendee() = Attendee(
        id = 8,
        name = "Alex",
        description = "Required",
        email = "alex@example.com"
    )

    private fun emailAlarm(vararg attendees: Attendee) = Alarm(
        action = Action.Email(
            description = "Email reminder",
            summary = "Reminder",
            attendee = attendees.toList()
        ),
        trigger = Trigger.RelativeTrigger(offset = Duration.ZERO)
    )
}
