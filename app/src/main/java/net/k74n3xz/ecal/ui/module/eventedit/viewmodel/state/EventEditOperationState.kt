package net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state

sealed interface EventEditOperationState {
    sealed interface InitializationState : EventEditOperationState {
        data object Uninitialized : InitializationState
        data object Initializing : InitializationState
        data class InitializationFailed(val cause: Throwable) : InitializationState
    }

    data class Idle(val failure: Throwable?) : EventEditOperationState
    data object Saving : EventEditOperationState
    data object Deleting : EventEditOperationState
    data object Exit : EventEditOperationState
    data object Exited : EventEditOperationState
}
