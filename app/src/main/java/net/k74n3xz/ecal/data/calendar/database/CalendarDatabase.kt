package net.k74n3xz.ecal.data.calendar.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.k74n3xz.ecal.data.calendar.database.converter.Converters
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventComponentDao
import net.k74n3xz.ecal.data.calendar.database.entity.AlarmComponent
import net.k74n3xz.ecal.data.calendar.database.entity.EventComponent

@Database(entities = [EventComponent::class, AlarmComponent::class], version = 1)
@TypeConverters(Converters::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun eventDao(): EventComponentDao
    abstract fun alarmDao(): AlarmComponentDao
}