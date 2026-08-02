package net.k74n3xz.ecal.core.application.port.out.repository

import java.time.Instant
import java.time.ZoneId
import net.k74n3xz.ecal.core.application.port.out.repository.result.AlarmOccurrenceNeedingReconciliation
import net.k74n3xz.ecal.core.model.property.alarm.Action

interface AlarmRepository {
    suspend fun getAlarmOccurrenceNeedingReconciliation(): AlarmOccurrenceNeedingReconciliation
    suspend fun getDueAlarmOccurrenceIdsAndActions(now: Instant): List<Pair<LongArray, Action>>
    suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long)
    suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long)
    suspend fun markAlarmOccurrenceAsUnknown(alarmOccurrenceId: Long)
    suspend fun markAllAlarmOccurrencesAsCancelled()
    suspend fun processDueAlarmOccurrence(alarmOccurrenceId: Long, hasHandled: Boolean, timeZone: ZoneId)
    suspend fun rebuildAllDayAlarmOccurrences(timeZone: ZoneId)
}
