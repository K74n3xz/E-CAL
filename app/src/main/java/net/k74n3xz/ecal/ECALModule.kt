package net.k74n3xz.ecal

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.k74n3xz.ecal.android.port.AndroidAlarmOccurrenceReconciler
import net.k74n3xz.ecal.android.port.AndroidAlarmScheduler
import net.k74n3xz.ecal.android.port.AndroidNotificationPublisher
import net.k74n3xz.ecal.core.application.port.AlarmOccurrenceReconciler
import net.k74n3xz.ecal.core.application.port.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.NotificationPublisher
import net.k74n3xz.ecal.core.application.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.repository.EventRepository
import net.k74n3xz.ecal.core.application.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.usecase.ReconcileAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.usecase.SaveEventUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ECALModule {
    companion object {
        @Provides
        @Singleton
        fun provideSaveEventUseCase(
            eventRepository: EventRepository,
            alarmOccurrenceReconciler: AlarmOccurrenceReconciler
        ): SaveEventUseCase =
            SaveEventUseCase(eventRepository, alarmOccurrenceReconciler)

        @Provides
        @Singleton
        fun provideDeleteEventUseCase(
            eventRepository: EventRepository,
            alarmOccurrenceReconciler: AlarmOccurrenceReconciler
        ): DeleteEventUseCase =
            DeleteEventUseCase(
                eventRepository,
                alarmOccurrenceReconciler
            )

        @Provides
        @Singleton
        fun provideReconcileAlarmOccurrencesUseCase(
            alarmRepository: AlarmRepository,
            alarmScheduler: AlarmScheduler
        ): ReconcileAlarmOccurrencesUseCase =
            ReconcileAlarmOccurrencesUseCase(
                alarmRepository,
                alarmScheduler
            )

        @Provides
        @Singleton
        fun provideHandleDueAlarmsUseCase(
            alarmRepository: AlarmRepository,
            alarmOccurrenceReconciler: AlarmOccurrenceReconciler,
            notificationPublisher: NotificationPublisher
        ): HandleDueAlarmsUseCase =
            HandleDueAlarmsUseCase(
                alarmRepository,
                alarmOccurrenceReconciler,
                notificationPublisher
            )
    }

    @Binds
    fun bindAlarmScheduler(androidAlarmScheduler: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    fun bindNotificationPublisher(androidNotificationPublisher: AndroidNotificationPublisher): NotificationPublisher

    @Binds
    fun bindAlarmOccurrenceReconciler(androidAlarmOccurrenceReconciler: AndroidAlarmOccurrenceReconciler): AlarmOccurrenceReconciler
}