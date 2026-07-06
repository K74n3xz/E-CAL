package net.k74n3xz.ecal.core.model

import java.time.Instant

data class AlarmOccurrence(
    val id: Long,
    val alarmId: Long?,
    val triggerAt: Instant
)