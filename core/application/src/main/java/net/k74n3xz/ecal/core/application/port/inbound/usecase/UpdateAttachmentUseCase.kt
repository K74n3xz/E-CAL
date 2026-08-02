package net.k74n3xz.ecal.core.application.port.inbound.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.command.UpdateAttachmentCommand

interface UpdateAttachmentUseCase {
    suspend operator fun invoke(updateAttachmentCommand: UpdateAttachmentCommand)
}
