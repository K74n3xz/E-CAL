package net.k74n3xz.ecal

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.k74n3xz.ecal.android.port.AndroidAlarmOccurrenceReconciler
import net.k74n3xz.ecal.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.data.calendar.repository.RoomAlarmRepository
import net.k74n3xz.ecal.data.calendar.repository.RoomEventRepository
import net.k74n3xz.ecal.domain.repository.AlarmRepository
import net.k74n3xz.ecal.domain.repository.EventRepository

@Module
@InstallIn(SingletonComponent::class)
interface ECALModule {
    @Binds
    fun bindEventRepository(roomEventRepository: RoomEventRepository): EventRepository

    @Binds
    fun bindAlarmRepository(roomAlarmRepository: RoomAlarmRepository): AlarmRepository

    @Binds
    fun bindAlarmOccurrenceReconciler(androidAlarmOccurrenceReconciler: AndroidAlarmOccurrenceReconciler): AlarmOccurrenceReconciler
}