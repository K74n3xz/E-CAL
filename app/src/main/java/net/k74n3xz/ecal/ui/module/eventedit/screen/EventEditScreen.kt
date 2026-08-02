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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventStatus
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.model.property.event.TimeTransparency
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import net.k74n3xz.ecal.ui.module.eventedit.component.AlarmCardEditComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.ComboBoxComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.DateFieldComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.DurationFieldComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.EventTextFieldComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.PeriodFieldComponent
import net.k74n3xz.ecal.ui.module.eventedit.component.TimeFieldComponent
import net.k74n3xz.ecal.ui.presentation.form.AlarmForm
import net.k74n3xz.ecal.ui.presentation.form.EventForm
import net.k74n3xz.ecal.ui.presentation.form.enumeration.event.TimingMode
import net.k74n3xz.ecal.ui.presentation.form.error.DateTimeFieldError
import net.k74n3xz.ecal.ui.presentation.form.utils.toAlarmForm
import net.k74n3xz.ecal.ui.presentation.form.utils.toEventForm
import net.k74n3xz.ecal.ui.utils.generateEventUid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventEditScreen(
    eventForm: EventForm,
    alarmForms: List<AlarmForm>,
    audioAttachments: List<Attachment>,
    attendees: List<Attendee>,
    attachments: List<Attachment>,
    onAddAlarm: () -> Unit,
    onRemoveAlarm: (Int) -> Unit,
    onCancel: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
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
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val timingModeOptions = listOf(
                TimingMode.POINT to stringResource(R.string.text_timing_mode_point),
                TimingMode.RANGE to stringResource(R.string.text_timing_mode_range),
                TimingMode.DURATION to stringResource(R.string.text_timing_mode_duration)
            )
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
                textFieldState = eventForm.summary,
                isClear = eventForm.isSummaryClear,
                onClear = { eventForm.isSummaryClear = true },
                onDirty = { eventForm.isSummaryClear = false },
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
                textFieldState = eventForm.description,
                isClear = eventForm.isDescriptionClear,
                onClear = { eventForm.isDescriptionClear = true },
                onDirty = { eventForm.isDescriptionClear = false },
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
                textFieldState = eventForm.location,
                isClear = eventForm.isLocationClear,
                onClear = { eventForm.isLocationClear = true },
                onDirty = { eventForm.isLocationClear = false },
                labelText = stringResource(R.string.text_field_label_location),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = defaultKeyboardOptions.copy(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Time:
            // Time - Mode
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                timingModeOptions.forEachIndexed { index, pair ->
                    SegmentedButton(
                        selected = eventForm.timingMode == pair.first,
                        onClick = { eventForm.timingMode = pair.first },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = timingModeOptions.size
                        )
                    ) {
                        Text(
                            text = pair.second,
                            color = Color.Unspecified,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

                Column(
                    modifier = Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    DateFieldComponent(
                        date = eventForm.startDate,
                        onPickDate = { eventForm.startDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(!eventForm.isAllDay) {
                        Column(
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TimeFieldComponent(
                                time = eventForm.startTime,
                                onPickTime = { eventForm.startTime = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time - End At
            AnimatedVisibility(eventForm.timingMode == TimingMode.RANGE) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

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

                        Column(
                            modifier = Modifier.weight(0.8f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            DateFieldComponent(
                                date = eventForm.endDate,
                                onPickDate = { eventForm.endDate = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(!eventForm.isAllDay) {
                                Column(
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    TimeFieldComponent(
                                        time = eventForm.endTime,
                                        onPickTime = { eventForm.endTime = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Time - Duration
            AnimatedVisibility(eventForm.timingMode == TimingMode.DURATION) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_duration_label),
                            modifier = Modifier.weight(0.2f),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (eventForm.isAllDay) {
                            PeriodFieldComponent(
                                period = eventForm.period,
                                onSave = { eventForm.period = it },
                                modifier = Modifier.weight(0.8f)
                            )
                        } else {
                            DurationFieldComponent(
                                duration = eventForm.duration,
                                onSave = { eventForm.duration = it },
                                modifier = Modifier.weight(0.8f)
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
                Text(
                    text = stringResource(R.string.switch_all_day_description),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                Switch(
                    checked = eventForm.isAllDay,
                    onCheckedChange = { eventForm.isAllDay = it }
                )
            }

            // Time - Error
            AnimatedVisibility(eventForm.timingFieldError != null) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (eventForm.timingFieldError) {
                            DateTimeFieldError.TooLongPeriod ->
                                stringResource(R.string.error_event_duration_too_long)

                            DateTimeFieldError.EndBeforeStart ->
                                stringResource(R.string.error_event_end_before_start)

                            DateTimeFieldError.EndNotAfterStart ->
                                stringResource(R.string.error_event_end_not_after_start)

                            null -> ""
                        },
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))

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
                    text = eventForm.priority?.let { priorities[it] } ?: "",
                    items = priorities,
                    onItemSelect = { eventForm.priority = it },
                    modifier = Modifier.weight(0.6f),
                    canClear = eventForm.priority != null,
                    onClear = { eventForm.priority = null }
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
                    text = eventForm.timeTransparency?.let { timeTransparencyOptions[it] } ?: "",
                    items = timeTransparencyOptions,
                    onItemSelect = { eventForm.timeTransparency = it },
                    modifier = Modifier.weight(0.6f),
                    canClear = eventForm.timeTransparency != null,
                    onClear = { eventForm.timeTransparency = null }
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
                    text = eventForm.status?.let { statusOptions[it] } ?: "",
                    items = statusOptions,
                    onItemSelect = { eventForm.status = it },
                    modifier = Modifier.weight(0.6f),
                    canClear = eventForm.status != null,
                    onClear = { eventForm.status = null }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))

            Text(
                text = stringResource(R.string.text_alarms),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium
            )

            // Alarms
            alarmForms.forEachIndexed { index, alarm ->
                // TODO: Animate alarm cards when reminders are added or removed.
                Spacer(modifier = Modifier.height(16.dp))

                AlarmCardEditComponent(
                    titleText = stringResource(R.string.text_alarm_title_numbered, index + 1),
                    alarmForm = alarm,
                    audioAttachments = audioAttachments,
                    attendees = attendees,
                    attachments = attachments,
                    onRemove = { onRemoveAlarm(index) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onAddAlarm) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.button_content_description_add_alarm)
                )
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
                onClick = onSave,
                enabled = canSave
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
    val timeZone = ZoneId.systemDefault()

    val eventUiModel = remember {
        Event(
            uid = generateEventUid(),
            schedule = EventTiming.Timed.InstantTiming(Instant.now())
        ).toEventForm(timeZone)
    }
    val alarmUiModels = remember { mutableStateListOf<AlarmForm>() }

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        Surface(modifier = Modifier.fillMaxSize()) {
            EventEditScreen(
                eventForm = eventUiModel,
                alarmForms = alarmUiModels,
                audioAttachments = emptyList(),
                attendees = emptyList(),
                attachments = emptyList(),
                onAddAlarm = {
                    alarmUiModels.add(
                        Alarm(
                            action = Action.Display(""),
                            trigger = Trigger.RelativeTrigger(
                                relativeTo = TriggerRelationship.START,
                                offset = Duration.ofMinutes(-15)
                            )
                        ).toAlarmForm(timeZone)
                    )
                },
                onRemoveAlarm = { alarmUiModels.removeAt(it) },
                onCancel = {},
                canSave = eventUiModel.isValid && alarmUiModels.all { it.isValid },
                onSave = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventEditScreenPreview2() {
    val timeZone = ZoneId.systemDefault()

    val eventUiModel = remember {
        Event(
            uid = generateEventUid(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            summary = "Team Standup",
            description = "Daily sync",
            location = "Conference Room A",
            schedule = EventTiming.Timed.RangeTiming(
                startAt = Instant.now().plusSeconds(3600),
                endAt = Instant.now().plusSeconds(3600 * 2)
            ),
            status = EventStatus.CONFIRMED
        ).toEventForm(timeZone)
    }
    val alarmUiModels = remember {
        mutableStateListOf(
            Alarm(
                action = Action.Display(""),
                trigger = Trigger.RelativeTrigger(
                    relativeTo = TriggerRelationship.START,
                    offset = Duration.ofMinutes(-15)
                )
            ).toAlarmForm(timeZone)
        )
    }

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        Surface(modifier = Modifier.fillMaxSize()) {
            EventEditScreen(
                eventForm = eventUiModel,
                alarmForms = alarmUiModels,
                audioAttachments = listOf(
                    Attachment(
                        id = 1L,
                        description = "An MP3 music sample",
                        name = "summer-melody.mp3",
                        mimeType = "audio/mpeg",
                        sizeBytes = 4_826_112L
                    ),
                    Attachment(
                        id = 2L,
                        description = "A high-quality WAV recording",
                        name = "studio-recording.wav",
                        mimeType = "audio/wav",
                        sizeBytes = 18_345_984L
                    ),
                    Attachment(
                        id = 3L,
                        description = "An Ogg Vorbis audio sample",
                        name = "forest-ambience.ogg",
                        mimeType = "audio/ogg",
                        sizeBytes = 6_291_456L
                    ),
                    Attachment(
                        id = 4L,
                        description = "A lossless FLAC music track",
                        name = "piano-performance.flac",
                        mimeType = "audio/flac",
                        sizeBytes = 24_117_248L
                    ),
                    Attachment(
                        id = 5L,
                        description = "An AAC podcast episode",
                        name = "technology-podcast.aac",
                        mimeType = "audio/aac",
                        sizeBytes = 9_437_184L
                    ),
                    Attachment(
                        id = 6L,
                        description = "An audio file stored in an MP4 container",
                        name = "interview-audio.m4a",
                        mimeType = "audio/mp4",
                        sizeBytes = 7_864_320L
                    ),
                    Attachment(
                        id = 7L,
                        description = "A WebM voice recording",
                        name = "voice-message.webm",
                        mimeType = "audio/webm",
                        sizeBytes = 1_572_864L
                    ),
                    Attachment(
                        id = 8L,
                        description = "A MIDI instrumental sequence",
                        name = "digital-composition.mid",
                        mimeType = "audio/midi",
                        sizeBytes = 84_992L
                    ),
                    Attachment(
                        id = 9L,
                        description = "An Opus speech sample",
                        name = "conference-speech.opus",
                        mimeType = "audio/opus",
                        sizeBytes = 2_359_296L
                    )
                ),
                attendees = listOf(
                    Attendee(
                        id = 1L,
                        name = "Alice Johnson",
                        description = "Product manager responsible for the mobile application.",
                        email = "alice.johnson@example.com"
                    ),
                    Attendee(
                        id = 2L,
                        name = "Brian Smith",
                        description = "Backend engineer specializing in Kotlin and Spring Boot.",
                        email = "brian.smith@example.com"
                    ),
                    Attendee(
                        id = 3L,
                        name = "Catherine Lee",
                        description = "UI designer focused on accessible user experiences.",
                        email = "catherine.lee@example.com"
                    ),
                    Attendee(
                        id = 4L,
                        name = "Daniel Brown",
                        description = "Data analyst presenting the quarterly performance report.",
                        email = "daniel.brown@example.com"
                    ),
                    Attendee(
                        id = 5L,
                        name = "Emma Wilson",
                        description = "Guest speaker discussing modern software architecture.",
                        email = "emma.wilson@example.com"
                    ),
                    Attendee(
                        id = 6L,
                        name = "Frank Miller",
                        description = null,
                        email = "frank.miller@example.com"
                    ),
                    Attendee(
                        id = 7L,
                        name = null,
                        description = "An attendee who chose not to provide a display name.",
                        email = "anonymous.attendee@example.com"
                    ),
                    Attendee(
                        id = 8L,
                        name = "Grace Taylor",
                        description = "New attendee whose registration is still being processed.",
                        email = "grace.taylor@example.com"
                    )
                ),
                attachments = listOf(
                    Attachment(
                        id = 1L,
                        description = "Quarterly financial report for the management team.",
                        name = "financial_report_q2.pdf",
                        mimeType = "application/pdf",
                        sizeBytes = 2_458_624L
                    ),
                    Attachment(
                        id = 2L,
                        description = "Profile photo uploaded by the user.",
                        name = "profile_photo.jpg",
                        mimeType = "image/jpeg",
                        sizeBytes = 845_312L
                    ),
                    Attachment(
                        id = 3L,
                        description = "Product demonstration video.",
                        name = "product_demo.mp4",
                        mimeType = "video/mp4",
                        sizeBytes = 18_742_560L
                    ),
                    Attachment(
                        id = 4L,
                        description = "Customer contact information exported as a spreadsheet.",
                        name = "customer_contacts.xlsx",
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        sizeBytes = 326_144L
                    ),
                    Attachment(
                        id = 5L,
                        description = "Meeting notes recorded during the project review.",
                        name = "project_review_notes.txt",
                        mimeType = "text/plain",
                        sizeBytes = 12_480L
                    ),
                    Attachment(
                        id = 6L,
                        description = null,
                        name = "archive.zip",
                        mimeType = "application/zip",
                        sizeBytes = 9_830_400L
                    ),
                    Attachment(
                        id = 7L,
                        description = "A newly selected file that has not been uploaded yet.",
                        name = "draft_proposal.docx",
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        sizeBytes = 1_204_736L
                    )
                ),
                onAddAlarm = {
                    alarmUiModels.add(
                        Alarm(
                            action = Action.Display(""),
                            trigger = Trigger.RelativeTrigger(
                                relativeTo = TriggerRelationship.START,
                                offset = Duration.ofMinutes(-15)
                            )
                        ).toAlarmForm(timeZone)
                    )
                },
                onRemoveAlarm = { alarmUiModels.removeAt(it) },
                onCancel = {},
                canSave = eventUiModel.isValid && alarmUiModels.all { it.isValid },
                onSave = {}
            )
        }
    }
}
