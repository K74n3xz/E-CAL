package net.k74n3xz.ecal.core.application.port.`in`.service

import java.io.FileInputStream
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.model.Attachment

interface AttachmentQueryService {
    suspend fun findAttachmentById(attachmentId: Long): Attachment?
    fun observeAvailableAttachments(): Flow<List<Attachment>>
    fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>>
    suspend fun openAttachmentById(attachmentId: Long): FileInputStream?
}
