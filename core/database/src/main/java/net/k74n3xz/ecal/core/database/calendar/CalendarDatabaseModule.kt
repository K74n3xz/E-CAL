package net.k74n3xz.ecal.core.database.calendar

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.k74n3xz.ecal.core.database.calendar.dao.AlarmComponentDao
import net.k74n3xz.ecal.core.database.calendar.dao.AlarmDao
import net.k74n3xz.ecal.core.database.calendar.dao.AlarmInstanceDao
import net.k74n3xz.ecal.core.database.calendar.dao.EventComponentDao
import net.k74n3xz.ecal.core.database.calendar.dao.EventDao

@Module
@InstallIn(SingletonComponent::class)
internal object CalendarDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): CalendarDatabase = Room
        .databaseBuilder(
            context = context.applicationContext,
            klass = CalendarDatabase::class.java,
            name = "calendar.db"
        )
        .build()

    @Provides
    fun provideEventComponentDao(db: CalendarDatabase): EventComponentDao = db.eventComponentDao()

    @Provides
    fun provideAlarmComponentDao(db: CalendarDatabase): AlarmComponentDao = db.alarmComponentDao()

    @Provides
    fun provideAlarmInstanceDao(db: CalendarDatabase): AlarmInstanceDao = db.alarmInstanceDao()

    @Provides
    fun provideEventDao(db: CalendarDatabase): EventDao = db.eventDao()

    @Provides
    fun provideAlarmDao(db: CalendarDatabase): AlarmDao = db.alarmDao()
}
