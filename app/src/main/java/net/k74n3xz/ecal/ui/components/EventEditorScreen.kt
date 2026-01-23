package net.k74n3xz.ecal.ui.components

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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.data.entity.Alarm
import net.k74n3xz.ecal.data.entity.Event
import net.k74n3xz.ecal.data.entity.enumeration.alarm.AlarmType
import net.k74n3xz.ecal.data.entity.enumeration.event.EventStatus
import net.k74n3xz.ecal.data.util.generateEventUid
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    editingEvent: Event?,
    alarmForEditingEvent: Array<Alarm>,
    defaultDate: LocalDate,
    zoneId: ZoneId,
    locale: Locale,
    onSave: (Event, Array<Alarm>) -> Unit,
    onDelete: (Event) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventUid by rememberSaveable { mutableStateOf(editingEvent?.uid ?: generateEventUid()) }
    val summary = rememberTextFieldState(editingEvent?.summary ?: "")
    val description = rememberTextFieldState(editingEvent?.description ?: "")
    val location = rememberTextFieldState(editingEvent?.location ?: "")
    var startAt by remember {
        mutableStateOf(
            if (editingEvent == null) ZonedDateTime.of(
                defaultDate,
                LocalTime.now(),
                zoneId
            ) else ZonedDateTime.ofInstant(editingEvent.startAt, zoneId)
        )
    }
    var isAllDayEvent by remember { mutableStateOf(editingEvent?.isAllDayEvent ?: false) }
    var isPeriod by remember { mutableStateOf(if (editingEvent == null) false else editingEvent.endAt != null) }
    var endAt by remember {
        mutableStateOf(
            if (editingEvent == null || editingEvent.endAt == null) startAt.plusHours(
                1
            ) else ZonedDateTime.ofInstant(editingEvent.endAt, zoneId)
        )
    }
    val alarms = rememberSaveable { mutableStateListOf(*alarmForEditingEvent) }

    var isShowingDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingEvent == null) stringResource(R.string.topbar_title_text_new_event) else stringResource(
                            R.string.topbar_title_text_edit_event
                        )
                    )
                },
                actions = {
                    if (editingEvent != null) {
                        FilledTonalButton(onClick = { isShowingDeleteConfirm = true }) {
                            Text(
                                stringResource(R.string.button_text_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                state = summary,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.text_field_label_summary)) },
                lineLimits = TextFieldLineLimits.SingleLine
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                state = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text(stringResource(R.string.text_field_label_description)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                state = location,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.text_field_label_location)) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            DateTimePicker(
                label = stringResource(R.string.datetime_picker_label_start_at),
                zoneId = zoneId,
                locale = locale,
                isDateOnly = isAllDayEvent,
                zonedDateTime = startAt,
                onDateTimeSelected = { startAt = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (isPeriod) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedVisibility(isPeriod) {
                DateTimePicker(
                    label = stringResource(R.string.datetime_picker_label_end_at),
                    zoneId = zoneId,
                    locale = locale,
                    isDateOnly = isAllDayEvent,
                    zonedDateTime = endAt,
                    onDateTimeSelected = { endAt = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.switch_period_description))
                Spacer(modifier = Modifier.width(16.dp))
                Switch(checked = isPeriod, onCheckedChange = { isPeriod = it })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.switch_all_day_description))
                Spacer(modifier = Modifier.width(16.dp))
                Switch(checked = isAllDayEvent, onCheckedChange = { isAllDayEvent = it })
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = stringResource(R.string.text_event_alarm_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                alarms.forEachIndexed { idx, alarm ->
                    AlarmEditor(
                        alarm = alarm,
                        zoneId = zoneId,
                        locale = locale,
                        onChange = { alarms[idx] = it },
                        onRemove = { alarms.remove(alarm) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = {
                        alarms.add(
                            Alarm(
                                refUid = eventUid,
                                type = AlarmType.ABSOLUTE,
                                absoluteTrigger = ZonedDateTime.now(zoneId).plusMinutes(15)
                                    .toInstant(),
                                relativeMinutesTrigger = null
                            )
                        )
                    }) { Text(stringResource(R.string.button_text_add_absolute_alarm)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(onClick = {
                        alarms.add(
                            Alarm(
                                refUid = eventUid,
                                type = AlarmType.RELATIVE,
                                absoluteTrigger = null,
                                relativeMinutesTrigger = 15
                            )
                        )
                    }) { Text(stringResource(R.string.button_text_add_relative_alarm)) }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.text_cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val eventToSave = Event(
                        uid = eventUid,
                        createdAt = editingEvent?.createdAt ?: Instant.now(),
                        updatedAt = Instant.now(),
                        summary = summary.text.toString(),
                        description = description.text.toString(),
                        location = location.text.toString(),
                        startAt = startAt.toInstant(),
                        isAllDayEvent = isAllDayEvent,
                        endAt = if (isPeriod) endAt.toInstant() else null,
                        status = EventStatus.CONFIRMED
                    )
                    onSave(eventToSave, alarms.toTypedArray())
                }) { Text(stringResource(R.string.text_save)) }
            }
        }
    }

    if (isShowingDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { isShowingDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(editingEvent!!)
                    isShowingDeleteConfirm = false
                }) { Text(stringResource(R.string.text_delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    isShowingDeleteConfirm = false
                }) { Text(stringResource(R.string.text_cancel)) }
            },
            title = { Text(stringResource(R.string.text_delete_event)) },
            text = { Text(stringResource(R.string.text_delete_event_warning)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePicker(
    label: String,
    zoneId: ZoneId,
    locale: Locale,
    isDateOnly: Boolean,
    zonedDateTime: ZonedDateTime,
    onDateTimeSelected: (ZonedDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    val datePickerState = rememberDatePickerState(zonedDateTime.toInstant().toEpochMilli())
    val timePickerState = rememberTimePickerState(
        initialHour = zonedDateTime.hour,
        initialMinute = zonedDateTime.minute
    )
    var isShowingDatePicker by remember { mutableStateOf(false) }
    var isShowingTimePicker by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(text = label)
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = zonedDateTime.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { isShowingDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.text_field_trailing_icon_content_description_select_date)
                        )
                    }
                })
            if (!isDateOnly) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            AnimatedVisibility(!isDateOnly) {
                OutlinedTextField(
                    value = zonedDateTime.format(timeFormatter),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { isShowingTimePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.text_field_trailing_icon_content_description_select_time)
                            )
                        }
                    })
            }
        }
    }

    if (isShowingDatePicker) {
        DatePickerDialog(
            onDismissRequest = { isShowingDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.getSelectedDate()?.let {
                        onDateTimeSelected(
                            ZonedDateTime.of(
                                it,
                                zonedDateTime.toLocalTime(),
                                zoneId
                            )
                        )
                    }
                    isShowingDatePicker = false
                }) {
                    Text(stringResource(R.string.text_ok))
                }
            },
            modifier = Modifier.fillMaxSize(),
            dismissButton = {
                TextButton(onClick = { isShowingDatePicker = false }) {
                    Text(stringResource(R.string.text_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (isShowingTimePicker) {
        AlertDialog(
            onDismissRequest = { isShowingTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateTimeSelected(
                        ZonedDateTime.of(
                            zonedDateTime.toLocalDate(),
                            LocalTime.of(timePickerState.hour, timePickerState.minute),
                            zoneId
                        )
                    )
                    isShowingTimePicker = false
                }) {
                    Text(stringResource(R.string.text_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingTimePicker = false }) {
                    Text(stringResource(R.string.text_cancel))
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun AlarmEditor(
    alarm: Alarm,
    zoneId: ZoneId,
    locale: Locale,
    onChange: (Alarm) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var type by rememberSaveable { mutableStateOf(alarm.type) }
    var relativeMinutesTrigger by rememberSaveable {
        mutableStateOf(
            alarm.relativeMinutesTrigger ?: 15
        )
    }

    var isRelativeTriggerTextError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val options = listOf(
            stringResource(R.string.text_at_alarm_type_hint) to AlarmType.ABSOLUTE,
            stringResource(R.string.text_before_alarm_type_hint) to AlarmType.RELATIVE
        )

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { idx, pair ->
                SegmentedButton(
                    selected = pair.second == type,
                    onClick = { type = pair.second },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = idx,
                        count = options.size
                    ),
                    label = { Text(pair.first) })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (type) {
                AlarmType.ABSOLUTE -> {
                    DateTimePicker(
                        label = "",
                        zoneId = zoneId,
                        locale = locale,
                        isDateOnly = false,
                        zonedDateTime = if (alarm.absoluteTrigger == null) ZonedDateTime.now(zoneId)
                            .plusMinutes(15) else ZonedDateTime.ofInstant(
                            alarm.absoluteTrigger,
                            zoneId
                        ),
                        onDateTimeSelected = {
                            onChange(
                                alarm.copy(
                                    type = type,
                                    absoluteTrigger = it.toInstant(),
                                    relativeMinutesTrigger = null
                                )
                            )
                        }
                    )
                }

                AlarmType.RELATIVE -> OutlinedTextField(
                    value = relativeMinutesTrigger.toString(),
                    onValueChange = {
                        if (Pattern.matches("""[1-9][0-9]*""", it)) {
                            relativeMinutesTrigger = it.toLong()
                            isRelativeTriggerTextError = false
                            onChange(
                                alarm.copy(
                                    type = type,
                                    absoluteTrigger = null,
                                    relativeMinutesTrigger = it.toLong()
                                )
                            )
                        } else {
                            isRelativeTriggerTextError = true
                        }
                    },
                    suffix = { Text(stringResource(R.string.text_minute_s)) },
                    supportingText = {
                        if (isRelativeTriggerTextError) {
                            Text(stringResource(R.string.text_non_negative_integer_only_error))
                        }
                    },
                    isError = isRelativeTriggerTextError
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "details",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventEditorScreenPreview() {
    EventEditorScreen(
        editingEvent = null,
        alarmForEditingEvent = emptyArray(),
        defaultDate = LocalDate.now(),
        zoneId = ZoneId.systemDefault(),
        locale = Locale.getDefault(),
        onSave = { _, _ -> },
        onDelete = {},
        onCancel = {})
}

@Preview(showBackground = true)
@Composable
fun EventModificationPreview() {
    val events = remember { sampleEvents().toMutableStateList() }
    val editingEvent by remember { derivedStateOf { events[0] } }
    val alarmsForEditingEvent = remember(editingEvent) { mutableStateListOf<Alarm>() }

    EventEditorScreen(
        editingEvent = editingEvent,
        alarmForEditingEvent = alarmsForEditingEvent.toTypedArray(),
        defaultDate = LocalDate.now(),
        zoneId = ZoneId.systemDefault(),
        locale = Locale.getDefault(),
        onSave = { event, alarms ->
            events.add(event)
            alarmsForEditingEvent.clear()
            alarms.forEach { alarmsForEditingEvent.add(it) }
        },
        onDelete = { events.remove(it) },
        onCancel = { })
}