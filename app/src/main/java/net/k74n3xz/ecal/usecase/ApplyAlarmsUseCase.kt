package net.k74n3xz.ecal.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.android.components.service.AlarmReconciliationService
import net.k74n3xz.ecal.data.calendar.model.Alarm
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplyAlarmsUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(referenceUid: String, alarms: List<Alarm>) {
        val intent = Intent(context.applicationContext, AlarmReconciliationService::class.java)

        alarmRepository.applyAlarmsForReference(referenceUid, alarms)
        context.applicationContext.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }
    }
}