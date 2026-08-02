package net.k74n3xz.ecal.core.data.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import net.k74n3xz.ecal.core.data.database.dao.result.AlarmWithAttachmentsAndAttendees
import net.k74n3xz.ecal.core.data.database.dao.result.EventEntityWithAlarmEntities
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity
import net.k74n3xz.ecal.core.data.database.entity.EventEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.ActionType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventTiming

internal fun EventEntity.toEvent(alarms: List<Alarm>): Event = Event(
    uid,
    createdAt,
    updatedAt,
    summary,
    description,
    location,
    if (isAllDayEvent) {
        if (endDate == null && period == null) {
            EventTiming.AllDay.SingleDateTiming(requireNotNull(startDate))
        } else if (endDate != null) {
            EventTiming.AllDay.RangeDateTiming(requireNotNull(startDate), endDate)
        } else if (period != null) {
            EventTiming.AllDay.PeriodDateTiming(requireNotNull(startDate), period)
        } else {
            error("Unreachable")
        }
    } else {
        if (endAt == null && duration == null) {
            EventTiming.Timed.InstantTiming(requireNotNull(startAt))
        } else if (endAt != null) {
            EventTiming.Timed.RangeTiming(requireNotNull(startAt), endAt)
        } else if (duration != null) {
            EventTiming.Timed.DurationTiming(requireNotNull(startAt), duration)
        } else {
            error("Unreachable")
        }
    },
    priority,
    transparency,
    recurrenceRule,
    status,
    alarms
)

internal fun Event.toEventEntity(): EventEntity {
    val startAt: Instant?
    val endAt: Instant?
    val duration: Duration?
    val actualEndAt: Instant?

    val startDate: LocalDate?
    val endDate: LocalDate?
    val period: Period?
    val actualEndDate: LocalDate?

    when (val schedule = schedule) {
        is EventTiming.Timed.InstantTiming -> {
            startAt = schedule.at
            endAt = null
            duration = null
            actualEndAt = schedule.timeRange.end

            startDate = null
            endDate = null
            period = null
            actualEndDate = null
        }

        is EventTiming.Timed.RangeTiming -> {
            startAt = schedule.startAt
            endAt = schedule.endAt
            duration = null
            actualEndAt = schedule.timeRange.end

            startDate = null
            endDate = null
            period = null
            actualEndDate = null
        }

        is EventTiming.Timed.DurationTiming -> {
            startAt = schedule.startAt
            endAt = null
            duration = schedule.duration
            actualEndAt = schedule.timeRange.end

            startDate = null
            endDate = null
            period = null
            actualEndDate = null
        }

        is EventTiming.AllDay.SingleDateTiming -> {
            startAt = null
            endAt = null
            duration = null
            actualEndAt = null

            startDate = schedule.date
            endDate = null
            period = null
            actualEndDate = schedule.dateRange.endDate
        }

        is EventTiming.AllDay.RangeDateTiming -> {
            startAt = null
            endAt = null
            duration = null
            actualEndAt = null

            startDate = schedule.startDate
            endDate = schedule.endDate
            period = null
            actualEndDate = schedule.dateRange.endDate
        }

        is EventTiming.AllDay.PeriodDateTiming -> {
            startAt = null
            endAt = null
            duration = null
            actualEndAt = null

            startDate = schedule.startDate
            endDate = null
            period = schedule.period
            actualEndDate = schedule.dateRange.endDate
        }
    }

    return EventEntity(
        uid,
        createdAt,
        updatedAt,
        summary,
        description,
        location,
        schedule is EventTiming.AllDay,
        startAt,
        endAt,
        duration,
        startDate,
        endDate,
        period,
        actualEndAt,
        actualEndDate,
        priority,
        transparency,
        recurrenceRule,
        status
    )
}

internal fun AlarmEntity.toAlarm(action: Action): Alarm = Alarm(
    id,
    action,
    when (triggerType) {
        TriggerType.RELATIVE -> Trigger.RelativeTrigger(
            requireNotNull(triggerRelativeTo) {
                "Neither `triggerRelativeTo` nor `triggerOffset` can be null for a relative alarm. May the record AlarmEntity(id=$id) is broken?"
            },
            requireNotNull(triggerOffset) {
                "Neither `triggerRelativeTo` nor `triggerOffset` can be null for a relative alarm. May the record AlarmEntity(id=$id) is broken?"
            }
        )

        TriggerType.ABSOLUTE -> Trigger.AbsoluteTrigger(
            requireNotNull(triggerAt) {
                "`triggerAt` cannot be null for an absolute alarm. May the record AlarmEntity(id=$id) is broken?"
            }
        )
    },
    if (interval == null && repeat == null) {
        null
    } else if (interval != null && repeat != null) {
        Repetition(interval, repeat)
    } else {
        throw IllegalArgumentException(
            "`interval` and `repeat` must be assigned values simultaneously or neither must be assigned a value. May the record AlarmEntity(id=$id) is broken?"
        )
    }
)

