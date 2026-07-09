package net.k74n3xz.ecal.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import java.time.Instant
import net.k74n3xz.ecal.core.database.calendar.CalendarDatabase
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmComponent
import net.k74n3xz.ecal.core.database.calendar.entity.AlarmInstance
import net.k74n3xz.ecal.core.database.calendar.entity.EventComponent
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.DesiredState
import net.k74n3xz.ecal.core.database.calendar.entity.enumeration.alarminstance.ReconcileResult
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.enumeration.alarm.Action
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerType

internal val TestInstant: Instant = Instant.parse("2026-07-04T12:00:00Z")

internal fun createInMemoryCalendarDatabase(): CalendarDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, CalendarDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

internal fun eventModel(
    uid: String = "event",
    startAt: Instant = TestInstant,
    endAt: Instant? = TestInstant.plus(Duration.ofHours(1)),
    alarms: List<Alarm> = emptyList()
): Event = Event(
    uid = uid,
    createdAt = TestInstant.minus(Duration.ofDays(1)),
    updatedAt = TestInstant,
    summary = uid,
    description = "Description for $uid",
    location = "Room $uid",
    startAt = startAt,
    endAt = endAt,
    priority = 5,
    alarms = alarms
)

internal fun absoluteDisplayAlarm(
    id: Long? = null,
    description: String = "Reminder",
    at: Instant = TestInstant,
    repetition: Alarm.Repetition? = null
): Alarm = Alarm(
    id = id,
    action = Alarm.Action.Display(description),
    trigger = Alarm.Trigger.AbsoluteTrigger(at),
    repetition = repetition
)

internal fun relativeDisplayAlarm(
    id: Long? = null,
    description: String = "Reminder",
    relativeTo: TriggerRelationship = TriggerRelationship.START,
    offset: Duration = Duration.ZERO,
    repetition: Alarm.Repetition? = null
): Alarm = Alarm(
    id = id,
    action = Alarm.Action.Display(description),
    trigger = Alarm.Trigger.RelativeTrigger(relativeTo, offset),
    repetition = repetition
)

internal suspend fun CalendarDatabase.insertEventComponent(
    uid: String,
    startAt: Instant = TestInstant,
    endAt: Instant? = TestInstant.plus(Duration.ofHours(1))
) {
    eventComponentDao()
        .insert(
            EventComponent(
                uid = uid,
                createdAt = TestInstant.minus(Duration.ofDays(1)),
                updatedAt = TestInstant,
                summary = uid,
                description = null,
                location = null,
                startAt = startAt,
                isAllDayEvent = false,
                endAt = endAt,
                priority = null,
                transparency = null,
                recurrenceRule = null,
                status = null,
                rawIcs = ""
            )
        )
}

internal suspend fun CalendarDatabase.insertAlarmComponent(
    eventUid: String,
    id: Long? = null,
    description: String = "Reminder",
    triggerAt: Instant = TestInstant,
    interval: Duration? = null,
    repeat: Int? = null
): Long = alarmComponentDao()
    .insert(
        AlarmComponent(
            id = id,
            refUid = eventUid,
            action = Action.DISPLAY,
            description = description,
            triggerType = TriggerType.ABSOLUTE,
            triggerRelativeTo = null,
            triggerOffset = null,
            triggerAt = triggerAt,
            summary = null,
            interval = interval,
            repeat = repeat,
            rawIcs = ""
        )
    )
    .single()

internal suspend fun CalendarDatabase.insertAlarmInstance(
    alarmId: Long?,
    id: Long? = null,
    triggerAt: Instant = TestInstant,
    desiredState: DesiredState = DesiredState.ACTIVE,
    lastReconcileResult: ReconcileResult = ReconcileResult.SCHEDULED
) {
    alarmInstanceDao()
        .insert(
            AlarmInstance(
                id = id,
                alarmComponentId = alarmId,
                triggerAt = triggerAt,
                desiredState = desiredState,
                lastReconcileResult = lastReconcileResult
            )
        )
}

internal data class AlarmInstanceSnapshot(
    val id: Long,
    val alarmComponentId: Long?,
    val triggerAt: Instant,
    val desiredState: DesiredState,
    val lastReconcileResult: ReconcileResult
)

internal fun CalendarDatabase.queryAlarmInstanceSnapshots(): List<AlarmInstanceSnapshot> {
    val cursor = query(
        """
        SELECT id, alarmComponentId, triggerAt, desiredState, lastReconcileResult
        FROM alarm_instance
        ORDER BY id
        """.trimIndent(),
        emptyArray()
    )
    return cursor.use {
        buildList {
            val idColumn = it.getColumnIndexOrThrow("id")
            val alarmComponentIdColumn = it.getColumnIndexOrThrow("alarmComponentId")
            val triggerAtColumn = it.getColumnIndexOrThrow("triggerAt")
            val desiredStateColumn = it.getColumnIndexOrThrow("desiredState")
            val lastReconcileResultColumn = it.getColumnIndexOrThrow("lastReconcileResult")
            while (it.moveToNext()) {
                add(
                    AlarmInstanceSnapshot(
                        id = it.getLong(idColumn),
                        alarmComponentId = if (it.isNull(alarmComponentIdColumn)) {
                            null
                        } else {
                            it.getLong(alarmComponentIdColumn)
                        },
                        triggerAt = Instant.parse(it.getString(triggerAtColumn)),
                        desiredState = DesiredState.valueOf(it.getString(desiredStateColumn)),
                        lastReconcileResult = ReconcileResult.valueOf(
                            it.getString(lastReconcileResultColumn)
                        )
                    )
                )
            }
        }
    }
}
