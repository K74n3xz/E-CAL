package net.k74n3xz.ecal.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.attachment.FileState

@Dao
internal interface AttachmentDao {
    @Insert
    suspend fun insert(vararg attachmentEntities: AttachmentEntity): LongArray

    @Update
    suspend fun update(vararg attachmentEntities: AttachmentEntity)

    @Query("UPDATE attachment SET description = :description WHERE id = :id")
    suspend fun updateDescriptionById(id: Long, description: String?)

    @Query("UPDATE attachment SET state = :fileState WHERE id = :id")
    suspend fun updateFileStateById(id: Long, fileState: FileState)

    @Upsert
    suspend fun upsert(vararg attachmentEntities: AttachmentEntity)

    @Delete
    suspend fun delete(vararg attachmentEntities: AttachmentEntity)

    @Query("DELETE FROM attachment WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM attachment WHERE id = :id")
    suspend fun queryById(id: Long): AttachmentEntity?

    @Query("SELECT * FROM attachment WHERE state IN (:fileStates)")
    suspend fun queryByFileState(vararg fileStates: FileState): List<AttachmentEntity>

    @Query("SELECT id FROM attachment WHERE _rowid_ = :rowId")
    suspend fun queryIdByRowId(rowId: Long): Long?

    @Query("SELECT * FROM attachment WHERE state = :fileState")
    fun observeAttachmentsByFileState(fileState: FileState): Flow<List<AttachmentEntity>>

    @Query("""SELECT * FROM attachment WHERE state = :fileState AND mimeType LIKE :topLevelType || "/%"""")
    fun observeAttachmentsByFileStateAndMimeTopLevelType(
        fileState: FileState,
        topLevelType: String
    ): Flow<List<AttachmentEntity>>
}
