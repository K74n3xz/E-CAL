package net.k74n3xz.ecal.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.k74n3xz.ecal.core.data.database.entity.enumeration.attachment.FileState

@Entity(tableName = "attachment")
data class AttachmentEntity(
    @PrimaryKey val id: Long?,
    val description: String?,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val relativePath: String,
    val platformToken: String,
    val state: FileState
)
