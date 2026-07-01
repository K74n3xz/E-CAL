package net.k74n3xz.ecal.ui.module.eventedit.navigation

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.ui.module.eventedit.component.DeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.eventedit.component.scaffold.EventEditScreenTopBarComponent
import net.k74n3xz.ecal.ui.module.eventedit.screen.ColorOverlay
import net.k74n3xz.ecal.ui.module.eventedit.screen.EventEditScreen
import net.k74n3xz.ecal.ui.module.eventedit.screen.LoadingScreen
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.EventEditViewModel
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditMode
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditOperationState
import net.k74n3xz.ecal.ui.root.navigation.NavHostAction
import java.time.Instant

const val TAG: String = "EventEditEntry"

fun EntryProviderScope<NavKey>.registerEventEditEntry(
    registerNavHostAction: @Composable (NavKey, NavHostAction) -> Unit,
    backToParent: () -> Unit
) {
    entry<EventEdit> {
        val operationFailedMessage = stringResource(R.string.error_event_operation_failed)

        val viewModel: EventEditViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val isBusy = uiState.operationState is EditOperationState.Initializing
                || uiState.operationState is EditOperationState.Saving
                || uiState.operationState is EditOperationState.Deleting

        var isConfirmingDeletion by rememberSaveable { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        registerNavHostAction(it, object : NavHostAction {
            override fun requestBack() {
                viewModel.requestBack()
            }
        })

        LaunchedEffect(Unit) {
            if (it.eventUid == null) {
                viewModel.switchToAddMode()
            } else {
                viewModel.switchToEditMode(it.eventUid)
            }
        }

        LaunchedEffect(uiState.operationState) {
            uiState.operationState.let { operationState ->
                if (operationState is EditOperationState.Success) {
                    if (viewModel.consumeOperationSuccess()) {
                        backToParent()
                    }
                } else if (operationState is EditOperationState.Failed) {
                    Log.e(TAG, "registerEventEditEntry: Operation failed.", operationState.cause)
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        // TODO: Map each operation failure to a specific, localized user-facing message.
                        message = operationFailedMessage,
                        actionLabel = null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        }

        uiState.let { state ->
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    EventEditScreenTopBarComponent(
                        titleText = when (state.editMode) {
                            is EditMode.AddEventMode -> stringResource(R.string.topbar_title_text_new_event)
                            is EditMode.EditEventMode -> stringResource(R.string.topbar_title_text_edit_event)
                            null -> ""
                        },
                        // TODO: Ask the user to confirm before discarding unsaved changes.
                        onBack = viewModel::requestBack,
                        canDelete = state.editMode is EditMode.EditEventMode,
                        onDelete = {
                            if (!isBusy) {
                                isConfirmingDeletion = true
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (state.editMode == null) {
                        LoadingScreen(modifier = Modifier.fillMaxSize())
                    } else {
                        ColorOverlay(enabled = isBusy) {
                            EventEditScreen(
                                event = when (state.editMode) {
                                    is EditMode.AddEventMode -> Event()
                                    is EditMode.EditEventMode -> state.editMode.event
                                },
                                onCancel = viewModel::requestBack,
                                onSave = { newEvent ->
                                    var e = newEvent
                                    // TODO: Move event validation into a shared validator and show errors instead of coercing endAt.
                                    val startAt = newEvent.startAt
                                    val endAt = newEvent.endAt
                                    if (endAt != null && endAt.isBefore(startAt)) {
                                        e = e.copy(endAt = startAt)
                                    }

                                    val updatedAt = Instant.now()
                                    e = if (it.eventUid == null) {
                                        e.copy(
                                            createdAt = updatedAt,
                                            updatedAt = updatedAt
                                        )
                                    } else {
                                        e.copy(updatedAt = updatedAt)
                                    }
                                    viewModel.saveEvent(e)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isConfirmingDeletion) {
                                DeletionConfirmationDialog(
                                    onDismiss = { isConfirmingDeletion = false },
                                    onConfirm = {
                                        isConfirmingDeletion = false
                                        if (state.editMode is EditMode.EditEventMode) {
                                            viewModel.deleteEvent(state.editMode.event)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}