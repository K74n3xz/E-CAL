package net.k74n3xz.ecal

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.k74n3xz.ecal.core.application.ApplicationFactory
import net.k74n3xz.ecal.core.application.port.`in`.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.`in`.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.`in`.service.EventQueryService
import net.k74n3xz.ecal.core.application.port.`in`.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.CleanupRemovingAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.RebuildAllDayAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.ReconcileAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.UpdateAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.out.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.out.platform.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.out.platform.AttachmentCleanupScheduler
import net.k74n3xz.ecal.core.application.port.out.platform.AudioPlayer
import net.k74n3xz.ecal.core.application.port.out.platform.EmailSender
import net.k74n3xz.ecal.core.application.port.out.platform.NotificationPublisher
import net.k74n3xz.ecal.core.application.port.out.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.application.port.out.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttendeeRepository
import net.k74n3xz.ecal.core.application.port.out.repository.EventRepository
import net.k74n3xz.ecal.platform.port.AndroidAlarmReconciler
import net.k74n3xz.ecal.platform.port.AndroidAlarmScheduler
import net.k74n3xz.ecal.platform.port.AndroidAttachmentCleanupScheduler
import net.k74n3xz.ecal.platform.port.AndroidAudioPlayer
import net.k74n3xz.ecal.platform.port.AndroidEmailSender
import net.k74n3xz.ecal.platform.port.AndroidNotificationPublisher
import net.k74n3xz.ecal.platform.port.PreferenceTimeZoneProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class ECALModule {
    companion object {
        /* Service */
        @Provides
        @Singleton
        internal fun provideEventQueryService(eventRepository: EventRepository): EventQueryService =
            ApplicationFactory.createEventQueryService(eventRepository)

        @Provides
        @Singleton
        internal fun provideAttachmentQueryService(attachmentRepository: AttachmentRepository): AttachmentQueryService =
            ApplicationFactory.createAttachmentQueryService(attachmentRepository)

        @Provides
        @Singleton
        internal fun provideAttendeeQueryService(attendeeRepository: AttendeeRepository): AttendeeQueryService =
            ApplicationFactory.createAttendeeQueryService(attendeeRepository)

        /* Use Case */
        @Provides
        @Singleton
        internal fun provideAddAttachmentUseCase(attachmentRepository: AttachmentRepository): AddAttachmentUseCase =
            ApplicationFactory.createAddAttachmentUseCase(attachmentRepository)

        @Provides
        @Singleton
        internal fun provideUpdateAttachmentUseCase(
            attachmentRepository: AttachmentRepository
        ): UpdateAttachmentUseCase = ApplicationFactory.createUpdateAttachmentUseCase(attachmentRepository)

        @Provides
        @Singleton
        internal fun provideDeleteAttachmentUseCase(
            attachmentRepository: AttachmentRepository,
            attachmentCleanupScheduler: AttachmentCleanupScheduler
        ): DeleteAttachmentUseCase =
            ApplicationFactory.createDeleteAttachmentUseCase(attachmentRepository, attachmentCleanupScheduler)

        @Provides
        @Singleton
        internal fun provideCleanupRemovingAttachmentUseCase(
            attachmentRepository: AttachmentRepository
        ): CleanupRemovingAttachmentUseCase =
            ApplicationFactory.createCleanupRemovingAttachmentUseCase(attachmentRepository)

        @Provides
        @Singleton
        internal fun provideSaveAttendeeUseCase(attendeeRepository: AttendeeRepository): SaveAttendeeUseCase =
            ApplicationFactory.createSaveAttendeeUseCase(attendeeRepository)

        @Provides
        @Singleton
        internal fun provideDeleteAttendeeUseCase(
            attendeeRepository: AttendeeRepository,
            alarmReconciler: AlarmReconciler
        ): DeleteAttendeeUseCase = ApplicationFactory.createDeleteAttendeeUseCase(attendeeRepository, alarmReconciler)

        @Provides
        @Singleton
        fun provideSaveEventUseCase(
            eventRepository: EventRepository,
            alarmReconciler: AlarmReconciler,
            timeZoneProvider: TimeZoneProvider
        ): SaveEventUseCase =
            ApplicationFactory.createSaveEventUseCase(eventRepository, alarmReconciler, timeZoneProvider)

        @Provides
        @Singleton
        fun provideDeleteEventUseCase(
            eventRepository: EventRepository,
            alarmReconciler: AlarmReconciler
        ): DeleteEventUseCase = ApplicationFactory.createDeleteEventUseCase(eventRepository, alarmReconciler)

        @Provides
        @Singleton
        fun provideReconcileAlarmOccurrencesUseCase(
            alarmRepository: AlarmRepository,
            alarmScheduler: AlarmScheduler
        ): ReconcileAlarmOccurrencesUseCase =
            ApplicationFactory.createReconcileAlarmOccurrencesUseCase(alarmRepository, alarmScheduler)

        @Provides
        @Singleton
        fun provideHandleDueAlarmsUseCase(
            alarmRepository: AlarmRepository,
            attachmentRepository: AttachmentRepository,
            timeZoneProvider: TimeZoneProvider,
            alarmReconciler: AlarmReconciler,
            audioPlayer: AudioPlayer,
            notificationPublisher: NotificationPublisher,
            emailSender: EmailSender
        ): HandleDueAlarmsUseCase = ApplicationFactory.createHandleDueAlarmsUseCase(
            alarmRepository,
            attachmentRepository,
            timeZoneProvider,
            alarmReconciler,
            audioPlayer,
            notificationPublisher,
            emailSender
        )

        @Provides
        @Singleton
        fun provideRebuildAllDayAlarmOccurrencesUseCase(
            alarmRepository: AlarmRepository,
            alarmReconciler: AlarmReconciler
        ): RebuildAllDayAlarmOccurrencesUseCase =
            ApplicationFactory.createRebuildAllDayAlarmOccurrencesUseCase(alarmRepository, alarmReconciler)
    }

    @Binds
    internal abstract fun bindAlarmScheduler(androidAlarmScheduler: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    internal abstract fun bindAlarmReconciler(androidAlarmReconciler: AndroidAlarmReconciler): AlarmReconciler

    @Binds
    internal abstract fun bindAudioPlayer(androidAudioPlayer: AndroidAudioPlayer): AudioPlayer

    @Binds
    internal abstract fun bindNotificationPublisher(
        androidNotificationPublisher: AndroidNotificationPublisher
    ): NotificationPublisher

    @Binds
    internal abstract fun bindEmailSender(androidEmailSender: AndroidEmailSender): EmailSender

    @Binds
    internal abstract fun bindAttachmentCleanupScheduler(
        androidAttachmentCleanupScheduler: AndroidAttachmentCleanupScheduler
    ): AttachmentCleanupScheduler

    @Binds
    internal abstract fun bindTimeZoneProvider(preferenceTimeZoneProvider: PreferenceTimeZoneProvider): TimeZoneProvider
}
