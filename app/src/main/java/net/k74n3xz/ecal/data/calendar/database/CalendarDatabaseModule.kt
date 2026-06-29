package net.k74n3xz.ecal.data.calendar.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmDao
import net.k74n3xz.ecal.data.calendar.database.dao.AlarmInstanceDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventComponentDao
import net.k74n3xz.ecal.data.calendar.database.dao.EventDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): CalendarDatabase {
        return Room
            .databaseBuilder(
                context = context.applicationContext,
                klass = CalendarDatabase::class.java,
                name = "calendar.db"
            )
            .build()
    }

    @Provides
    fun provideEventComponentDao(db: CalendarDatabase): EventComponentDao {
        return db.eventComponentDao()
    }

    @Provides
    fun provideAlarmComponentDao(db: CalendarDatabase): AlarmComponentDao {
        return db.alarmComponentDao()
    }

    @Provides
    fun provideAlarmInstanceDao(db: CalendarDatabase): AlarmInstanceDao {
        return db.alarmInstanceDao()
    }

    @Provides
    fun provideEventDao(db: CalendarDatabase): EventDao {
        return db.eventDao()
    }

    @Provides
    fun provideAlarmDao(db: CalendarDatabase): AlarmDao {
        return db.alarmDao()
    }
}