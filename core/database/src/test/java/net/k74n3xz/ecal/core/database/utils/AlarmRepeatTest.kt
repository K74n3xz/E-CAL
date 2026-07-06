package net.k74n3xz.ecal.core.database.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AlarmRepeatTest {
    private val firstTriggerAt = Instant.parse("2026-06-28T01:00:00Z")
    private val interval = Duration.ofMinutes(5)

    @Test
    fun nextTrigger_advancesByCurrentInstanceCount() {
        assertEquals(
            Instant.parse("2026-06-28T01:05:00Z"),
            calculateNextAlarmTrigger(firstTriggerAt, interval, instancesCount = 1, repeat = 3)
        )
        assertEquals(
            Instant.parse("2026-06-28T01:10:00Z"),
            calculateNextAlarmTrigger(firstTriggerAt, interval, instancesCount = 2, repeat = 3)
        )
        assertEquals(
            Instant.parse("2026-06-28T01:15:00Z"),
            calculateNextAlarmTrigger(firstTriggerAt, interval, instancesCount = 3, repeat = 3)
        )
    }

    @Test
    fun nextTrigger_stopsAfterRepeatCountIsReached() {
        assertNull(
            calculateNextAlarmTrigger(firstTriggerAt, interval, instancesCount = 1, repeat = 0)
        )
        assertNull(
            calculateNextAlarmTrigger(firstTriggerAt, interval, instancesCount = 4, repeat = 3)
        )
    }
}
