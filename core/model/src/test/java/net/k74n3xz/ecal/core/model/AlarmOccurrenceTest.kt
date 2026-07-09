package net.k74n3xz.ecal.core.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmOccurrenceTest {
    @Test
    fun alarmOccurrence_preservesFieldsWhenAlarmIdExists() {
        val triggerAt = Instant.parse("2026-07-09T06:00:00Z")

        val occurrence = AlarmOccurrence(id = 1L, alarmId = 2L, triggerAt = triggerAt)

        assertEquals(1L, occurrence.id)
        assertEquals(2L, occurrence.alarmId)
        assertEquals(triggerAt, occurrence.triggerAt)
    }

    @Test
    fun alarmOccurrence_supportsMissingAlarmId() {
        val triggerAt = Instant.parse("2026-07-09T06:30:00Z")

        val occurrence = AlarmOccurrence(id = 3L, alarmId = null, triggerAt = triggerAt)

        assertEquals(3L, occurrence.id)
        assertNull(occurrence.alarmId)
        assertEquals(triggerAt, occurrence.triggerAt)
    }
}
