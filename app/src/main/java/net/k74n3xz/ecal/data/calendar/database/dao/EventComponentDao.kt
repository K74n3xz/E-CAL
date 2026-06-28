package net.k74n3xz.ecal.data.calendar.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.data.calendar.database.entity.EventComponent
import java.time.Instant

@Dao
interface EventComponentDao {
    @Insert
    fun insert(vararg eventComponents: EventComponent)

    @Update
    fun update(vararg eventComponents: EventComponent)

    @Upsert
    fun upsert(vararg eventComponents: EventComponent)

    @Delete
    fun delete(vararg eventComponents: EventComponent)

    @Query("DELETE FROM event_component WHERE uid = :eventComponentIds")
    fun deleteByUid(vararg eventComponentIds: String)

    @Query("SELECT * FROM event_component WHERE uid = :uid")
    fun queryEventComponentByUid(uid: String): EventComponent?

    @Query("SELECT * FROM event_component WHERE (endAt ISNULL AND startAt BETWEEN :left AND :right) OR NOT (startAt > :right OR endAt < :left)")
    fun queryEventComponentOverlappingInCloseRange(
        left: Instant,
        right: Instant
    ): Flow<List<EventComponent>>
}