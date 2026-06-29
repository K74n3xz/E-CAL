package net.k74n3xz.ecal.domain.repository

import net.k74n3xz.ecal.domain.model.AlarmOccurrence

interface AlarmRepository {
    suspend fun getAlarmDescriptionByAlarmOccurrenceId(alarmOccurrenceId: Long): String?

    suspend fun getAlarmOccurrenceNeedingReconciliation(): Pair<List<AlarmOccurrence>, List<AlarmOccurrence>>

    suspend fun markAlarmOccurrenceAsCancelled(alarmOccurrenceId: Long)

    suspend fun markAlarmOccurrenceAsScheduled(alarmOccurrenceId: Long)

    suspend fun markAllAlarmOccurrencesAsCancelled()

    suspend fun handleAlarmOccurrenceRinging(alarmOccurrenceId: Long)
}