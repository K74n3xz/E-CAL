package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.AddAttachmentCommand
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttachmentRepository
import net.k74n3xz.ecal.core.model.Attachment

internal class AddAttachmentUseCaseImpl(private val attachmentRepository: AttachmentRepository) : AddAttachmentUseCase {
    override suspend fun invoke(addAttachmentCommand: AddAttachmentCommand) {
        attachmentRepository.addAttachment(addAttachmentCommand.description, addAttachmentCommand.platformToken)
    }
}
