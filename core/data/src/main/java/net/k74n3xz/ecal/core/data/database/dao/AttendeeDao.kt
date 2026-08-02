package net.k74n3xz.ecal.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity

@Dao
interface AttendeeDao {
    @Insert
    suspend fun insert(vararg attendeeEntities: AttendeeEntity)

    @Update
    suspend fun update(vararg attendeeEntities: AttendeeEntity)

    @Upsert
    suspend fun upsert(vararg attendeeEntities: AttendeeEntity)

    @Delete
    suspend fun delete(vararg attendeeEntities: AttendeeEntity)

    @Query("DELETE FROM attendee WHERE id = :attendeeId")
    suspend fun deleteById(attendeeId: Long)

    @Query("SELECT * FROM attendee WHERE id = :attendeeId")
    fun queryById(attendeeId: Long): AttendeeEntity?

    @Query("SELECT * FROM attendee")
    fun observeAllAttendees(): Flow<List<AttendeeEntity>>
}
