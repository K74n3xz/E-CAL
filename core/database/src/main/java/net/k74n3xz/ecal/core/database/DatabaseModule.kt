package net.k74n3xz.ecal.core.database

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.repository.EventRepository
import net.k74n3xz.ecal.core.database.repository.DatabaseAlarmRepository
import net.k74n3xz.ecal.core.database.repository.DatabaseEventRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    @Binds
    internal abstract fun bindEventRepository(databaseEventRepository: DatabaseEventRepository): EventRepository

    @Binds
    internal abstract fun bindAlarmRepository(databaseAlarmRepository: DatabaseAlarmRepository): AlarmRepository
}