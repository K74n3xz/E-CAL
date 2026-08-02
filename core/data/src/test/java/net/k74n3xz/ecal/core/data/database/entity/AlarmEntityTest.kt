package net.k74n3xz.ecal.core.data.database.entity

import java.time.Duration
import java.time.Instant
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.ActionType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import org.junit.Assert.assertThrows
import org.junit.Test

class AlarmEntityTest {
    @Test
    fun audioAction_rejectsDescriptionAndSummary() {
        assertThrows(IllegalArgumentException::class.java) {
            alarm(action = ActionType.AUDIO, description = "Not allowed")
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(action = ActionType.AUDIO, summary = "Not allowed")
        }
    }

    @Test
    fun displayAction_requiresDescriptionAndRejectsSummary() {
        assertThrows(IllegalArgumentException::class.java) {
            alarm(action = ActionType.DISPLAY, description = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(action = ActionType.DISPLAY, summary = "Not allowed")
        }
    }

    @Test
    fun emailAction_requiresDescriptionAndSummary() {
        assertThrows(IllegalArgumentException::class.java) {
            alarm(action = ActionType.EMAIL, description = null, summary = "Subject")
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(action = ActionType.EMAIL, description = "Body", summary = null)
        }
    }

    @Test
    fun relativeTrigger_requiresRelationshipAndOffsetAndRejectsAbsoluteTime() {
        assertThrows(IllegalArgumentException::class.java) {
            alarm(triggerRelativeTo = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(triggerOffset = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(triggerAt = Instant.EPOCH)
        }
    }

    @Test
    fun absoluteTrigger_requiresTimeAndRejectsRelativeFields() {
        assertThrows(IllegalArgumentException::class.java) {
            alarm(
                triggerType = TriggerType.ABSOLUTE,
                triggerRelativeTo = null,
                triggerOffset = null,
                triggerAt = null
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(triggerType = TriggerType.ABSOLUTE, triggerAt = Instant.EPOCH)
        }
    }

    @Test
    fun repetition_requiresCompletePairPositiveIntervalAndNonNegativeRepeat() {
        listOf(Duration.ofMinutes(5) to null, null to 2).forEach { (interval, repeat) ->
            assertThrows(IllegalArgumentException::class.java) {
                alarm(interval = interval, repeat = repeat)
            }
        }
        listOf(Duration.ZERO, Duration.ofMinutes(-1)).forEach { interval ->
            assertThrows(IllegalArgumentException::class.java) {
                alarm(interval = interval, repeat = 1)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            alarm(interval = Duration.ofMinutes(1), repeat = -1)
        }
    }

    private fun alarm(
        action: ActionType = ActionType.DISPLAY,
        summary: String? = if (action == ActionType.EMAIL) "Subject" else null,
        description: String? = if (action == ActionType.AUDIO) null else "Reminder",
        triggerType: TriggerType = TriggerType.RELATIVE,
        triggerRelativeTo: TriggerRelationship? = TriggerRelationship.START,
        triggerOffset: Duration? = Duration.ZERO,
        triggerAt: Instant? = null,
        interval: Duration? = null,
        repeat: Int? = null
    ) = AlarmEntity(
        id = 1,
        refUid = "event-1",
        action = action,
        summary = summary,
        description = description,
        triggerType = triggerType,
        triggerRelativeTo = triggerRelativeTo,
        triggerOffset = triggerOffset,
        triggerAt = triggerAt,
        interval = interval,
        repeat = repeat
    )
}
