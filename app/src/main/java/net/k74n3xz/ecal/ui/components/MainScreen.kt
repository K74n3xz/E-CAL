package net.k74n3xz.ecal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.data.entity.Event
import net.k74n3xz.ecal.ui.viewmodel.MainScreenViewModel

@Composable
fun MainScreen(viewModel: MainScreenViewModel, modifier: Modifier = Modifier) {
    val zoneId = viewModel.zoneId
    val locale = viewModel.locale
    val calendarType = viewModel.calendarType
    val firstDayOfWeek = viewModel.firstDayOfWeek
    val selectedDate = viewModel.selectedDate
    val editingEvent: Event? = viewModel.editingEvent

    val dayWithEvent by viewModel.dateWithEvent.collectAsStateWithLifecycle()
    val eventInSelectedDate by viewModel.eventInSelectedDate.collectAsStateWithLifecycle()
    val alarmForEditingEvent by viewModel.alarmForEditingEvent.collectAsStateWithLifecycle()

    var isShowingEditor by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (!isShowingEditor) {
                FloatingActionButton(
                    onClick = {
                        isShowingEditor = true
                        viewModel.editingEvent = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.fab_content_description_add_new_event)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ModeSelectionRow(
                    locale = locale,
                    mode = calendarType,
                    onModeChanged = { viewModel.calendarType = it })
                Spacer(Modifier.height(12.dp))
                Calendar(
                    type = calendarType,
                    firstDayOfWeek = firstDayOfWeek,
                    locale = locale,
                    selectedDate = selectedDate,
                    eventDays = dayWithEvent,
                    onDateChanged = { viewModel.selectedDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HorizontalDivider()
            EventList(
                events = eventInSelectedDate.toList(),
                noEventHint = { Text(stringResource(R.string.text_no_events_today_hint)) },
                onEventClick = {
                    isShowingEditor = true
                    viewModel.editingEvent = it
                },
                timezone = zoneId,
                locale = locale,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = isShowingEditor,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                tonalElevation = 8.dp
            ) {
                EventEditorScreen(
                    editingEvent = editingEvent,
                    alarmForEditingEvent = alarmForEditingEvent,
                    defaultDate = selectedDate,
                    zoneId = zoneId,
                    locale = locale,
                    onSave = { event, alarms ->
                        viewModel.upsertEventWithAlarms(event, alarms)
                        isShowingEditor = false
                    },
                    onDelete = if (editingEvent == null) { _ ->
                        isShowingEditor = false
                    } else { it ->
                        viewModel.deleteEvent(it)
                        isShowingEditor = false
                    },
                    onCancel = { isShowingEditor = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/*
* TODO: Add a preview for MainScreen.
* @Preview(showBackground = true)
* @Composable
* private fun MainScreenPreview() {
*     val viewModel = MainScreenViewModel.Factory.create(MainScreenViewModel::class.java)
*
*     MainScreen(viewModel)
* }
* */