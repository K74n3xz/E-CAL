package net.k74n3xz.ecal.ui.module.eventedit.component

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneId
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import net.k74n3xz.ecal.ui.presentation.form.AlarmForm
import net.k74n3xz.ecal.ui.presentation.form.enumeration.alarm.ActionType
import net.k74n3xz.ecal.ui.presentation.form.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.ui.presentation.form.error.ContainerFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.DurationFieldError
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError
import net.k74n3xz.ecal.ui.presentation.form.utils.toAlarmForm

private enum class BottomSheetType { ATTENDEE, ATTACHMENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmCardEditComponent(
    titleText: String,
    alarmForm: AlarmForm,
    audioAttachments: List<Attachment>,
    attendees: List<Attendee>,
    attachments: List<Attachment>,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bottomSheetType by remember { mutableStateOf<BottomSheetType?>(null) }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val actionTypeOptions = listOf(
                ActionType.AUDIO to stringResource(R.string.text_alarm_action_audio),
                ActionType.DISPLAY to stringResource(R.string.text_alarm_action_display),
                ActionType.EMAIL to stringResource(R.string.text_alarm_action_email)
            )
            val triggerTypeOptions = listOf(
                TriggerType.RELATIVE to stringResource(R.string.text_before_alarm_type_hint),
                TriggerType.ABSOLUTE to stringResource(R.string.text_at_alarm_type_hint)
            )
            val triggerRelationshipOptions = mapOf(
                TriggerRelationship.START to stringResource(R.string.text_alarm_trigger_start),
                TriggerRelationship.END to stringResource(R.string.text_alarm_trigger_end)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    color = Color.Unspecified,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.button_content_description_delete)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action

            // Action: Action Type
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                actionTypeOptions.forEachIndexed { index, pair ->
                    SegmentedButton(
                        selected = alarmForm.actionType == pair.first,
                        onClick = { alarmForm.actionType = pair.first },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = actionTypeOptions.size
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

            Spacer(modifier = Modifier.height(8.dp))

            // Action - Audio
            AnimatedVisibility(alarmForm.actionType == ActionType.AUDIO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment
                    Text(
                        text = stringResource(R.string.text_attachment_label),
                        modifier = Modifier.weight(0.2f),
                        color = Color.Unspecified,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ComboBoxComponent(
                        text = alarmForm.audioAttachment?.name ?: stringResource(R.string.text_default_audio),
                        items = audioAttachments.associateWith { it.name },
                        onItemSelect = { alarmForm.audioAttachment = it },
                        modifier = Modifier.weight(0.8f),
                        canClear = alarmForm.audioAttachment != null,
                        onClear = { alarmForm.audioAttachment = null }
                    )
                }
            }

            // Action - Display
            AnimatedVisibility(alarmForm.actionType == ActionType.DISPLAY) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Description
                    OutlinedTextField(
                        state = alarmForm.description,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        labelPosition = TextFieldLabelPosition.Attached(),
                        label = {
                            Text(
                                text = stringResource(R.string.text_field_label_description),
                                color = Color.Unspecified,
                                textAlign = TextAlign.Center,
                                style = LocalTextStyle.current
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            showKeyboardOnFocus = true
                        )
                    )
                }
            }

            // Action - Email
            AnimatedVisibility(alarmForm.actionType == ActionType.EMAIL) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Summary
                    OutlinedTextField(
                        state = alarmForm.summary,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        labelPosition = TextFieldLabelPosition.Attached(),
                        label = {
                            Text(
                                text = stringResource(R.string.text_field_label_summary),
                                color = Color.Unspecified,
                                textAlign = TextAlign.Center,
                                style = LocalTextStyle.current
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            showKeyboardOnFocus = true
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    OutlinedTextField(
                        state = alarmForm.descriptionEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        labelPosition = TextFieldLabelPosition.Attached(),
                        label = {
                            Text(
                                text = stringResource(R.string.text_field_label_description),
                                color = Color.Unspecified,
                                textAlign = TextAlign.Center,
                                style = LocalTextStyle.current
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            showKeyboardOnFocus = true
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Attendee
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.text_attendees_label),
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            alarmForm.attendees.forEach {
                                Text(
                                    text = stringResource(
                                        R.string.text_attendee_with_email,
                                        it.name ?: stringResource(R.string.text_no_name_hint),
                                        it.email
                                    ),
                                    color = Color.Unspecified,
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        AnimatedVisibility(alarmForm.attendeesFieldError != null) {
                            Column(
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = when (alarmForm.attendeesFieldError) {
                                        ContainerFieldError.Empty ->
                                            stringResource(R.string.error_at_least_one_attendee)

                                        null -> ""
                                    },
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.labelSmall
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        TextButton(onClick = { bottomSheetType = BottomSheetType.ATTENDEE }) {
                            Text(
                                text = stringResource(R.string.text_edit),
                                color = Color.Unspecified,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Attachment
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.text_attachments),
                                color = Color.Unspecified,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Switch(
                                checked = alarmForm.hasAttachments,
                                onCheckedChange = { alarmForm.hasAttachments = it }
                            )
                        }

                        AnimatedVisibility(alarmForm.hasAttachments) {
                            Column(
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    alarmForm.attachments.forEach {
                                        Text(
                                            text = stringResource(
                                                R.string.text_attachment_file_metadata,
                                                it.name,
                                                it.mimeType,
                                                Formatter.formatShortFileSize(LocalContext.current, it.sizeBytes)
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color.Unspecified,
                                            textAlign = TextAlign.Start,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                TextButton(onClick = { bottomSheetType = BottomSheetType.ATTACHMENT }) {
                                    Text(
                                        text = stringResource(R.string.text_edit),
                                        color = Color.Unspecified,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

            // Trigger

            // Trigger: Trigger Type
            SingleChoiceSegmentedButtonRow {
                triggerTypeOptions.forEachIndexed { index, pair ->
                    SegmentedButton(
                        selected = alarmForm.triggerType == pair.first,
                        onClick = { alarmForm.triggerType = pair.first },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = triggerTypeOptions.size
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

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger - RelativeTrigger
            AnimatedVisibility(alarmForm.triggerType == TriggerType.RELATIVE) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Relative To
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_alarm_relative_to_label),
                            modifier = Modifier.weight(0.4f),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        ComboBoxComponent(
                            text = triggerRelationshipOptions.getValue(alarmForm.relativeTo),
                            items = triggerRelationshipOptions,
                            onItemSelect = { alarmForm.relativeTo = it },
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Offest
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_alarm_offset_label),
                            modifier = Modifier.weight(0.4f),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        DurationFieldComponent(
                            duration = alarmForm.offset,
                            onSave = { alarmForm.offset = it },
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }

            // Trigger - AbsoluteTrigger
            AnimatedVisibility(alarmForm.triggerType == TriggerType.ABSOLUTE) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // At
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_alarm_at_label),
                            modifier = Modifier.weight(0.4f),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Column(
                            modifier = Modifier.weight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            DateFieldComponent(
                                date = alarmForm.atDate,
                                onPickDate = { alarmForm.atDate = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            TimeFieldComponent(
                                time = alarmForm.atTime,
                                onPickTime = { alarmForm.atTime = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

            // Interval & Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_repetition),
                    color = Color.Unspecified,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                Switch(
                    checked = alarmForm.isRepetitionEnabled,
                    onCheckedChange = { alarmForm.isRepetitionEnabled = it }
                )
            }

            AnimatedVisibility(alarmForm.isRepetitionEnabled) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Interval
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_interval_label),
                            modifier = Modifier.weight(0.4f),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        DurationFieldComponent(
                            duration = alarmForm.interval,
                            onSave = { alarmForm.interval = it },
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    AnimatedVisibility(alarmForm.intervalFieldError != null) {
                        Column(
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when (alarmForm.intervalFieldError) {
                                    DurationFieldError.NotPositive ->
                                        stringResource(R.string.error_repetition_interval_not_positive)

                                    null -> ""
                                },
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.labelSmall
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Repeat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_repeat_count_label),
                            modifier = Modifier.weight(0.4f),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            state = alarmForm.repeat,
                            modifier = Modifier.weight(0.6f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            supportingText = {
                                alarmForm.repeatFieldError.let {
                                    if (it != null) {
                                        Text(
                                            text = when (it) {
                                                is NumberTextFieldError.Empty,
                                                is NumberTextFieldError.InvalidCharacter ->
                                                    stringResource(R.string.text_non_negative_integer_only_error)

                                                is NumberTextFieldError.TooBigNumber ->
                                                    stringResource(R.string.error_number_too_large)
                                            },
                                            color = Color.Unspecified,
                                            textAlign = TextAlign.Start,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            },
                            isError = alarmForm.repeatFieldError != null,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = true,
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                                showKeyboardOnFocus = true
                            ),
                            lineLimits = TextFieldLineLimits.SingleLine
                        )
                    }
                }
            }
        }
    }

    when (bottomSheetType) {
        BottomSheetType.ATTENDEE -> ModalBottomSheet(onDismissRequest = { bottomSheetType = null }) {
            if (attendees.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_attendees),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        count = attendees.size,
                        key = { requireNotNull(attendees[it].id) }
                    ) {
                        val element = attendees[it]
                        AttendeeSelectionCard(
                            attendee = element,
                            isSelected = alarmForm.attendees.contains(element),
                            onSelectedChange = { boolean ->
                                if (boolean) {
                                    alarmForm.attendees.add(element)
                                } else {
                                    alarmForm.attendees.remove(element)
                                }
                            }
                        )
                    }
                }
            }
        }

        BottomSheetType.ATTACHMENT -> ModalBottomSheet(onDismissRequest = { bottomSheetType = null }) {
            if (attachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.text_no_attachments),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        count = attachments.size,
                        key = { requireNotNull(attachments[it].id) }
                    ) {
                        val element = attachments[it]
                        AttachmentSelectionCard(
                            attachment = element,
                            isSelected = alarmForm.attachments.contains(element),
                            onSelectedChange = { boolean ->
                                if (boolean) {
                                    alarmForm.attachments.add(element)
                                } else {
                                    alarmForm.attachments.remove(element)
                                }
                            }
                        )
                    }
                }
            }
        }

        null -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmCardEditComponentPreview() {
    val timeZone = ZoneId.systemDefault()

    val alarmUiModel = remember {
        Alarm(
            id = null,
            action = Action.Display("This is an example description."),
            trigger = Trigger.RelativeTrigger(
                relativeTo = TriggerRelationship.START,
                offset = Duration.ofMinutes(-30)
            )
        ).toAlarmForm(timeZone)
    }

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            AlarmCardEditComponent(
                titleText = "Alarm",
                alarmForm = alarmUiModel,
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
                onRemove = {}
            )
        }
    }
}
