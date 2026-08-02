package net.k74n3xz.ecal.core.data

import java.time.Duration
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.data.repository.EventRepositoryImpl
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: EventRepositoryImpl

    @Before
    fun setUp() {
        database = createInMemoryDatabase()
        repository = EventRepositoryImpl(
            database,
            database.eventDao(),
            database.alarmDao(),
            database.alarmOccurrenceDao()
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveEvent_roundTripsEventAndCreatesAbsoluteOccurrence() = runTest {
        val triggerAt = TestInstant.plus(Duration.ofMinutes(30))
        repository.saveEvent(
            timedEvent(
                alarms = listOf(absoluteDisplayAlarm(description = "absolute", at = triggerAt))
            ),
            TestTimeZone
        )

        val saved = requireNotNull(repository.getEventByUid("event"))
        val alarm = saved.alarms.single()
        assertEquals("event", saved.uid)
        assertEquals(Action.Display("absolute"), alarm.action)
        assertEquals(Trigger.AbsoluteTrigger(triggerAt), alarm.trigger)
        assertTrue(alarm.id != null)
        assertEquals(
            listOf(
                AlarmOccurrenceSnapshot(
                    id = 1,
                    alarmId = alarm.id,
                    index = 0,
                    triggerAt = triggerAt,
                    desiredState = DesiredState.ACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            ),
            database.queryOccurrenceSnapshots()
        )
    }

    @Test
    fun saveEvent_roundTripsAudioAttachmentAndGeneratedAlarmId() = runTest {
        val attachment = Attachment(7, null, "alarm.mp3", "audio/mpeg", 42)
        database.insertAttachment(
            id = 7,
            name = attachment.name,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes
        )

        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteAudioAlarm(attachment = attachment))),
            TestTimeZone
        )

        val savedAlarm = requireNotNull(repository.getEventByUid("event")).alarms.single()
        assertTrue(savedAlarm.id != null)
        assertEquals(Action.Audio(attachment), savedAlarm.action)
        assertEquals(listOf(requireNotNull(savedAlarm.id) to 7L), database.queryAlarmAttachmentLinks())
    }

    @Test
    fun saveEvent_replacesAndRemovesAudioAttachmentRelation() = runTest {
        val first = Attachment(7, null, "first.mp3", "audio/mpeg", 42)
        val second = Attachment(8, null, "second.wav", "audio/wav", 84)
        listOf(first, second).forEach { attachment ->
            database.insertAttachment(
                id = requireNotNull(attachment.id),
                name = attachment.name,
                mimeType = attachment.mimeType,
                sizeBytes = attachment.sizeBytes
            )
        }
        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteAudioAlarm(attachment = first))),
            TestTimeZone
        )
        val alarmId = requireNotNull(repository.getEventByUid("event")!!.alarms.single().id)

        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteAudioAlarm(id = alarmId, attachment = second))),
            TestTimeZone
        )
        assertEquals(Action.Audio(second), repository.getEventByUid("event")!!.alarms.single().action)
        assertEquals(listOf(alarmId to 8L), database.queryAlarmAttachmentLinks())

        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteAudioAlarm(id = alarmId))),
            TestTimeZone
        )
        assertEquals(Action.Audio(), repository.getEventByUid("event")!!.alarms.single().action)
        assertTrue(database.queryAlarmAttachmentLinks().isEmpty())

        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteDisplayAlarm(id = alarmId))),
            TestTimeZone
        )
        assertTrue(database.queryAlarmAttachmentLinks().isEmpty())
    }

    @Test
    fun saveEvent_roundTripsAndReplacesEmailRelations() = runTest {
        val firstAttendee = Attendee(31, "First", null, "first@example.com")
        val secondAttendee = Attendee(32, "Second", null, "second@example.com")
        val attachment = Attachment(7, null, "agenda.pdf", "application/pdf", 42)
        database.insertAttendee(firstAttendee)
        database.insertAttendee(secondAttendee)
        database.insertAttachment(
            id = 7,
            name = attachment.name,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes
        )
        repository.saveEvent(
            timedEvent(
                alarms = listOf(
                    Alarm(
                        action = Action.Email("Body", "Subject", listOf(firstAttendee), listOf(attachment)),
                        trigger = Trigger.AbsoluteTrigger(TestInstant)
                    )
                )
            ),
            TestTimeZone
        )

        val savedAlarm = requireNotNull(repository.getEventByUid("event")).alarms.single()
        val alarmId = requireNotNull(savedAlarm.id)
        assertEquals(
            Action.Email("Body", "Subject", listOf(firstAttendee), listOf(attachment)),
            savedAlarm.action
        )
        assertEquals(listOf(alarmId to 31L), database.queryAlarmAttendeeLinks())
        assertEquals(listOf(alarmId to 7L), database.queryAlarmAttachmentLinks())

        repository.saveEvent(
            timedEvent(
                alarms = listOf(
                    Alarm(
                        id = alarmId,
                        action = Action.Email("New body", "New subject", listOf(secondAttendee)),
                        trigger = Trigger.AbsoluteTrigger(TestInstant)
                    )
                )
            ),
            TestTimeZone
        )
        assertEquals(
            Action.Email("New body", "New subject", listOf(secondAttendee)),
            repository.getEventByUid("event")!!.alarms.single().action
        )
        assertEquals(listOf(alarmId to 32L), database.queryAlarmAttendeeLinks())
        assertTrue(database.queryAlarmAttachmentLinks().isEmpty())

        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteDisplayAlarm(id = alarmId))),
            TestTimeZone
        )
        assertTrue(database.queryAlarmAttendeeLinks().isEmpty())
        assertTrue(database.queryAlarmAttachmentLinks().isEmpty())
    }

    @Test
    fun saveEvent_createsRelativeOccurrencesFromTimedStartAndEnd() = runTest {
        val start = TestInstant.plus(Duration.ofHours(2))
        val end = start.plus(Duration.ofHours(1))
        repository.saveEvent(
            timedEvent(
                schedule = EventTiming.Timed.RangeTiming(start, end),
                alarms = listOf(
                    relativeDisplayAlarm(
                        description = "start",
                        offset = Duration.ofMinutes(-10)
                    ),
                    relativeDisplayAlarm(
                        description = "end",
                        relativeTo = TriggerRelationship.END,
                        offset = Duration.ofMinutes(5)
                    )
                )
            ),
            TestTimeZone
        )

        assertEquals(
            setOf(start.minus(Duration.ofMinutes(10)), end.plus(Duration.ofMinutes(5))),
            database.queryOccurrenceSnapshots().map { it.triggerAt }.toSet()
        )
    }

    @Test
    fun saveAllDayEvent_usesProvidedTimeZoneForRelativeOccurrence() = runTest {
        val timeZone = ZoneId.of("Pacific/Kiritimati")
        repository.saveEvent(
            allDayEvent(alarms = listOf(relativeDisplayAlarm())),
            timeZone
        )

        assertEquals(
            TestDate.atStartOfDay(timeZone).toInstant(),
            database.queryOccurrenceSnapshots().single().triggerAt
        )
    }

    @Test
    fun saveEvent_updatesKnownAlarmReplacesSetAndRetiresOldOccurrences() = runTest {
        repository.saveEvent(
            timedEvent(
                alarms = listOf(
                    absoluteDisplayAlarm(description = "keep", at = TestInstant.plusSeconds(10)),
                    absoluteDisplayAlarm(description = "remove", at = TestInstant.plusSeconds(20))
                )
            ),
            TestTimeZone
        )
        val originalAlarms = requireNotNull(repository.getEventByUid("event")).alarms
            .associateBy { (it.action as Action.Display).description }
        val keptAlarmId = requireNotNull(originalAlarms.getValue("keep").id)
        val removedAlarmId = requireNotNull(originalAlarms.getValue("remove").id)
        val updatedSchedule = EventTiming.Timed.InstantTiming(TestInstant.plus(Duration.ofDays(1)))

        repository.saveEvent(
            timedEvent(
                schedule = updatedSchedule,
                alarms = listOf(
                    absoluteDisplayAlarm(
                        id = keptAlarmId,
                        description = "updated",
                        at = TestInstant.plusSeconds(30)
                    ),
                    absoluteDisplayAlarm(description = "new", at = TestInstant.plusSeconds(40))
                )
            ),
            TestTimeZone
        )

        val saved = requireNotNull(repository.getEventByUid("event"))
        assertEquals(updatedSchedule, saved.schedule)
        assertEquals(
            setOf("updated", "new"),
            saved.alarms.map { (it.action as Action.Display).description }.toSet()
        )
        val currentAlarmIds = database.alarmDao().queryIdsByRefUid("event").toSet()
        assertTrue(keptAlarmId in currentAlarmIds)
        assertFalse(removedAlarmId in currentAlarmIds)
        val occurrences = database.queryOccurrenceSnapshots()
        assertEquals(4, occurrences.size)
        assertEquals(2, occurrences.count { it.desiredState == DesiredState.ACTIVE })
        assertTrue(
            occurrences.filter { it.desiredState == DesiredState.INACTIVE }
                .all { it.alarmId == null }
        )
    }

    @Test
    fun saveEvent_ignoresUnknownNonNullAlarmId() = runTest {
        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteDisplayAlarm(id = 999))),
            TestTimeZone
        )

        assertTrue(requireNotNull(repository.getEventByUid("event")).alarms.isEmpty())
        assertTrue(database.queryOccurrenceSnapshots().isEmpty())
    }

    @Test
    fun deleteEvent_retiresOccurrencesAndDeletesEventAndAlarms() = runTest {
        repository.saveEvent(
            timedEvent(alarms = listOf(absoluteDisplayAlarm())),
            TestTimeZone
        )

        repository.deleteEventByUid("event")

        assertNull(repository.getEventByUid("event"))
        assertTrue(database.alarmDao().queryIdsByRefUid("event").isEmpty())
        val occurrence = database.queryOccurrenceSnapshots().single()
        assertNull(occurrence.alarmId)
        assertEquals(DesiredState.INACTIVE, occurrence.desiredState)
    }

    @Test
    fun overlappingQuery_filtersTimedEventsAndIncludesTouchingBoundary() = runTest {
        val rangeStart = TestInstant
        val rangeEnd = TestInstant.plus(Duration.ofHours(1))
        database.insertEvent(
            timedEvent("point-inside", EventTiming.Timed.InstantTiming(rangeStart.plusSeconds(30)))
        )
        database.insertEvent(
            timedEvent("point-outside", EventTiming.Timed.InstantTiming(rangeEnd.plusSeconds(1)))
        )
        database.insertEvent(
            timedEvent(
                "touches-left",
                EventTiming.Timed.RangeTiming(rangeStart.minusSeconds(60), rangeStart)
            )
        )
        database.insertEvent(
            timedEvent(
                "spans-range",
                EventTiming.Timed.RangeTiming(rangeStart.minusSeconds(1), rangeEnd.plusSeconds(1))
            )
        )

        val observed = repository.observeEventsOverlappingRange(
            rangeStart.atZone(ZoneOffset.UTC),
            rangeEnd.atZone(ZoneOffset.UTC)
        ).first()

        assertEquals(
            setOf("point-inside", "touches-left", "spans-range"),
            observed.map { it.uid }.toSet()
        )
    }

    @Test
    fun allDayQuery_doesNotDriftAcrossPositiveAndNegativeOffsets() = runTest {
        repository.saveEvent(allDayEvent(), TestTimeZone)

        listOf(ZoneId.of("Pacific/Kiritimati"), ZoneId.of("Pacific/Honolulu")).forEach { timeZone ->
            val events = repository.observeEventsOverlappingRange(
                TestDate.atStartOfDay(timeZone),
                TestDate.plusDays(1).atStartOfDay(timeZone)
            ).first()
            assertEquals(listOf("all-day"), events.map { it.uid })
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun overlappingQuery_emitsAfterSaveAndDelete() = runTest {
        val emissions = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeEventsOverlappingRange(
                TestInstant.minusSeconds(1).atZone(ZoneOffset.UTC),
                TestInstant.plusSeconds(1).atZone(ZoneOffset.UTC)
            ).take(2).toList()
        }

        repository.saveEvent(
            timedEvent(schedule = EventTiming.Timed.InstantTiming(TestInstant)),
            TestTimeZone
        )
        repository.deleteEventByUid("event")

        assertEquals(
            listOf(listOf("event"), emptyList()),
            emissions.await().map { events -> events.map { it.uid } }
        )
    }
}

private fun absoluteAudioAlarm(
    id: Long? = null,
    attachment: Attachment? = null,
    at: java.time.Instant = TestInstant
): Alarm = Alarm(
    id = id,
    action = Action.Audio(attachment),
    trigger = Trigger.AbsoluteTrigger(at)
)
