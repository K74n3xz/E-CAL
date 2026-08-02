package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.out.platform.AttachmentCleanupScheduler
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository

internal class DeleteAttachmentUseCaseImpl(
    private val attachmentRepository: AttachmentRepository,
    private val attachmentCleanupScheduler: AttachmentCleanupScheduler
) : DeleteAttachmentUseCase {
    override suspend fun invoke(attachmentId: Long) {
        attachmentRepository.deleteAttachmentById(attachmentId)
        attachmentCleanupScheduler.request()
    }
}
