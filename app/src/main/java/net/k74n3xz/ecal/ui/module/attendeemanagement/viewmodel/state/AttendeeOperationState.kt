package net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel.state

internal sealed interface AttendeeOperationState {
    data class Idle(val failure: Throwable?) : AttendeeOperationState
    data object Saving : AttendeeOperationState
    data object Deleting : AttendeeOperationState
    data object Exit : AttendeeOperationState
    data object Exited : AttendeeOperationState
}
