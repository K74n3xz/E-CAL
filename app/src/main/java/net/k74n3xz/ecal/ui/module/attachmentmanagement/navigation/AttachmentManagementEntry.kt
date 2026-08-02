package net.k74n3xz.ecal.ui.module.attachmentmanagement.navigation

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
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.AttachmentDeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.AttachmentEditDialog
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.scaffold.AddAttachmentFabComponent
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.scaffold.AttachmentManagementScreenTopBarComponent
import net.k74n3xz.ecal.ui.module.attachmentmanagement.screen.AttachmentManagementScreen
import net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel.AttachmentManagementViewModel
import net.k74n3xz.ecal.ui.module.attachmentmanagement.viewmodel.state.AttachmentOperationState
import net.k74n3xz.ecal.ui.root.navigation.NavHostAction

private const val TAG: String = "AttachmentManagementEntry"

private enum class DialogType { ADD, EDIT, DELETE }

fun EntryProviderScope<NavKey>.registerAttachmentManagementEntry(
    registerNavHostAction: @Composable (NavKey, NavHostAction) -> Unit,
    backToParent: () -> Unit
) {
    entry<AttachmentManagementNavKey> {
        val operationFailedMessage = stringResource(R.string.error_attachment_operation_failed)

        val viewModel: AttachmentManagementViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val attachments by viewModel.attachments.collectAsStateWithLifecycle()

        val uiStateSnapshot = uiState

        val isOperationInProgress = uiStateSnapshot.operationState == AttachmentOperationState.Adding ||
            uiStateSnapshot.operationState == AttachmentOperationState.Updating ||
            uiStateSnapshot.operationState == AttachmentOperationState.Deleting

        var dialogState by remember { mutableStateOf<DialogType?>(null) }
        var selectedAttachment by remember { mutableStateOf<Attachment?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }

        registerNavHostAction(
            it,
            object : NavHostAction {
                override fun requestBack() {
                    viewModel.requestExit()
                }
            }
        )

        LaunchedEffect(uiStateSnapshot.operationState) {
            if (uiStateSnapshot.operationState is AttachmentOperationState.Idle &&
                uiStateSnapshot.operationState.failure != null
            ) {
                Log.e(
                    TAG,
                    "registerAttachmentManagementEntry: Operation failed.",
                    uiStateSnapshot.operationState.failure
                )
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    // TODO: Map each operation failure to a specific, localized user-facing message.
                    message = operationFailedMessage,
                    actionLabel = null,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
            } else if (uiState.operationState is AttachmentOperationState.Exit) {
                if (viewModel.consumeExitState()) {
                    backToParent()
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { AttachmentManagementScreenTopBarComponent(onBack = viewModel::requestExit) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (uiStateSnapshot.operationState is AttachmentOperationState.Idle) {
                    AddAttachmentFabComponent {
                        dialogState = DialogType.ADD
                        selectedAttachment = null
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

                AttachmentManagementScreen(
                    attachments = attachments,
                    operationsEnabled = !isOperationInProgress,
                    onEdit = { attachment ->
                        selectedAttachment = attachment
                        dialogState = DialogType.EDIT
                    },
                    onDelete = { attachment ->
                        selectedAttachment = attachment
                        dialogState = DialogType.DELETE
                    },
                    modifier = Modifier.fillMaxSize()
                )

                when (dialogState) {
                    DialogType.ADD -> {
                        AttachmentEditDialog(
                            initialDescription = null,
                            canImport = true,
                            onDismiss = {
                                dialogState = null
                                selectedAttachment = null
                            },
                            onConfirm = { description, platformToken ->
                                viewModel.addAttachment(description, platformToken!!)
                                dialogState = null
                                selectedAttachment = null
                            }
                        )
                    }

                    DialogType.EDIT -> {
                        AttachmentEditDialog(
                            initialDescription = selectedAttachment!!.description,
                            canImport = false,
                            onDismiss = {
                                dialogState = null
                                selectedAttachment = null
                            },
                            onConfirm = { description, _ ->
                                viewModel.updateAttachmentDescription(
                                    attachmentId = requireNotNull(selectedAttachment!!.id),
                                    description = description
                                )
                                dialogState = null
                                selectedAttachment = null
                            }
                        )
                    }

                    DialogType.DELETE -> {
                        AttachmentDeletionConfirmationDialog(
                            attachmentName = selectedAttachment!!.name,
                            onDismiss = {
                                dialogState = null
                                selectedAttachment = null
                            },
                            onConfirm = {
                                viewModel.deleteAttachment(requireNotNull(selectedAttachment!!.id))
                                dialogState = null
                                selectedAttachment = null
                            }
                        )
                    }

                    null -> Unit
                }
            }
        }
    }
}
