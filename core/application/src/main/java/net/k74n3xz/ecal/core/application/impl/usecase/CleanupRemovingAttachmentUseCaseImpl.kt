package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.CleanupRemovingAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttachmentRepository

internal class CleanupRemovingAttachmentUseCaseImpl(private val attachmentRepository: AttachmentRepository) :
    CleanupRemovingAttachmentUseCase {
    override suspend fun invoke() {
        attachmentRepository.cleanup()
    }
}
