package net.k74n3xz.ecal.core.application.port.out.platform

import java.time.Instant

interface AlarmScheduler {
    fun schedule(id: Long, triggerAt: Instant)
    fun cancel(id: Long)
}
