package net.k74n3xz.ecal.core.application.port.inbound.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.AddAttachmentCommand

interface AddAttachmentUseCase {
    suspend operator fun invoke(addAttachmentCommand: AddAttachmentCommand)
}
