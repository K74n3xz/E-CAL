package net.k74n3xz.ecal.core.application

import net.k74n3xz.ecal.core.application.impl.service.AttachmentQueryServiceImpl
import net.k74n3xz.ecal.core.application.impl.service.AttendeeQueryServiceImpl
import net.k74n3xz.ecal.core.application.impl.service.EventQueryServiceImpl
import net.k74n3xz.ecal.core.application.impl.usecase.AddAttachmentUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.CleanupRemovingAttachmentUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.DeleteAttachmentUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.DeleteAttendeeUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.DeleteEventUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.HandleDueAlarmsUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.RebuildAllDayAlarmOccurrencesUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.ReconcileAlarmOccurrencesUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.SaveAttendeeUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.SaveEventUseCaseImpl
import net.k74n3xz.ecal.core.application.impl.usecase.UpdateAttachmentUseCaseImpl
import net.k74n3xz.ecal.core.application.port.inbound.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.inbound.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.inbound.service.EventQueryService
import net.k74n3xz.ecal.core.application.port.inbound.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.CleanupRemovingAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.RebuildAllDayAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.ReconcileAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.SaveAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.UpdateAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmScheduler
import net.k74n3xz.ecal.core.application.port.outbound.platform.AttachmentCleanupScheduler
import net.k74n3xz.ecal.core.application.port.outbound.platform.AudioPlayer
import net.k74n3xz.ecal.core.application.port.outbound.platform.EmailSender
import net.k74n3xz.ecal.core.application.port.outbound.platform.NotificationPublisher
import net.k74n3xz.ecal.core.application.port.outbound.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.application.port.outbound.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttachmentRepository
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttendeeRepository
import net.k74n3xz.ecal.core.application.port.outbound.repository.EventRepository

object ApplicationFactory {
    /* Query Service */
    fun createEventQueryService(eventRepository: EventRepository): EventQueryService =
        EventQueryServiceImpl(eventRepository)

    fun createAttachmentQueryService(attachmentRepository: AttachmentRepository): AttachmentQueryService =
        AttachmentQueryServiceImpl(attachmentRepository)

    fun createAttendeeQueryService(attendeeRepository: AttendeeRepository): AttendeeQueryService =
        AttendeeQueryServiceImpl(attendeeRepository)

    /* Use Case */
    fun createAddAttachmentUseCase(attachmentRepository: AttachmentRepository): AddAttachmentUseCase =
        AddAttachmentUseCaseImpl(attachmentRepository)

    fun createUpdateAttachmentUseCase(attachmentRepository: AttachmentRepository): UpdateAttachmentUseCase =
        UpdateAttachmentUseCaseImpl(attachmentRepository)

    fun createDeleteAttachmentUseCase(
        attachmentRepository: AttachmentRepository,
        attachmentCleanupScheduler: AttachmentCleanupScheduler
    ): DeleteAttachmentUseCase = DeleteAttachmentUseCaseImpl(attachmentRepository, attachmentCleanupScheduler)

    fun createCleanupRemovingAttachmentUseCase(
        attachmentRepository: AttachmentRepository
    ): CleanupRemovingAttachmentUseCase = CleanupRemovingAttachmentUseCaseImpl(attachmentRepository)

    fun createSaveAttendeeUseCase(attendeeRepository: AttendeeRepository): SaveAttendeeUseCase =
        SaveAttendeeUseCaseImpl(attendeeRepository)

    fun createDeleteAttendeeUseCase(
        attendeeRepository: AttendeeRepository,
        alarmReconciler: AlarmReconciler
    ): DeleteAttendeeUseCase = DeleteAttendeeUseCaseImpl(attendeeRepository, alarmReconciler)

    fun createSaveEventUseCase(
        eventRepository: EventRepository,
        alarmReconciler: AlarmReconciler,
        timeZoneProvider: TimeZoneProvider
    ): SaveEventUseCase = SaveEventUseCaseImpl(eventRepository, alarmReconciler, timeZoneProvider)

    fun createDeleteEventUseCase(
        eventRepository: EventRepository,
        alarmReconciler: AlarmReconciler
    ): DeleteEventUseCase = DeleteEventUseCaseImpl(eventRepository, alarmReconciler)

    fun createReconcileAlarmOccurrencesUseCase(
        alarmRepository: AlarmRepository,
        alarmScheduler: AlarmScheduler
    ): ReconcileAlarmOccurrencesUseCase = ReconcileAlarmOccurrencesUseCaseImpl(alarmRepository, alarmScheduler)

    fun createHandleDueAlarmsUseCase(
        alarmRepository: AlarmRepository,
        attachmentRepository: AttachmentRepository,
        timeZoneProvider: TimeZoneProvider,
        alarmReconciler: AlarmReconciler,
        audioPlayer: AudioPlayer,
        notificationPublisher: NotificationPublisher,
        emailSender: EmailSender
    ): HandleDueAlarmsUseCase = HandleDueAlarmsUseCaseImpl(
        alarmRepository,
        attachmentRepository,
        timeZoneProvider,
        alarmReconciler,
        audioPlayer,
        notificationPublisher,
        emailSender
    )

    fun createRebuildAllDayAlarmOccurrencesUseCase(
        alarmRepository: AlarmRepository,
        alarmReconciler: AlarmReconciler
    ): RebuildAllDayAlarmOccurrencesUseCase = RebuildAllDayAlarmOccurrencesUseCaseImpl(alarmRepository, alarmReconciler)
}