internal fun Alarm.toAlarmEntity(referenceUid: String): AlarmEntity {
    val actionType: ActionType
    val summary: String?
    val description: String?

    val triggerType: TriggerType
    val triggerRelativeTo: TriggerRelationship?
    val triggerOffset: Duration?
    val triggerAt: Instant?

    when (val action = action) {
        is Action.Audio -> {
            actionType = ActionType.AUDIO
            summary = null
            description = null
        }

        is Action.Display -> {
            actionType = ActionType.DISPLAY
            summary = null
            description = action.description
        }

        is Action.Email -> {
            actionType = ActionType.EMAIL
            summary = action.summary
            description = action.description
        }
    }

    when (val trigger = trigger) {
        is Trigger.RelativeTrigger -> {
            triggerType = TriggerType.RELATIVE
            triggerRelativeTo = trigger.relativeTo
            triggerOffset = trigger.offset
            triggerAt = null
        }

        is Trigger.AbsoluteTrigger -> {
            triggerType = TriggerType.ABSOLUTE
            triggerRelativeTo = null
            triggerOffset = null
            triggerAt = trigger.at
        }
    }

    return AlarmEntity(
        id,
        referenceUid,
        actionType,
        summary,
        description,
        triggerType,
        triggerRelativeTo,
        triggerOffset,
        triggerAt,
        repetition?.interval,
        repetition?.repeat
    )
}

internal fun AlarmOccurrenceEntity.toAlarmOccurrence(): AlarmOccurrence = AlarmOccurrence(
    id!!,
    alarmId,
    triggerAt
)

internal fun AttachmentEntity.toAttachment(): Attachment = Attachment(
    id,
    description,
    name,
    mimeType,
    sizeBytes
)

internal fun AttendeeEntity.toAttendee(): Attendee = Attendee(
    id,
    name,
    description,
    email
)

internal fun Attendee.toAttendeeEntity(): AttendeeEntity = AttendeeEntity(
    id,
    name,
    description,
    email
)

internal fun EventEntityWithAlarmEntities.toEvent(): Event = eventEntity.toEvent(alarmEntities.map { it.toAlarm() })

internal fun AlarmWithAttachmentsAndAttendees.toAlarm(): Alarm = alarmEntity.toAlarm(
    when (alarmEntity.action) {
        ActionType.AUDIO -> {
            require(attachmentEntities.size <= 1) {
                "When the action is `AUDIO`, the alarm can't also include more than one `ATTCH` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
            }
            require(attendeeEntities.isEmpty()) {
                "When the action is `AUDIO`, the alarm must not include `ATTENDEE` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
            }

            Action.Audio(attachmentEntities.firstOrNull()?.toAttachment())
        }

        ActionType.DISPLAY -> {
            require(attachmentEntities.isEmpty()) {
                "When the action is `DISPLAY`, the alarm can't also include more than one `ATTCH` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
            }
            require(attendeeEntities.isEmpty()) {
                "When the action is `DISPLAY`, the alarm must not include `ATTENDEE` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
            }

            Action.Display(
                requireNotNull(alarmEntity.description) {
                    "When the action is `DISPLAY`, the alarm must also include a `DESCRIPTION` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
                }
            )
        }

        ActionType.EMAIL -> {
            require(attendeeEntities.isNotEmpty()) {
                "When the action is `EMAIL`, the alarm must include one or more `ATTENDEE` properties. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
            }

            Action.Email(
                requireNotNull(alarmEntity.description) {
                    "When the action is `EMAIL`, the alarm must also include a `DESCRIPTION` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
                },
                requireNotNull(alarmEntity.summary) {
                    "When the action is `EMAIL`, the alarm must also include a `SUMMARY` property. May the record AlarmEntity(id=${alarmEntity.id}) is broken?"
                },
                attendeeEntities.map { attendeeEntity -> attendeeEntity.toAttendee() },
                if (attachmentEntities.isEmpty()) {
                    null
                } else {
                    attachmentEntities.map { attachmentEntity -> attachmentEntity.toAttachment() }
                }
            )
        }
    }
)
