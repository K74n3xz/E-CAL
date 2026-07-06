package net.k74n3xz.ecal.core.database.calendar.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import net.k74n3xz.ecal.core.database.calendar.entity.EventComponent

@Dao
internal interface EventComponentDao {
    @Insert
    suspend fun insert(vararg eventComponents: EventComponent)

    @Update
    suspend fun update(vararg eventComponents: EventComponent)

    @Upsert
    suspend fun upsert(vararg eventComponents: EventComponent)

    @Delete
    suspend fun delete(vararg eventComponents: EventComponent)

    @Query("DELETE FROM event_component WHERE uid = :eventComponentIds")
    suspend fun deleteByUid(vararg eventComponentIds: String)

    @Query("SELECT * FROM event_component WHERE uid = :uid")
    suspend fun queryByUid(uid: String): EventComponent?

    @Query("SELECT rawIcs FROM event_component WHERE uid = :uid")
    suspend fun queryRawIcsByUid(uid: String): String?
}