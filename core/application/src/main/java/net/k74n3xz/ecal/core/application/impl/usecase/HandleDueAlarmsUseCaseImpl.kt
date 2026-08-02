package net.k74n3xz.ecal.core.application.impl.usecase

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.k74n3xz.ecal.core.application.port.`in`.usecase.HandleDueAlarmsUseCase
import net.k74n3xz.ecal.core.application.port.out.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.out.platform.AudioPlayer
import net.k74n3xz.ecal.core.application.port.out.platform.EmailSender
import net.k74n3xz.ecal.core.application.port.out.platform.NotificationPublisher
import net.k74n3xz.ecal.core.application.port.out.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.application.port.out.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.model.property.alarm.Action

internal class HandleDueAlarmsUseCaseImpl(
    private val alarmRepository: AlarmRepository,
    private val attachmentRepository: AttachmentRepository,
    private val timeZoneProvider: TimeZoneProvider,
    private val alarmReconciler: AlarmReconciler,
    private val audioPlayer: AudioPlayer,
    private val notificationPublisher: NotificationPublisher,
    private val emailSender: EmailSender
) : HandleDueAlarmsUseCase {
    private val mutex: Mutex = Mutex()

    override suspend operator fun invoke(now: Instant) = mutex.withLock {
        val dueAlarmOccurrences = alarmRepository.getDueAlarmOccurrenceIdsAndActions(now)
        val timeZone = timeZoneProvider.currentTimeZone()

        dueAlarmOccurrences.forEach { (ids, action) ->
            ids.forEach { id ->
                alarmRepository.processDueAlarmOccurrence(
                    alarmOccurrenceId = id,
                    hasHandled = when (action) {
                        is Action.Audio -> audioPlayer.play(action.attach?.let { requireNotNull(it.id) })

                        is Action.Display -> notificationPublisher.publish(id, action.description)

                        is Action.Email -> emailSender.send(
                            id = id.hashCode(), // TODO: Use collision-free IDs.
                            subject = action.summary,
                            text = action.description,
                            receivers = action.attendee.map { it.email },
                            attachments = action.attach?.let {
                                it.mapNotNull { attachment ->
                                    attachmentRepository.generateAttachmentSharingTokenById(
                                        requireNotNull(attachment.id)
                                    )
                                }
                            } ?: emptyList()
                        )
                    },
                    timeZone = timeZone
                )
            }
        }
        if (dueAlarmOccurrences.any { it.first.isNotEmpty() }) {
            alarmReconciler.request()
        }
    }
}
