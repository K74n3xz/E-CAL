package net.k74n3xz.ecal.ui.module.attendeemanagement.navigation

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.AttendeeDeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.AttendeeEditDialog
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.scaffold.AddAttendeeFabComponent
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.scaffold.AttendeeManagementScreenTopBarComponent
import net.k74n3xz.ecal.ui.module.attendeemanagement.screen.AttendeeManagementScreen
import net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel.AttendeeManagementViewModel
import net.k74n3xz.ecal.ui.module.attendeemanagement.viewmodel.state.AttendeeOperationState
import net.k74n3xz.ecal.ui.root.navigation.NavHostAction

private const val TAG = "AttendeeManagementEntry"

private enum class DialogType { ADD, EDIT, DELETE }

fun EntryProviderScope<NavKey>.registerAttendeeManagementEntry(
    registerNavHostAction: @Composable (NavKey, NavHostAction) -> Unit,
    backToParent: () -> Unit
) {
    entry<AttendeeManagementNavKey> { navKey ->
        val operationFailedMessage = stringResource(R.string.error_attendee_operation_failed)

        val viewModel: AttendeeManagementViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val attendees by viewModel.attendees.collectAsStateWithLifecycle()

        val uiStateSnapshot = uiState

        val isOperationInProgress = uiStateSnapshot.operationState == AttendeeOperationState.Saving ||
            uiStateSnapshot.operationState == AttendeeOperationState.Deleting

        var dialogState by remember { mutableStateOf<DialogType?>(null) }
        var selectedAttendee by remember { mutableStateOf<Attendee?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }

        registerNavHostAction(
            navKey,
            object : NavHostAction {
                override fun requestBack() {
                    viewModel.requestExit()
                }
            }
        )

        LaunchedEffect(uiStateSnapshot.operationState) {
            if (uiStateSnapshot.operationState is AttendeeOperationState.Idle &&
                uiStateSnapshot.operationState.failure != null
            ) {
                Log.e(TAG, "registerAttendeeManagementEntry: Operation failed.", uiStateSnapshot.operationState.failure)
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    // TODO: Map each operation failure to a specific, localized user-facing message.
                    message = operationFailedMessage,
                    actionLabel = null,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
            } else if (uiStateSnapshot.operationState == AttendeeOperationState.Exit) {
                if (viewModel.consumeExitState()) {
                    backToParent()
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { AttendeeManagementScreenTopBarComponent(onBack = viewModel::requestExit) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (uiStateSnapshot.operationState is AttendeeOperationState.Idle) {
                    AddAttendeeFabComponent {
                        dialogState = DialogType.ADD
                        selectedAttendee = null
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isOperationInProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                AttendeeManagementScreen(
                    attendees = attendees,
                    operationsEnabled = !isOperationInProgress,
                    onEdit = {
                        selectedAttendee = it
                        dialogState = DialogType.EDIT
                    },
                    onDelete = {
                        selectedAttendee = it
                        dialogState = DialogType.DELETE
                    },
                    modifier = Modifier.fillMaxSize()
                )

                when (dialogState) {
                    DialogType.ADD, DialogType.EDIT -> {
                        AttendeeEditDialog(
                            attendee = selectedAttendee,
                            onDismiss = {
                                dialogState = null
                                selectedAttendee = null
                            },
                            onConfirm = {
                                viewModel.saveAttendee(it)
                                dialogState = null
                                selectedAttendee = null
                            }
                        )
                    }

                    DialogType.DELETE -> {
                        AttendeeDeletionConfirmationDialog(
                            attendeeName = selectedAttendee!!.name ?: selectedAttendee!!.email,
                            onDismiss = {
                                dialogState = null
                                selectedAttendee = null
                            },
                            onConfirm = {
                                viewModel.deleteAttendee(requireNotNull(selectedAttendee!!.id))
                                dialogState = null
                                selectedAttendee = null
                            }
                        )
                    }

                    null -> Unit
                }
            }
        }
    }
}
