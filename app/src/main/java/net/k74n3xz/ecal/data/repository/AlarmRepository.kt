package net.k74n3xz.ecal.data.repository

import net.k74n3xz.ecal.data.AppDatabase
import net.k74n3xz.ecal.data.dao.AlarmDao
import net.k74n3xz.ecal.data.entity.Alarm
import net.k74n3xz.ecal.data.entity.Event

class AlarmRepository private constructor(private val alarmDao: AlarmDao) {
    companion object {
        @Volatile
        private var INSTANCE: AlarmRepository? = null

        fun getRepository(database: AppDatabase): AlarmRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmRepository(database.alarmDao()).also { INSTANCE = it }
            }
        }
    }

    suspend fun getAlarmForEvent(event: Event): Array<Alarm> = alarmDao.getAlarmForEvent(event.uid)
}