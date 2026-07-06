package net.k74n3xz.ecal.core.database.calendar

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.k74n3xz.ecal.core.database.calendar.converter.Converters
import net.k74n3xz.ecal.core.database.calendar.dao.AlarmComponentDao
import net.k74n3xz.ecal.core.database.calendar.dao.AlarmDao
import net.k74n3xz.ecal.core.database.calendar.dao.AlarmInstanceDao
import net.k74n3xz.ecal.core.database.calendar.dao.EventComponentDao
import net.k74n3xz.ecal.core.database.calendar.dao.EventDao
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmComponent
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmInstance
import net.k74n3xz.ecal.core.database.calendar.entity.EventComponent

@Database(
    entities = [
        EventComponent::class,
        AlarmComponent::class,
        AlarmInstance::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
internal abstract class CalendarDatabase : RoomDatabase() {
    abstract fun eventComponentDao(): EventComponentDao
    abstract fun alarmComponentDao(): AlarmComponentDao
    abstract fun alarmInstanceDao(): AlarmInstanceDao
    abstract fun eventDao(): EventDao
    abstract fun alarmDao(): AlarmDao
}