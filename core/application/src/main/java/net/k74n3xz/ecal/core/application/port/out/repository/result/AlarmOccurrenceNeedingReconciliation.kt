package net.k74n3xz.ecal.core.application.port.out.repository.result

import net.k74n3xz.ecal.core.model.AlarmOccurrence

data class AlarmOccurrenceNeedingReconciliation(
    val alarmOccurrenceNeedingCancellation: List<AlarmOccurrence>,
    val alarmOccurrenceNeedingScheduling: List<AlarmOccurrence>
)
