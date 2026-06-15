package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.data.calendar.database.entity.EventComponent
import java.time.Instant

@Dao
interface EventComponentDao {
    @Insert
    fun insert(event: EventComponent)

    @Insert
    fun insertAll(vararg event: EventComponent)

    @Upsert
    fun upsert(event: EventComponent)

    @Upsert
    fun upsertAll(vararg event: EventComponent)

    @Delete
    fun delete(event: EventComponent)

    @Query("SELECT * FROM event_component WHERE uid = :uid")
    fun queryEventComponentByUid(uid: String): EventComponent?

    @Query("SELECT * FROM event_component WHERE (endAt ISNULL AND startAt BETWEEN :left AND :right) OR NOT (startAt > :right OR endAt < :left)")
    fun queryEventComponentOverlappingInCloseRange(
        left: Instant,
        right: Instant
    ): Flow<List<EventComponent>>
}