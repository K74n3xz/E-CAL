package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.`in`.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.AddAttachmentCommand
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.model.Attachment

internal class AddAttachmentUseCaseImpl(private val attachmentRepository: AttachmentRepository) : AddAttachmentUseCase {
    override suspend fun invoke(addAttachmentCommand: AddAttachmentCommand) {
        attachmentRepository.addAttachment(addAttachmentCommand.description, addAttachmentCommand.platformToken)
    }
}
