package net.k74n3xz.ecal.core.application.port.out.repository

import java.io.FileInputStream
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Attachment

interface AttachmentRepository {
    suspend fun findAttachmentById(id: Long): Attachment?
    fun observeAvailableAttachments(): Flow<List<Attachment>>
    fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>>
    suspend fun addAttachment(description: String?, platformToken: String)
    suspend fun updateAttachmentDescription(id: Long, description: String?)
    suspend fun deleteAttachmentById(id: Long)
    suspend fun cleanup()
    suspend fun openAttachmentById(id: Long): FileInputStream?
    suspend fun generateAttachmentSharingTokenById(id: Long): String?
}
