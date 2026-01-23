package net.k74n3xz.ecal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.k74n3xz.ecal.data.converter.Converters
import net.k74n3xz.ecal.data.dao.AlarmDao
import net.k74n3xz.ecal.data.dao.EventDao
import net.k74n3xz.ecal.data.dao.EventWithAlarmsDao
import net.k74n3xz.ecal.data.entity.Alarm
import net.k74n3xz.ecal.data.entity.Event

@Database(entities = [Event::class, Alarm::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder<AppDatabase>(context, "ECal.db").build()
                    .also { INSTANCE = it }
            }
        }
    }

    abstract fun eventDao(): EventDao
    abstract fun alarmDao(): AlarmDao
    abstract fun eventWithAlarmsDao(): EventWithAlarmsDao
}