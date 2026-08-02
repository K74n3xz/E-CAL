package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.`in`.usecase.CleanupRemovingAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository

internal class CleanupRemovingAttachmentUseCaseImpl(private val attachmentRepository: AttachmentRepository) :
    CleanupRemovingAttachmentUseCase {
    override suspend fun invoke() {
        attachmentRepository.cleanup()
    }
}
