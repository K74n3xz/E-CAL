package net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state

sealed interface EditOperationState {
    data object Uninitialized : EditOperationState
    data object Initializing : EditOperationState
    data object Idle : EditOperationState
    data object Saving : EditOperationState
    data object Deleting : EditOperationState
    data object Success : EditOperationState
    data class Failed(val cause: Throwable) : EditOperationState
}