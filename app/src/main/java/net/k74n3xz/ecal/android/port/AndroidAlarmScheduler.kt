package net.k74n3xz.ecal.android.port

import net.k74n3xz.ecal.android.helper.alarm.AlarmHelper
import net.k74n3xz.ecal.core.application.port.AlarmScheduler
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmScheduler @Inject constructor(private val alarmHelper: AlarmHelper) :
    AlarmScheduler {
    override fun schedule(id: Long, triggerAt: Instant) {
        alarmHelper.schedule(id, triggerAt.toEpochMilli())
    }

    override fun cancel(id: Long) {
        alarmHelper.cancel(id)
    }
}