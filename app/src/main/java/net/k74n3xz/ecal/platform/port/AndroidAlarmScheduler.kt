package net.k74n3xz.ecal.platform.port

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmScheduler
import net.k74n3xz.ecal.platform.android.helper.alarm.AlarmHelper

@Singleton
internal class AndroidAlarmScheduler @Inject constructor(private val alarmHelper: AlarmHelper) : AlarmScheduler {
    override fun schedule(id: Long, triggerAt: Instant) {
        alarmHelper.schedule(id, triggerAt.toEpochMilli())
    }

    override fun cancel(id: Long) {
        alarmHelper.cancel(id)
    }
}
