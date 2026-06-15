package net.k74n3xz.ecal.data.calendar.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmComponentDao
import net.k74n3xz.ecal.data.calendar.model.Alarm
import net.k74n3xz.ecal.data.calendar.utils.toAlarm
import net.k74n3xz.ecal.data.calendar.utils.toAlarmComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(private val alarmComponentDao: AlarmComponentDao) {
    fun getAlarmForEventByEventUid(eventUid: String): Flow<List<Alarm>> =
        alarmComponentDao
            .queryAlarmComponentForEventByEventUid(eventUid)
            .map { it.map { alarmComponent -> alarmComponent.toAlarm() } }

    fun replaceAlarms(oldAlarms: List<Alarm>, newAlarms: List<Alarm>) {
        // TODO: Run the alarm replacement as a single database transaction.
        val originalIcsMap = mapOf(
            *newAlarms
                .mapNotNull { alarm ->
                    alarm.id?.let { id ->
                        alarmComponentDao.queryAlarmComponentById(id)?.let { alarmComponent ->
                            id to alarmComponent.rawIcs
                        }
                    }
                }
                .toTypedArray()
        )
        oldAlarms.forEach { alarmComponentDao.delete(it.toAlarmComponent()) }
        alarmComponentDao.insertAll(
            *newAlarms
                .map { it.toAlarmComponent(originalIcsMap[it.id]) }
                .toTypedArray()
        )
    }
}