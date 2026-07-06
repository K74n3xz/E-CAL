package net.k74n3xz.ecal.ui.module.eventedit.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.core.model.enumeration.event.TimeTransparency
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import net.k74n3xz.ecal.ui.module.eventedit.component.AlarmCardEditComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.ComboBoxComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.DateFieldComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.EventTextFieldComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.TimeFieldComponent
import net.k74n3xz.ecal.utils.atEndOfDay
import net.k74n3xz.ecal.utils.generateEventUid
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    event: Event,
    onCancel: () -> Unit,
    onSave: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(state = rememberScrollState())
            .animateContentSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val timeZone = LocalTimeZone.current

        // Summary
        val summaryFieldState = rememberTextFieldState(event.summary ?: "")
        var isSummaryFieldClear by rememberSaveable { mutableStateOf(event.summary == null) }

        // Description
        val descriptionFieldState = rememberTextFieldState(event.description ?: "")
        var isDescriptionFieldClear by rememberSaveable { mutableStateOf(event.description == null) }

        // Location
        val locationFieldState = rememberTextFieldState(event.location ?: "")
        var isLocationFieldClear by rememberSaveable { mutableStateOf(event.location == null) }

        // Time
        var hasEndAt by rememberSaveable { mutableStateOf(event.endAt != null) }
        var isAllDayEvent by rememberSaveable { mutableStateOf(event.isAllDayEvent) }

        // Time - Start At
        var startDate by rememberSaveable {
            mutableStateOf(
                event.startAt.atZone(timeZone).toLocalDate()
            )
        }
        var startTime by rememberSaveable {
            mutableStateOf(
                event.startAt.atZone(timeZone).toLocalTime()
            )
        }

        // Time - End At
        var endDate by rememberSaveable {
            mutableStateOf(
                event.endAt?.atZone(timeZone)?.toLocalDate()
            )
        }
        var endTime by rememberSaveable {
            mutableStateOf(
                event.endAt?.atZone(timeZone)?.toLocalTime()
            )
        }

        // Priority
        var priority by rememberSaveable { mutableStateOf(event.priority) }

        // Time Transparency
        var timeTransparency by rememberSaveable { mutableStateOf(event.transparency) }

        // recurrenceRule

        // Status
        var status by rememberSaveable { mutableStateOf(event.status) }

        // Alarms
        val mutableAlarms = rememberSaveable { event.alarms.toMutableStateList() }

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val priorities = mapOf(
                0 to stringResource(R.string.text_priority_undefined),
                1 to stringResource(R.string.text_priority_highest),
                2 to "2",
                3 to "3",
                4 to "4",
                5 to stringResource(R.string.text_priority_medium),
                6 to "6",
                7 to "7",
                8 to "8",
                9 to stringResource(R.string.text_priority_lowest)
            )
            val timeTransparencyOptions = mapOf(
                TimeTransparency.OPAQUE to stringResource(R.string.text_time_transparency_opaque),
                TimeTransparency.TRANSPARENT to stringResource(R.string.text_time_transparency_transparent)
            )
            val statusOptions = mapOf(
                EventStatus.TENTATIVE to stringResource(R.string.text_event_status_tentative),
                EventStatus.CONFIRMED to stringResource(R.string.text_event_status_confirmed),
                EventStatus.CANCELLED to stringResource(R.string.text_event_status_cancelled)
            )
            val defaultKeyboardOptions = KeyboardOptions.Default.copy(
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                showKeyboardOnFocus = true
            )

            // Summary
            EventTextFieldComponent(
                textFieldState = summaryFieldState,
                isClear = isSummaryFieldClear,
                onClear = { isSummaryFieldClear = true },
                onDirty = { isSummaryFieldClear = false },
                labelText = stringResource(R.string.text_field_label_summary),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = defaultKeyboardOptions.copy(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            EventTextFieldComponent(
                textFieldState = descriptionFieldState,
                isClear = isDescriptionFieldClear,
                onClear = { isDescriptionFieldClear = true },
                onDirty = { isDescriptionFieldClear = false },
                labelText = stringResource(R.string.text_field_label_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = defaultKeyboardOptions.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            EventTextFieldComponent(
                textFieldState = locationFieldState,
                isClear = isLocationFieldClear,
                onClear = { isLocationFieldClear = true },
                onDirty = { isLocationFieldClear = false },
                labelText = stringResource(R.string.text_field_label_location),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = defaultKeyboardOptions.copy(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time:
            // Time - Start At
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.datetime_picker_label_start_at),
                    modifier = Modifier.weight(0.2f),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.weight(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateFieldComponent(
                        date = startDate,
                        onPickDate = { startDate = it },
                        modifier = Modifier.weight(0.62f)
                    )

                    // TODO: Animate the start and end time fields when all-day mode changes.
                    if (!isAllDayEvent) {
                        TimeFieldComponent(
                            time = startTime,
                            onPickTime = { startTime = it },
                            modifier = Modifier.weight(0.38f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time - End At
            AnimatedVisibility(hasEndAt) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.datetime_picker_label_end_at),
                        modifier = Modifier.weight(0.2f),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.weight(0.8f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (endDate == null) {
                            endDate = startDate
                        }
                        DateFieldComponent(
                            date = endDate!!,
                            onPickDate = { endDate = it },
                            modifier = Modifier.weight(0.62f)
                        )

                        if (!isAllDayEvent) {
                            if (endTime == null) {
                                endTime = startTime.plusMinutes(15)
                            }
                            TimeFieldComponent(
                                time = endTime!!,
                                onPickTime = { endTime = it },
                                modifier = Modifier.weight(0.38f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time - Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.switch_period_description),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Switch(
                        checked = hasEndAt,
                        onCheckedChange = { hasEndAt = it }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.switch_all_day_description),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Switch(
                        checked = isAllDayEvent,
                        onCheckedChange = { isAllDayEvent = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_field_label_priority),
                    modifier = Modifier.weight(0.4f),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                ComboBoxComponent(
                    text = priority?.let { priorities[it] } ?: "",
                    items = priorities,
                    onItemSelect = { priority = it },
                    modifier = Modifier.weight(0.6f),
                    canClear = priority != null,
                    onClear = { priority = null }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Transparency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_field_label_time_transparency),
                    modifier = Modifier.weight(0.4f),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                ComboBoxComponent(
                    text = timeTransparency?.let { timeTransparencyOptions[it] } ?: "",
                    items = timeTransparencyOptions,
                    onItemSelect = { timeTransparency = it },
                    modifier = Modifier.weight(0.6f),
                    canClear = timeTransparency != null,
                    onClear = { timeTransparency = null }
                )
            }

            // recurrenceRule

            Spacer(modifier = Modifier.height(16.dp))

            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_field_label_status),
                    modifier = Modifier.weight(0.4f),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                ComboBoxComponent(
                    text = status?.let { statusOptions[it] } ?: "",
                    items = statusOptions,
                    onItemSelect = { status = it },
                    modifier = Modifier.weight(0.6f),
                    canClear = status != null,
                    onClear = { status = null }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alarms
            mutableAlarms.forEachIndexed { index, alarm ->
                // TODO: Animate alarm cards when reminders are added or removed.
                Spacer(modifier = Modifier.height(16.dp))

                AlarmCardEditComponent(
                    titleText = stringResource(R.string.text_alarm_title_numbered, index + 1),
                    alarm = alarm,
                    onModify = { mutableAlarms[index] = it },
                    onRemove = { mutableAlarms.removeAt(index) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        mutableAlarms.add(
                            Alarm(
                                id = null,
                                action = Alarm.Action.Display(""),
                                trigger = Alarm.Trigger.RelativeTrigger(
                                    relativeTo = TriggerRelationship.START,
                                    offset = Duration.ofMinutes(-15)
                                ),
                                repetition = null
                            )
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.button_text_add_relative_alarm),
                        color = Color.Unspecified,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        mutableAlarms.add(
                            Alarm(
                                id = null,
                                action = Alarm.Action.Display(""),
                                trigger = Alarm.Trigger.AbsoluteTrigger(
                                    at = ZonedDateTime.now(timeZone).plusMinutes(15).toInstant()
                                ),
                                repetition = null
                            )
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.button_text_add_absolute_alarm),
                        color = Color.Unspecified,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.text_cancel),
                    color = Color.Unspecified,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    val startAt = if (isAllDayEvent) {
                        startDate.atStartOfDay(timeZone).toInstant()
                    } else {
                        ZonedDateTime.of(startDate, startTime, timeZone).toInstant()
                    }
                    val endAt = if (hasEndAt) {
                        if (isAllDayEvent) {
                            endDate!!.atEndOfDay(timeZone).toInstant()
                        } else {
                            ZonedDateTime.of(endDate!!, endTime!!, timeZone).toInstant()
                        }
                    } else {
                        null
                    }

                    onSave(
                        event.copy(
                            summary = if (isSummaryFieldClear) {
                                null
                            } else {
                                summaryFieldState.text.toString()
                            },
                            description = if (isDescriptionFieldClear) {
                                null
                            } else {
                                descriptionFieldState.text.toString()
                            },
                            location = if (isLocationFieldClear) {
                                null
                            } else {
                                locationFieldState.text.toString()
                            },
                            startAt = startAt,
                            isAllDayEvent = isAllDayEvent,
                            endAt = endAt,
                            priority = priority,
                            transparency = timeTransparency,
                            status = status,
                            alarms = mutableAlarms.toList()
                        )
                    )
                }
            ) {
                Text(
                    text = stringResource(R.string.text_save),
                    color = Color.Unspecified,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventEditScreenPreview1() {
    CompositionLocalProvider(LocalTimeZone provides ZoneId.systemDefault()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            EventEditScreen(
                event = Event(generateEventUid()),
                onCancel = {},
                onSave = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventEditScreenPreview2() {
    CompositionLocalProvider(LocalTimeZone provides ZoneId.systemDefault()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            EventEditScreen(
                event = Event(
                    uid = generateEventUid(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    summary = "Team Standup",
                    description = "Daily sync",
                    location = "Conference Room A",
                    startAt = Instant.now().plusSeconds(3600),
                    isAllDayEvent = false,
                    endAt = Instant.now().plusSeconds(3600 * 2),
                    status = EventStatus.CONFIRMED
                ),
                onCancel = {},
                onSave = {}
            )
        }
    }
}