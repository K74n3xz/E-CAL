package net.k74n3xz.ecal.core.application.port.inbound.usecase

interface DeleteAttachmentUseCase {
    suspend operator fun invoke(attachmentId: Long)
}
