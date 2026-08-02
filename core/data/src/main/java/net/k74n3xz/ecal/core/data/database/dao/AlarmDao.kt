package net.k74n3xz.ecal.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttachmentCrossRef
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttendeeCrossRef

@Dao
internal interface AlarmDao {
    @Insert
    suspend fun insert(vararg alarmEntities: AlarmEntity): LongArray

    @Insert
    suspend fun insertRelatedAttachmentRecord(vararg alarmAttachmentCrossRefs: AlarmAttachmentCrossRef)

    @Insert
    suspend fun insertRelatedAttendeeRecord(vararg alarmAttendeeCrossRefs: AlarmAttendeeCrossRef)

    @Update
    suspend fun update(vararg alarmEntities: AlarmEntity)

    @Upsert
    suspend fun upsert(vararg alarmEntities: AlarmEntity)

    @Delete
    suspend fun delete(vararg alarmEntities: AlarmEntity)

    @Query("DELETE FROM alarm WHERE id = :alarmIds")
    suspend fun deleteById(vararg alarmIds: Long)

    @Query("DELETE FROM x_alarm_attachment WHERE alarmId = :alarmId")
    suspend fun deleteRelatedAttachmentRecordByAlarmId(alarmId: Long)

    @Query("DELETE FROM x_alarm_attendee WHERE alarmId = :alarmId")
    suspend fun deleteRelatedAttendeeRecordByAlarmId(alarmId: Long)

    @Query("SELECT * FROM alarm WHERE id = :id")
    suspend fun queryById(id: Long): AlarmEntity?

    @Query("SELECT id FROM alarm WHERE _rowid_ = :rowId")
    suspend fun queryIdByRowId(rowId: Long): Long?

    @Query("SELECT id FROM alarm WHERE refUid = :refUid")
    suspend fun queryIdsByRefUid(refUid: String): LongArray

    @Query(
        """
        SELECT DISTINCT alarm.id
        FROM alarm
        JOIN event ON alarm.refUid = event.uid
        JOIN alarm_occurrence ON alarm.id = alarm_occurrence.alarmId
        WHERE event.isAllDayEvent = 1 AND alarm.triggerType = :triggerType AND alarm_occurrence.desiredState = :desiredState
        """
    )
    suspend fun queryAlarmIdsForAllDayEventsByTriggerTypeAndDesiredState(
        triggerType: TriggerType,
        desiredState: DesiredState
    ): LongArray

    @Query(
        """
        SELECT alarmId
        FROM x_alarm_attendee
        WHERE alarmId IN (SELECT alarmId FROM x_alarm_attendee WHERE attendeeId = :attendeeId)
        GROUP BY alarmId
        HAVING COUNT(*) = 1
        """
    )
    suspend fun queryIdsWithOnlyAttendee(attendeeId: Long): LongArray
}
