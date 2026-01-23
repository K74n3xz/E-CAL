package net.k74n3xz.ecal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.data.entity.Event
import java.time.Instant

@Dao
interface EventDao {
    @Insert
    fun insert(event: Event)

    @Insert
    fun insertAll(vararg event: Event)

    @Upsert
    fun upsert(event: Event)

    @Upsert
    fun upsertAll(vararg event: Event)

    @Delete
    fun delete(event: Event)

    @Query("SELECT * FROM event WHERE (endAt ISNULL AND startAt BETWEEN :left AND :right) OR NOT (startAt > :right OR endAt < :left)")
    fun getEventOverlappingInCloseRange(left: Instant, right: Instant): Flow<Array<Event>>
}