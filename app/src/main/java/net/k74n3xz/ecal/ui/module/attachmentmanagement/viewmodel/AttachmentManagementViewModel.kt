package net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.k74n3xz.ecal.core.application.port.`in`.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.`in`.usecase.AddAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.UpdateAttachmentUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.AddAttachmentCommand
import net.k74n3xz.ecal.core.application.port.`in`.usecase.command.UpdateAttachmentCommand
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel.state.AttachmentOperationState

@HiltViewModel
internal class AttachmentManagementViewModel @Inject constructor(
    private val attachmentQueryService: AttachmentQueryService,
    private val addAttachmentUseCase: AddAttachmentUseCase,
    private val updateAttachmentUseCase: UpdateAttachmentUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase
) : ViewModel() {
    private val _uiState: MutableStateFlow<AttachmentManagementUiState> =
        MutableStateFlow(AttachmentManagementUiState(operationState = AttachmentOperationState.Idle(null)))
    val uiState: StateFlow<AttachmentManagementUiState> = _uiState.asStateFlow()

    // TODO: Convert upstream query failures into a retryable UI state while preserving the last successfully loaded attachment list.
    val attachments: StateFlow<List<Attachment>> = attachmentQueryService.observeAvailableAttachments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAttachment(description: String?, platformToken: String) {
        launchOperation(AttachmentOperationState.Adding) {
            withContext(Dispatchers.IO) {
                addAttachmentUseCase(
                    AddAttachmentCommand(
                        description = description,
                        platformToken = platformToken
                    )
                )
            }
        }
    }

    fun updateAttachmentDescription(attachmentId: Long, description: String?) {
        launchOperation(AttachmentOperationState.Updating) {
            updateAttachmentUseCase(
                UpdateAttachmentCommand(
                    id = attachmentId,
                    description = description
                )
            )
        }
    }

    fun deleteAttachment(attachmentId: Long) {
        launchOperation(AttachmentOperationState.Deleting) {
            deleteAttachmentUseCase(attachmentId)
        }
    }

    fun requestExit() {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is AttachmentOperationState.Idle) {
            return
        }

        _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(operationState = AttachmentOperationState.Exit)
        )
    }

    fun consumeExitState(): Boolean {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState != AttachmentOperationState.Exit) {
            return false
        }

        return _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(operationState = AttachmentOperationState.Exited)
        )
    }

    private fun launchOperation(operationState: AttachmentOperationState, operation: suspend () -> Unit) {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is AttachmentOperationState.Idle) {
            return
        }

        if (!_uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = operationState)
            )
        ) {
            return
        }

        viewModelScope.launch {
            val failure: Exception? = try {
                operation()
                null
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                exception
            }

            _uiState.update { it.copy(operationState = AttachmentOperationState.Idle(failure)) }
        }
    }
}
