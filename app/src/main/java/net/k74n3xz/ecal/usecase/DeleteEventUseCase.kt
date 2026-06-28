package net.k74n3xz.ecal.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.android.components.service.AlarmReconciliationService
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import net.k74n3xz.ecal.data.calendar.repository.EventRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val alarmRepository: AlarmRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(eventUid: String) {
        val intent = Intent(context.applicationContext, AlarmReconciliationService::class.java)

        alarmRepository.deleteAlarmsForReference(eventUid)
        eventRepository.deleteEventByUid(eventUid)
        context.applicationContext.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startForegroundService(intent)
            } else {
                it.startService(intent)
            }
        }
    }
}