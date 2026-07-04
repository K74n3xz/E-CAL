package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.data.calendar.database.relation.EventComponentWithAlarmComponents
import java.time.Instant

@Dao
interface EventDao {
    @Transaction
    @Query("SELECT * FROM event_component WHERE uid = :eventComponentUid")
    suspend fun queryEventComponentWithAlarmComponentsByEventComponentUid(eventComponentUid: String): EventComponentWithAlarmComponents?

    @Transaction
    @Query("SELECT * FROM event_component WHERE (endAt ISNULL AND startAt BETWEEN :left AND :right) OR NOT (startAt > :right OR endAt < :left)")
    fun observeEventComponentWithAlarmComponentsOverlappingInCloseRange(
        left: Instant,
        right: Instant
    ): Flow<List<EventComponentWithAlarmComponents>>
}