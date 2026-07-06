package net.k74n3xz.ecal.core.database.utils

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.PropertyList
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Related
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.Created
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.Priority
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Repeat
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Transp
import net.fortuna.ical4j.model.property.Trigger
import net.fortuna.ical4j.model.property.Uid
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmComponent
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmInstance
import net.k74n3xz.ecal.core.database.calendar.entity.EventComponent
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.AlarmOccurrence
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.enumeration.alarm.Action
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerType
import java.io.StringReader
import java.time.Duration
import java.time.Instant

// TODO: Replace this prototype PRODID with the final stable product identifier before release.
private const val PROD_ID: String = "-//K74n3xz//E·CAL prototype//ZH-CN"

internal fun EventComponent.toEvent(alarms: List<Alarm>): Event = Event(
    uid,
    createdAt,
    updatedAt,
    summary,
    description,
    location,
    startAt,
    isAllDayEvent,
    endAt,
    priority,
    transparency,
    recurrenceRule,
    status,
    alarms
)

internal fun Event.toEventComponent(originalIcs: String? = null): EventComponent {
    val rawIcs: String = if (originalIcs == null) {
        Calendar()
            .withDefaults()
            .withProdId(PROD_ID)
            .withComponent(
                VEvent(
                    PropertyList(
                        listOfNotNull(
                            Uid(uid),
                            Created(createdAt),
                            DtStamp(updatedAt),
                            Summary(summary),
                            Description(description),
                            Location(location),
                            DtStart(
                                ParameterList(listOf(if (isAllDayEvent) Value.DATE else Value.DATE_TIME)),
                                startAt
                            ),
                            DtEnd(
                                ParameterList(listOf(if (isAllDayEvent) Value.DATE else Value.DATE_TIME)),
                                endAt
                            ),
                            priority?.let { Priority(it) },
                            transparency?.let { Transp(it.name) },
                            recurrenceRule?.let { RRule<Instant>(it) },
                            status?.let { Status(it.name) }
                        )
                    )
                )
            )
            .fluentTarget
            .toString()
    } else {
        val calendar = CalendarBuilder().build(StringReader(originalIcs))
            .also {
                val compList = it.getComponents<CalendarComponent>()
                if (compList.size != 1 || compList[0].name != CalendarComponent.VEVENT) {
                    throw IllegalArgumentException("Unrecognized ICS content.")
                }
            }
            .withDefaults()
            .withProdId(PROD_ID)
            .fluentTarget

        val event = calendar.getComponents<VEvent>()[0]
            .withProperty(Uid(uid))
            .withProperty(Created(createdAt))
            .withProperty(DtStamp(updatedAt))
            .withProperty(Summary(summary))
            .withProperty(Description(description))
            .withProperty(Location(location))
            .withProperty(
                DtStart(
                    ParameterList(listOf(if (isAllDayEvent) Value.DATE else Value.DATE_TIME)),
                    startAt
                )
            )
            .withProperty(
                DtEnd(
                    ParameterList(listOf(if (isAllDayEvent) Value.DATE else Value.DATE_TIME)),
                    endAt
                )
            )
            .let {
                if (priority == null) {
                    it
                } else {
                    it.withProperty(Priority(priority!!))
                }
            }
            .let {
                if (transparency == null) {
                    it
                } else {
                    it.withProperty(Transp(transparency!!.name))
                }
            }
            .let {
                if (recurrenceRule == null) {
                    it
                } else {
                    it.withProperty(RRule<Instant>(recurrenceRule))
                }
            }
            .let {
                if (status == null) {
                    it
                } else {
                    it.withProperty(Status(status!!.name))
                }
            }
            .fluentTarget as VEvent

        calendar.replace<Calendar>(event).toString()
    }

    return EventComponent(
        uid,
        createdAt,
        updatedAt,
        summary,
        description,
        location,
        startAt,
        isAllDayEvent,
        endAt,
        priority,
        transparency,
        recurrenceRule,
        status,
        rawIcs
    )
}

internal fun AlarmComponent.toAlarm(): Alarm = Alarm(
    id,
    when (action) {
        Action.DISPLAY -> Alarm.Action.Display(
            description
                ?: throw IllegalArgumentException("When the action is \"DISPLAY\", the alarm MUST also include a \"DESCRIPTION\" property. (May the instance of AlarmComponent be broken?)")
        )

        else -> TODO()
    },
    when (triggerType) {
        TriggerType.RELATIVE -> Alarm.Trigger.RelativeTrigger(
            triggerRelativeTo
                ?: throw IllegalArgumentException("Neither `triggerRelativeTo` nor `triggerOffset` can be null for a relative alarm. (May the instance of AlarmComponent be broken?)"),
            triggerOffset
                ?: throw IllegalArgumentException("Neither `triggerRelativeTo` nor `triggerOffset` can be null for a relative alarm. (May the instance of AlarmComponent be broken?)")
        )

        TriggerType.ABSOLUTE -> Alarm.Trigger.AbsoluteTrigger(
            triggerAt
                ?: throw IllegalArgumentException("`triggerAt` cannot be null for an absolute alarm. (May the instance of AlarmComponent be broken?)")
        )
    },
    if (interval == null && repeat == null) null
    else if (interval != null && repeat != null) Alarm.Repetition(interval, repeat)
    else throw IllegalArgumentException("\"`interval` and `repeat` must be assigned values simultaneously or neither must be assigned a value.\"")
)

