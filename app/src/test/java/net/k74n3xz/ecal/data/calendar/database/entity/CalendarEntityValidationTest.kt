package net.k74n3xz.ecal.data.calendar.database.entity

import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarmcomponent.Action
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarmcomponent.TriggerRelationship
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.alarmcomponent.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CalendarEntityValidationTest {
    @Test
    fun eventComponent_acceptsPriorityBoundsAndRejectsOutsideValues() {
        assertEquals(0, eventComponent(priority = 0).priority)
        assertEquals(9, eventComponent(priority = 9).priority)

        listOf(-1, 10).forEach { priority ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                eventComponent(priority = priority)
            }
            assertEquals("The priority must be specified in the range 0 to 9.", error.message)
        }
    }

    @Test
    fun displayAlarm_requiresDescription() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            alarmComponent(description = null)
        }

        assertEquals(
            "When the action is \"DISPLAY\", the alarm MUST also include a \"DESCRIPTION\" property.",
            error.message
        )
    }

    @Test
    fun relativeAlarm_requiresRelationshipAndOffsetAndRejectsAbsoluteTime() {
        listOf(
            Triple(null, Duration.ZERO, null),
            Triple(TriggerRelationship.START, null, null)
        ).forEach { (relationship, offset, at) ->
            assertThrows(IllegalArgumentException::class.java) {
                alarmComponent(
                    triggerRelativeTo = relationship,
                    triggerOffset = offset,
                    triggerAt = at
                )
            }
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            alarmComponent(triggerAt = Instant.EPOCH)
        }
        assertEquals("`triggerAt` must be null for a relative alarm.", error.message)
    }

    @Test
    fun absoluteAlarm_requiresTimeAndRejectsRelativeFields() {
        assertThrows(IllegalArgumentException::class.java) {
            alarmComponent(
                triggerType = TriggerType.ABSOLUTE,
                triggerRelativeTo = null,
                triggerOffset = null,
                triggerAt = null
            )
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            alarmComponent(triggerType = TriggerType.ABSOLUTE, triggerAt = Instant.EPOCH)
        }
        assertEquals(
            "Both `triggerRelativeTo` and `triggerOffset` must be null for an absolute alarm.",
            error.message
        )
    }

    @Test
    fun displayAlarm_rejectsSummaryAndIncompleteRepeatPair() {
        val summaryError = assertThrows(IllegalArgumentException::class.java) {
            alarmComponent(summary = "Email subject")
        }
        assertEquals(
            "Only if the action is \"EMAIL\", the alarm can include a \"SUMMARY\" property.",
            summaryError.message
        )

        listOf(Duration.ofMinutes(5) to null, null to 2).forEach { (interval, repeat) ->
            assertThrows(IllegalArgumentException::class.java) {
                alarmComponent(interval = interval, repeat = repeat)
            }
        }
    }

    private fun eventComponent(priority: Int?) = EventComponent(
        uid = "event-1",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        summary = null,
        description = null,
        location = null,
        startAt = Instant.EPOCH,
        endAt = null,
        priority = priority,
        transparency = null,
        recurrenceRule = null,
        status = null,
        rawIcs = ""
    )

    private fun alarmComponent(
        description: String? = "Reminder",
        triggerType: TriggerType = TriggerType.RELATIVE,
        triggerRelativeTo: TriggerRelationship? = TriggerRelationship.START,
        triggerOffset: Duration? = Duration.ZERO,
        triggerAt: Instant? = null,
        summary: String? = null,
        interval: Duration? = null,
        repeat: Int? = null
    ) = AlarmComponent(
        id = 1,
        refUid = "event-1",
        action = Action.DISPLAY,
        description = description,
        triggerType = triggerType,
        triggerRelativeTo = triggerRelativeTo,
        triggerOffset = triggerOffset,
        triggerAt = triggerAt,
        summary = summary,
        interval = interval,
        repeat = repeat,
        rawIcs = ""
    )
}