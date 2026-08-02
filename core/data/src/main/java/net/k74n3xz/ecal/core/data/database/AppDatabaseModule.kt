package net.k74n3xz.ecal.core.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.k74n3xz.ecal.core.data.database.dao.AlarmDao
import net.k74n3xz.ecal.core.data.database.dao.AlarmOccurrenceDao
import net.k74n3xz.ecal.core.data.database.dao.AttachmentDao
import net.k74n3xz.ecal.core.data.database.dao.AttendeeDao
import net.k74n3xz.ecal.core.data.database.dao.EventDao

@Module
@InstallIn(SingletonComponent::class)
internal object AppDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = Room
        .databaseBuilder(
            context = context.applicationContext,
            klass = AppDatabase::class.java,
            name = "database.db"
        )
        .build()

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideAlarmOccurrenceDao(db: AppDatabase): AlarmOccurrenceDao = db.alarmOccurrenceDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun provideAttendeeDao(db: AppDatabase): AttendeeDao = db.attendeeDao()
}
