package net.k74n3xz.ecal.android.port

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.android.components.service.AlarmReconciliationService
import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmOccurrenceReconciler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AlarmOccurrenceReconciler {
    override fun reconcileAlarmOccurrences() {
        val intent = Intent(context.applicationContext, AlarmReconciliationService::class.java)
        context.applicationContext.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }
    }
}