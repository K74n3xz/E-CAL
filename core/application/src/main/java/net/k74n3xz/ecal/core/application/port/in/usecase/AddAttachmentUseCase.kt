package net.k74n3xz.ecal.core.application.port.`in`.usecase

import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.AddAttachmentCommand

interface AddAttachmentUseCase {
    suspend operator fun invoke(addAttachmentCommand: AddAttachmentCommand)
}
