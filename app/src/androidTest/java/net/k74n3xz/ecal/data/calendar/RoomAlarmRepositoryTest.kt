package net.k74n3xz.ecal.data.calendar

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.data.calendar.database.CalendarDatabase
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmInstance
import net.k74n3xz.ecal.data.calendar.database.entity.EventComponent
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.data.calendar.repository.RoomAlarmRepository
import net.k74n3xz.ecal.domain.model.Alarm
import net.k74n3xz.ecal.domain.model.enumeration.alarm.Action
import net.k74n3xz.ecal.domain.model.enumeration.alarm.TriggerType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RoomAlarmRepositoryTest {
    private lateinit var database: CalendarDatabase
    private lateinit var repository: RoomAlarmRepository
    private val now = Instant.parse("2026-07-04T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CalendarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomAlarmRepository(
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
    fun dueQuery_joinsEachInstanceToItsOwnComponentAndFiltersStateAndTime() = runTest {
        insertEvent("event-a")
        insertEvent("event-b")
        val alarmA = insertAlarm("event-a", "A")
        val alarmB = insertAlarm("event-b", "B")
        insertInstance(alarmA, now.minusSeconds(1), DesiredState.ACTIVE)
        insertInstance(alarmB, now.minusSeconds(2), DesiredState.ACTIVE)
        insertInstance(alarmA, now.plusSeconds(1), DesiredState.ACTIVE)
        insertInstance(alarmB, now.minusSeconds(3), DesiredState.INACTIVE)

        val due = repository.getDueAlarmOccurrenceIdsAndActions(now)

        assertEquals(2, due.size)
        assertEquals(
            setOf("A", "B"),
            due.map { (it.second as Alarm.Action.Display).description }.toSet()
        )
        assertEquals(2, due.sumOf { it.first.size })
    }

    @Test
    fun processDueOccurrence_marksItInactiveAndDoesNotCreateNextForNonRepeatingAlarm() = runTest {
        insertEvent("event")
        val alarmId = insertAlarm("event", "once")
        insertInstance(alarmId, now.minusSeconds(1), DesiredState.ACTIVE)
        val occurrenceId =
            repository.getDueAlarmOccurrenceIdsAndActions(now).single().first.single()

        repository.processDueAlarmOccurrence(occurrenceId)

        assertTrue(repository.getDueAlarmOccurrenceIdsAndActions(now.plusSeconds(60)).isEmpty())
        val needingReconciliation = repository.getAlarmOccurrenceNeedingReconciliation()
        assertTrue(needingReconciliation.first.isEmpty())
        assertTrue(needingReconciliation.second.isEmpty())
    }

    @Test
    fun processDueOccurrence_createsNextOccurrenceForRepeatingAlarm() = runTest {
        insertEvent("event")
        val alarmId = insertAlarm("event", "repeat", Duration.ofMinutes(5), repeat = 2)
        insertInstance(alarmId, now, DesiredState.ACTIVE)
        val occurrenceId =
            repository.getDueAlarmOccurrenceIdsAndActions(now).single().first.single()

        repository.processDueAlarmOccurrence(occurrenceId)

        assertTrue(repository.getDueAlarmOccurrenceIdsAndActions(now).isEmpty())
        val toSchedule = repository.getAlarmOccurrenceNeedingReconciliation().second
        assertEquals(1, toSchedule.size)
        assertEquals(now.plus(Duration.ofMinutes(5)), toSchedule.single().triggerAt)
    }

    private suspend fun insertEvent(uid: String) {
        database.eventComponentDao()
            .insert(
                EventComponent(
                    uid = uid,
                    createdAt = now,
                    updatedAt = now,
                    summary = uid,
                    description = null,
                    location = null,
                    startAt = now,
                    isAllDayEvent = false,
                    endAt = now.plusSeconds(3600),
                    priority = null,
                    transparency = null,
                    recurrenceRule = null,
                    status = null,
                    rawIcs = ""
                )
            )
    }

    private suspend fun insertAlarm(
        eventUid: String,
        description: String,
        interval: Duration? = null,
        repeat: Int? = null
    ): Long = database.alarmComponentDao()
        .insert(
            AlarmComponent(
                id = null,
                refUid = eventUid,
                action = Action.DISPLAY,
                description = description,
                triggerType = TriggerType.ABSOLUTE,
                triggerRelativeTo = null,
                triggerOffset = null,
                triggerAt = now,
                summary = null,
                interval = interval,
                repeat = repeat,
                rawIcs = ""
            )
        )
        .single()

    private suspend fun insertInstance(
        alarmId: Long,
        triggerAt: Instant,
        desiredState: DesiredState
    ) {
        database.alarmInstanceDao()
            .insert(
                AlarmInstance(
                    id = null,
                    alarmComponentId = alarmId,
                    triggerAt = triggerAt,
                    desiredState = desiredState,
                    lastReconcileResult = ReconcileResult.SCHEDULED
                )
            )
    }
}