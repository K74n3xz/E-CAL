package net.k74n3xz.ecal.application.port

import java.time.Instant

interface AlarmScheduler {
    fun schedule(id: Long, triggerAt: Instant)

    fun cancel(id: Long)
}