internal fun Alarm.toAlarmComponent(
    referenceUid: String,
    originalIcs: String? = null
): AlarmComponent {
    val actionType: Action
    val description: String?
    val summary: String?

    val triggerType: TriggerType
    val triggerRelativeTo: TriggerRelationship?
    val triggerOffset: Duration?
    val triggerAt: Instant?

    val triggerProp: Trigger

    when (action) {
        is Alarm.Action.Audio -> {
            TODO()
        }

        is Alarm.Action.Display -> {
            actionType = Action.DISPLAY
            description = (action as Alarm.Action.Display).description
            summary = null
        }

        is Alarm.Action.Email -> {
            TODO()
        }
    }

    when (trigger) {
        is Alarm.Trigger.RelativeTrigger -> {
            triggerType = TriggerType.RELATIVE
            triggerRelativeTo = (trigger as Alarm.Trigger.RelativeTrigger).relativeTo
            triggerOffset = (trigger as Alarm.Trigger.RelativeTrigger).offset
            triggerAt = null

            triggerProp = Trigger(
                ParameterList(listOf(Value.DURATION, Related(triggerRelativeTo.name))),
                triggerOffset
            )
        }

        is Alarm.Trigger.AbsoluteTrigger -> {
            triggerType = TriggerType.ABSOLUTE
            triggerRelativeTo = null
            triggerOffset = null
            triggerAt = (trigger as Alarm.Trigger.AbsoluteTrigger).at

            triggerProp = Trigger(ParameterList(listOf(Value.DATE)), triggerAt)
        }
    }

    val rawIcs: String = if (originalIcs == null) {
        Calendar()
            .withDefaults()
            .withProdId(PROD_ID)
            .withComponent(
                VEvent().also {
                    it.add<VEvent>(
                        VAlarm(
                            PropertyList(
                                listOfNotNull(
                                    net.fortuna.ical4j.model.property.Action(actionType.name),
                                    description?.let { Description(description) },
                                    triggerProp,
                                    summary?.let { Summary(summary) },
                                    repetition?.let { alarmRepetition ->
                                        net.fortuna.ical4j.model.property.Duration(alarmRepetition.interval)
                                    },
                                    repetition?.let { alarmRepetition ->
                                        Repeat(alarmRepetition.repeat)
                                    }
                                )
                            )
                        )
                    )
                }
            )
            .fluentTarget
            .toString()
    } else {
        val calendar = CalendarBuilder().build(StringReader(originalIcs))
            .also {
                val compList = it.getComponents<CalendarComponent>()
                if (compList.size != 1 || compList[0].name != CalendarComponent.VEVENT || (compList[0] as VEvent).alarms.size != 1) {
                    throw IllegalArgumentException("Unrecognized ICS content.")
                }
            }
            .withDefaults()
            .withProdId(PROD_ID)
            .fluentTarget
        val event = calendar.getComponents<VEvent>()[0]
        val alarm = event.alarms[0]
            .withProperty(net.fortuna.ical4j.model.property.Action(actionType.name))
            .let {
                if (description == null) {
                    it
                } else {
                    it.withProperty(Description(description))
                }
            }
            .withProperty(triggerProp)
            .let {
                if (summary == null) {
                    it
                } else {
                    it.withProperty(Summary(summary))
                }
            }
            .let {
                if (repetition == null) {
                    it
                } else {
                    it.withProperty(net.fortuna.ical4j.model.property.Duration(repetition!!.interval))
                        .withProperty(Repeat(repetition!!.repeat))
                }
            }
            .fluentTarget as VAlarm

        calendar.replace<Calendar>(event.replace<VEvent>(alarm)).toString()
    }

    return AlarmComponent(
        id,
        referenceUid,
        actionType,
        description,
        triggerType,
        triggerRelativeTo,
        triggerOffset,
        triggerAt,
        summary,
        repetition?.interval,
        repetition?.repeat,
        rawIcs
    )
}

internal fun AlarmInstance.toAlarmOccurrence(): AlarmOccurrence = AlarmOccurrence(
    id!!,
    alarmComponentId,
    triggerAt
)