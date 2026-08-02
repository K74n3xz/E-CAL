package net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel.state

sealed interface AttachmentOperationState {
    data class Idle(val failure: Throwable?) : AttachmentOperationState
    data object Adding : AttachmentOperationState
    data object Updating : AttachmentOperationState
    data object Deleting : AttachmentOperationState
    data object Exit : AttachmentOperationState
    data object Exited : AttachmentOperationState
}
