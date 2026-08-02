package net.k74n3xz.ecal.core.application.impl.service

import java.io.FileInputStream
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.application.port.`in`.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.model.Attachment

internal class AttachmentQueryServiceImpl(private val attachmentRepository: AttachmentRepository) :
    AttachmentQueryService {
    override suspend fun findAttachmentById(attachmentId: Long): Attachment? =
        attachmentRepository.findAttachmentById(attachmentId)

    override fun observeAvailableAttachments(): Flow<List<Attachment>> =
        attachmentRepository.observeAvailableAttachments()

    override fun observeAvailableAttachmentsByMimeTopLevelType(topLevelType: String): Flow<List<Attachment>> =
        attachmentRepository.observeAvailableAttachmentsByMimeTopLevelType(topLevelType)

    override suspend fun openAttachmentById(attachmentId: Long): FileInputStream? =
        attachmentRepository.openAttachmentById(attachmentId)
}
