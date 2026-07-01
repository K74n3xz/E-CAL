package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.application.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.application.usecase.SaveEventUseCase
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.domain.repository.EventRepository
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditMode
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditOperationState
import net.k74n3xz.ecal.di.IoDispatcher
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class EventEditViewModel @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val eventRepository: EventRepository,
    private val saveEventUseCase: SaveEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) : ViewModel() {
    private val _uiState: MutableStateFlow<EventEditUiState> = MutableStateFlow(
        EventEditUiState(
            editMode = null,
            operationState = EditOperationState.Uninitialized
        )
    )
    val uiState: StateFlow<EventEditUiState> = _uiState.asStateFlow()

    fun switchToAddMode() {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EditOperationState.Uninitialized) {
            return
        }

        _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(
                editMode = EditMode.AddEventMode,
                operationState = EditOperationState.Idle
            )
        )
    }

    fun switchToEditMode(eventUid: String) {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EditOperationState.Uninitialized) {
            return
        }

        if (!_uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = EditOperationState.Initializing)
            )
        ) {
            return
        }
        viewModelScope.launch(ioDispatcher) {
            var event: Event? = null
            val cause: Exception? = try {
                event = eventRepository.getEventByUid(eventUid)
                null
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                exception
            }

            if (cause is Exception) {
                _uiState.update { eventEditUiState ->
                    eventEditUiState.copy(
                        editMode = null,
                        operationState = EditOperationState.Failed(cause)
                    )
                }
            } else if (event == null) {
                _uiState.update { eventEditUiState ->
                    eventEditUiState.copy(
                        editMode = null,
                        operationState = EditOperationState.Failed(
                            IllegalArgumentException("Event(uid=$eventUid) doesn't exist.")
                        )
                    )
                }
            } else {
                _uiState.update { eventEditUiState ->
                    eventEditUiState.copy(
                        editMode = EditMode.EditEventMode(event),
                        operationState = EditOperationState.Idle
                    )
                }
            }
        }
    }

    fun saveEvent(event: Event) {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EditOperationState.Idle
            && uiStateSnapshot.operationState !is EditOperationState.Failed
        ) {
            return
        }

        if (!_uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = EditOperationState.Saving)
            )
        ) {
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val operationState = try {
                saveEventUseCase(event)
                EditOperationState.Success
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                EditOperationState.Failed(exception)
            }
            _uiState.update { it.copy(operationState = operationState) }
        }
    }

    fun deleteEvent(event: Event) {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EditOperationState.Idle
            && uiStateSnapshot.operationState !is EditOperationState.Failed
        ) {
            return
        }

        if (!_uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = EditOperationState.Deleting)
            )
        ) {
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val operationState = try {
                deleteEventUseCase(event.uid)
                EditOperationState.Success
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                EditOperationState.Failed(exception)
            }
            _uiState.update { it.copy(operationState = operationState) }
        }
    }

    fun requestBack() {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EditOperationState.Idle
            && uiStateSnapshot.operationState !is EditOperationState.Failed
        ) {
            return
        }

        _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(operationState = EditOperationState.Success)
        )
    }

    fun consumeOperationSuccess(): Boolean {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EditOperationState.Success) {
            return false
        }

        return _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(operationState = EditOperationState.Idle)
        )
    }
}