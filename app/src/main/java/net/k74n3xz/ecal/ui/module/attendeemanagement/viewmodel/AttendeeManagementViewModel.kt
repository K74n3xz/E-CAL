package net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.core.application.port.`in`.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveAttendeeUseCase
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel.state.AttendeeOperationState

@HiltViewModel
internal class AttendeeManagementViewModel @Inject constructor(
    attendeeQueryService: AttendeeQueryService,
    private val saveAttendeeUseCase: SaveAttendeeUseCase,
    private val deleteAttendeeUseCase: DeleteAttendeeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AttendeeManagementUiState(operationState = AttendeeOperationState.Idle(null))
    )
    val uiState: StateFlow<AttendeeManagementUiState> = _uiState.asStateFlow()

    val attendees: StateFlow<List<Attendee>> = attendeeQueryService.observeAllAttendees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAttendee(attendee: Attendee) {
        launchOperation(AttendeeOperationState.Saving) {
            saveAttendeeUseCase(attendee)
        }
    }

    fun deleteAttendee(attendeeId: Long) {
        launchOperation(AttendeeOperationState.Deleting) {
            deleteAttendeeUseCase(attendeeId)
        }
    }

    fun requestExit() {
        val snapshot = _uiState.value
        if (snapshot.operationState !is AttendeeOperationState.Idle) return

        _uiState.compareAndSet(
            expect = snapshot,
            update = snapshot.copy(operationState = AttendeeOperationState.Exit)
        )
    }

    fun consumeExitState(): Boolean {
        val snapshot = _uiState.value
        if (snapshot.operationState != AttendeeOperationState.Exit) return false

        return _uiState.compareAndSet(
            expect = snapshot,
            update = snapshot.copy(operationState = AttendeeOperationState.Exited)
        )
    }

    private fun launchOperation(state: AttendeeOperationState, operation: suspend () -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.operationState !is AttendeeOperationState.Idle) return
        if (!_uiState.compareAndSet(snapshot, snapshot.copy(operationState = state))) return

        viewModelScope.launch {
            val failure = try {
                operation()
                null
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                exception
            }
            _uiState.update { it.copy(operationState = AttendeeOperationState.Idle(failure)) }
        }
    }
}
