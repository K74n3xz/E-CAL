package net.k74n3xz.ecal.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.data.database.dao.result.EventEntityWithAlarmEntities
import net.k74n3xz.ecal.core.data.database.entity.EventEntity

@Dao
internal interface EventDao {
    @Insert
    suspend fun insert(vararg eventEntities: EventEntity)

    @Update
    suspend fun update(vararg eventEntities: EventEntity)

    @Upsert
    suspend fun upsert(vararg eventEntities: EventEntity)

    @Delete
    suspend fun delete(vararg eventEntities: EventEntity)

    @Query("DELETE FROM event WHERE uid = :eventUids")
    suspend fun deleteByUid(vararg eventUids: String)

    @Query("SELECT * FROM event WHERE uid = :uid")
    suspend fun queryByUid(uid: String): EventEntity?

    @Transaction
    @Query("SELECT * FROM event WHERE uid = :eventUid")
    suspend fun queryEventWithAlarmsByEventUid(eventUid: String): EventEntityWithAlarmEntities?

    @Transaction
    @Query(
        """
        SELECT *
        FROM event
        WHERE (isAllDayEvent = 0 AND NOT (startAt > :right OR actualEndAt < :left))
            OR (isAllDayEvent = 1 AND NOT (startDate > :rightDate OR actualEndDate < :leftDate))
        """
    )
    fun observeEventWithAlarmsOverlappingIn(
        left: Instant,
        right: Instant,
        leftDate: LocalDate,
        rightDate: LocalDate
    ): Flow<List<EventEntityWithAlarmEntities>>
}
