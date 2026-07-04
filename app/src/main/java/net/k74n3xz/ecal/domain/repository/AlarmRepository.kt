package net.k74n3xz.ecal.domain.repository

import net.k74n3xz.ecal.domain.model.Alarm
import net.k74n3xz.ecal.domain.model.AlarmOccurrence
import java.time.Instant

interface AlarmRepository {
    suspend fun getDueAlarmOccurrenceIdsAndActions(triggerAt: Instant): List<Pair<LongArray, Alarm.Action>>

    suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long)

    suspend fun getAlarmOccurrenceNeedingReconciliation(): Pair<List<AlarmOccurrence>, List<AlarmOccurrence>>

    suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long)

    suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long)

    suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long)

    suspend fun markAllAlarmOccurrencesAsCancelled()
}