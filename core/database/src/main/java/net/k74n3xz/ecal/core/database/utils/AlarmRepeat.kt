package net.k74n3xz.ecal.core.database.utils

import java.time.Duration
import java.time.Instant

internal fun calculateNextAlarmTrigger(
    firstTriggerAt: Instant,
    interval: Duration,
    instancesCount: Long,
    repeat: Int
): Instant? =
    if (instancesCount - 1 < repeat) {
        firstTriggerAt.plus(interval.multipliedBy(instancesCount))
    } else {
        null
    }