package net.k74n3xz.ecal.core.application.port.`in`.usecase

import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.UpdateAttachmentCommand

interface UpdateAttachmentUseCase {
    suspend operator fun invoke(updateAttachmentCommand: UpdateAttachmentCommand)
}
