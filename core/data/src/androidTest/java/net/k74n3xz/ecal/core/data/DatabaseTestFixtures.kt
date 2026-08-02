package net.k74n3xz.ecal.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.entity.AlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.data.database.entity.enumeration.attachment.FileState
import net.k74n3xz.ecal.core.data.utils.toAlarmEntity
import net.k74n3xz.ecal.core.data.utils.toEventEntity
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventTiming

internal val TestInstant: Instant = Instant.parse("2026-07-04T12:00:00Z")
internal val TestDate: LocalDate = LocalDate.of(2026, 7, 4)
internal val TestTimeZone: ZoneOffset = ZoneOffset.UTC

internal fun createInMemoryDatabase(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

internal fun timedEvent(
    uid: String = "event",
    schedule: EventTiming.Timed = EventTiming.Timed.RangeTiming(
        TestInstant,
        TestInstant.plus(Duration.ofHours(1))
    ),
    alarms: List<Alarm> = emptyList()
): Event = Event(
    uid = uid,
    createdAt = TestInstant.minus(Duration.ofDays(1)),
    updatedAt = TestInstant,
    summary = uid,
    description = "Description for $uid",
    location = "Room $uid",
    schedule = schedule,
    priority = 5,
    alarms = alarms
)

internal fun allDayEvent(
    uid: String = "all-day",
    schedule: EventTiming.AllDay = EventTiming.AllDay.SingleDateTiming(TestDate),
    alarms: List<Alarm> = emptyList()
): Event = Event(
    uid = uid,
    createdAt = TestInstant.minus(Duration.ofDays(1)),
    updatedAt = TestInstant,
    summary = uid,
    schedule = schedule,
    alarms = alarms
)

internal fun absoluteDisplayAlarm(
    id: Long? = null,
    description: String = "Reminder",
    at: Instant = TestInstant,
    repetition: Repetition? = null
): Alarm = Alarm(
    id = id,
    action = Action.Display(description),
    trigger = Trigger.AbsoluteTrigger(at),
    repetition = repetition
)

internal fun relativeDisplayAlarm(
    id: Long? = null,
    description: String = "Reminder",
    relativeTo: TriggerRelationship = TriggerRelationship.START,
    offset: Duration = Duration.ZERO,
    repetition: Repetition? = null
): Alarm = Alarm(
    id = id,
    action = Action.Display(description),
    trigger = Trigger.RelativeTrigger(relativeTo, offset),
    repetition = repetition
)

internal suspend fun AppDatabase.insertEvent(event: Event = timedEvent()) {
    eventDao().insert(event.toEventEntity())
}

internal suspend fun AppDatabase.insertAlarm(eventUid: String, alarm: Alarm): Long =
    alarmDao().insert(alarm.toAlarmEntity(eventUid)).single()

internal suspend fun AppDatabase.insertOccurrence(
    alarmId: Long?,
    id: Long? = null,
    index: Long = 0,
    triggerAt: Instant = TestInstant,
    desiredState: DesiredState = DesiredState.ACTIVE,
    lastReconcileResult: ReconcileResult = ReconcileResult.CANCELLED
) {
    alarmOccurrenceDao().insert(
        AlarmOccurrenceEntity(
            id = id,
            alarmId = alarmId,
            index = index,
            triggerAt = triggerAt,
            desiredState = desiredState,
            lastReconcileResult = lastReconcileResult
        )
    )
}

internal suspend fun AppDatabase.insertAttachment(
    id: Long,
    description: String? = null,
    state: FileState = FileState.OK,
    relativePath: String = "Attachments/attachment-$id",
    name: String = "attachment-$id.txt",
    mimeType: String = "text/plain",
    sizeBytes: Long = id
) {
    attachmentDao().insert(
        AttachmentEntity(
            id = id,
            description = description,
            name = name,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            relativePath = relativePath,
            platformToken = "content://fixture/$id",
            state = state
        )
    )
}

internal suspend fun AppDatabase.insertAttendee(attendee: Attendee) {
    attendeeDao().insert(
        net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity(
            attendee.id,
            attendee.name,
            attendee.description,
            attendee.email
        )
    )
}

internal fun AppDatabase.linkAttachment(alarmId: Long, attachmentId: Long) {
    openHelper.writableDatabase.execSQL(
        "INSERT INTO x_alarm_attachment(alarmId, attachmentId) VALUES (?, ?)",
        arrayOf(alarmId, attachmentId)
    )
}

internal fun AppDatabase.linkAttendee(alarmId: Long, attendeeId: Long) {
    openHelper.writableDatabase.execSQL(
        "INSERT INTO x_alarm_attendee(alarmId, attendeeId) VALUES (?, ?)",
        arrayOf(alarmId, attendeeId)
    )
}

internal fun AppDatabase.queryAlarmAttachmentLinks(): List<Pair<Long, Long>> {
    val cursor = query(
        "SELECT alarmId, attachmentId FROM x_alarm_attachment ORDER BY alarmId, attachmentId",
        emptyArray()
    )
    return cursor.use {
        buildList {
            val alarmIdColumn = it.getColumnIndexOrThrow("alarmId")
            val attachmentIdColumn = it.getColumnIndexOrThrow("attachmentId")
            while (it.moveToNext()) {
                add(it.getLong(alarmIdColumn) to it.getLong(attachmentIdColumn))
            }
        }
    }
}

internal fun AppDatabase.queryAlarmAttendeeLinks(): List<Pair<Long, Long>> {
    val cursor = query(
        "SELECT alarmId, attendeeId FROM x_alarm_attendee ORDER BY alarmId, attendeeId",
        emptyArray()
    )
    return cursor.use {
        buildList {
            val alarmIdColumn = it.getColumnIndexOrThrow("alarmId")
            val attendeeIdColumn = it.getColumnIndexOrThrow("attendeeId")
            while (it.moveToNext()) {
                add(it.getLong(alarmIdColumn) to it.getLong(attendeeIdColumn))
            }
        }
    }
}

internal data class AlarmOccurrenceSnapshot(
    val id: Long,
    val alarmId: Long?,
    val index: Long,
    val triggerAt: Instant,
    val desiredState: DesiredState,
    val lastReconcileResult: ReconcileResult
)

internal fun AppDatabase.queryOccurrenceSnapshots(): List<AlarmOccurrenceSnapshot> {
    val cursor = query(
        """
        SELECT id, alarmId, `index`, triggerAt, desiredState, lastReconcileResult
        FROM alarm_occurrence
        ORDER BY id
        """.trimIndent(),
        emptyArray()
    )
    return cursor.use {
        buildList {
            val idColumn = it.getColumnIndexOrThrow("id")
            val alarmIdColumn = it.getColumnIndexOrThrow("alarmId")
            val indexColumn = it.getColumnIndexOrThrow("index")
            val triggerAtColumn = it.getColumnIndexOrThrow("triggerAt")
            val desiredStateColumn = it.getColumnIndexOrThrow("desiredState")
            val reconcileResultColumn = it.getColumnIndexOrThrow("lastReconcileResult")
            while (it.moveToNext()) {
                add(
                    AlarmOccurrenceSnapshot(
                        id = it.getLong(idColumn),
                        alarmId = if (it.isNull(alarmIdColumn)) null else it.getLong(alarmIdColumn),
                        index = it.getLong(indexColumn),
                        triggerAt = Instant.parse(it.getString(triggerAtColumn)),
                        desiredState = DesiredState.valueOf(it.getString(desiredStateColumn)),
                        lastReconcileResult = ReconcileResult.valueOf(it.getString(reconcileResultColumn))
                    )
                )
            }
        }
    }
}

internal fun attachmentRootDirectory(context: Context): File = requireNotNull(context.getExternalFilesDir(null))

internal fun attachmentDirectory(context: Context): File = File(attachmentRootDirectory(context), "Attachments")
