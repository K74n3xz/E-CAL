package net.k74n3xz.ecal.ui.module.eventedit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.ui.module.eventedit.component.DeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.eventedit.component.scaffold.EventEditScreenTopBarComponent
import net.k74n3xz.ecal.ui.module.eventedit.screen.EventEditScreen
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.EventEditViewModel
import java.time.Instant

fun EntryProviderScope<NavKey>.registerEventEditEntry(
    backToParent: () -> Unit
) {
    entry<EventEdit> {
        val isEditMode = it.eventUid != null

        val viewModel: EventEditViewModel = hiltViewModel()

        val event by viewModel.event.collectAsStateWithLifecycle()
        val isLoadingComplete = event != null
        var isConfirmingDeletion by rememberSaveable { mutableStateOf(false) }

        if (isEditMode) {
            LaunchedEffect(Unit) {
                viewModel.loadEventByUid(it.eventUid)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                EventEditScreenTopBarComponent(
                    titleText = if (!isEditMode) stringResource(R.string.topbar_title_text_new_event)
                    else stringResource(R.string.topbar_title_text_edit_event),
                    onBack = backToParent,  // TODO: Prompt before leaving when there are unsaved changes.
                    canDelete = isEditMode && isLoadingComplete,
                    onDelete = { isConfirmingDeletion = true }
                )
            }
        ) { innerPadding ->
            if (isEditMode && !isLoadingComplete) {
                // TODO: Extract the loading state into a reusable screen-level component.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.text_loading),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            } else {
                EventEditScreen(
                    event = if (!isEditMode) Event() else event!!,
                    alarms = if (!isEditMode) emptyList() else event!!.alarms,
                    onCancel = backToParent,
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
                        backToParent()
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                if (isConfirmingDeletion) {
                    DeletionConfirmationDialog(
                        onDismiss = { isConfirmingDeletion = false },
                        onConfirm = {
                            isConfirmingDeletion = false
                            backToParent()
                            viewModel.deleteEvent(event!!)
                        }
                    )
                }
            }
        }
    }
}