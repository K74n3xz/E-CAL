package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.UpdateAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.UpdateAttachmentCommand
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttachmentRepository

internal class UpdateAttachmentUseCaseImpl(private val attachmentRepository: AttachmentRepository) :
    UpdateAttachmentUseCase {
    override suspend fun invoke(updateAttachmentCommand: UpdateAttachmentCommand) {
        attachmentRepository.updateAttachmentDescription(
            updateAttachmentCommand.id,
            updateAttachmentCommand.description
        )
    }
}
