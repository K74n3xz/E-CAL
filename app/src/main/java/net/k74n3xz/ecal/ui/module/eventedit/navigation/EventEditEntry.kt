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
import net.k74n3xz.ecal.ui.module.eventedit.component.DeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.eventedit.component.scaffold.EventEditScreenTopBarComponent
import net.k74n3xz.ecal.ui.module.eventedit.screen.ColorOverlay
import net.k74n3xz.ecal.ui.module.eventedit.screen.EventEditScreen
import net.k74n3xz.ecal.ui.module.eventedit.screen.TextScreen
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.EventEditViewModel
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EventEditOperationState
import net.k74n3xz.ecal.ui.root.navigation.NavHostAction

private const val TAG: String = "EventEditEntry"

fun EntryProviderScope<NavKey>.registerEventEditEntry(
    registerNavHostAction: @Composable (NavKey, NavHostAction) -> Unit,
    backToParent: () -> Unit
) {
    entry<EventEditNavKey> {
        val operationFailedMessage = stringResource(R.string.error_event_operation_failed)

        val viewModel: EventEditViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val alarmForms by viewModel.alarmForms.collectAsStateWithLifecycle()
        val audioAttachments by viewModel.audioAttachments.collectAsStateWithLifecycle()
        val attendees by viewModel.attendees.collectAsStateWithLifecycle()
        val attachments by viewModel.attachments.collectAsStateWithLifecycle()

        val uiStateSnapshot = uiState

        val isBusyWithOperation = uiStateSnapshot.operationState is EventEditOperationState.Saving ||
            uiStateSnapshot.operationState is EventEditOperationState.Deleting

        var isConfirmingDeletion by rememberSaveable { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        registerNavHostAction(
            it,
            object : NavHostAction {
                override fun requestBack() {
                    viewModel.requestExit()
                }
            }
        )

        LaunchedEffect(Unit) {
            viewModel.initialize(it.eventUid)
        }

        LaunchedEffect(uiStateSnapshot.operationState) {
            if (uiStateSnapshot.operationState is EventEditOperationState.InitializationState.InitializationFailed) {
                Log.e(TAG, "registerEventEditEntry: Initialization failed.", uiStateSnapshot.operationState.cause)
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    // TODO: Map each operation failure to a specific, localized user-facing message.
                    message = operationFailedMessage,
                    actionLabel = null,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
            } else if (uiStateSnapshot.operationState is EventEditOperationState.Idle &&
                uiStateSnapshot.operationState.failure != null
            ) {
                Log.e(TAG, "registerEventEditEntry: Operation failed.", uiStateSnapshot.operationState.failure)
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    // TODO: Map each operation failure to a specific, localized user-facing message.
                    message = operationFailedMessage,
                    actionLabel = null,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
            } else if (uiStateSnapshot.operationState is EventEditOperationState.Exit) {
                if (viewModel.consumeExitState()) {
                    backToParent()
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                EventEditScreenTopBarComponent(
                    titleText = if (uiStateSnapshot.operationState is EventEditOperationState.InitializationState) {
                        ""
                    } else if (it.eventUid == null) {
                        stringResource(R.string.topbar_title_text_new_event)
                    } else {
                        stringResource(R.string.topbar_title_text_edit_event)
                    },
                    // TODO: Ask the user to confirm before discarding unsaved changes.
                    onBack = viewModel::requestExit,
                    canDelete = uiStateSnapshot.operationState !is EventEditOperationState.InitializationState &&
                        it.eventUid != null,
                    onDelete = {
                        if (uiStateSnapshot.operationState !is EventEditOperationState.InitializationState) {
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
                if (isBusyWithOperation) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when (uiStateSnapshot.operationState) {
                    is EventEditOperationState.InitializationState.Uninitialized,
                    is EventEditOperationState.InitializationState.Initializing -> {
                        TextScreen(
                            text = stringResource(R.string.text_loading),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is EventEditOperationState.InitializationState.InitializationFailed -> {
                        TextScreen(
                            text = stringResource(R.string.error_event_loading_failed),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is EventEditOperationState.Idle,
                    is EventEditOperationState.Saving,
                    is EventEditOperationState.Deleting -> {
                        ColorOverlay(enabled = isBusyWithOperation) {
                            EventEditScreen(
                                eventForm = viewModel.eventForm,
                                alarmForms = alarmForms,
                                audioAttachments = audioAttachments,
                                attendees = attendees,
                                attachments = attachments,
                                onAddAlarm = viewModel::addAlarm,
                                onRemoveAlarm = viewModel::removeAlarm,
                                onCancel = viewModel::requestExit,
                                canSave =
                                    viewModel.eventForm.isValid && alarmForms.all { alarmForm -> alarmForm.isValid },
                                onSave = viewModel::saveEvent,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isConfirmingDeletion) {
                                DeletionConfirmationDialog(
                                    onDismiss = { isConfirmingDeletion = false },
                                    onConfirm = {
                                        isConfirmingDeletion = false
                                        if (it.eventUid != null) {
                                            viewModel.deleteEvent()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    is EventEditOperationState.Exit,
                    is EventEditOperationState.Exited -> {
                        TextScreen(
                            text = "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
