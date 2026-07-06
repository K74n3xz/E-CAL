package net.k74n3xz.ecal.ui.module.eventedit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.core.text.isDigitsOnly
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.enumeration.alarm.TriggerRelationship
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

private enum class TriggerType { RELATIVE, ABSOLUTE }

@Composable
fun AlarmCardEditComponent(
    titleText: String,
    alarm: Alarm,
    onModify: (Alarm) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Add editors for audio and email actions instead of hiding unsupported alarms.
    if (alarm.action !is Alarm.Action.Display) {
        return
    }

    val timeZone = LocalTimeZone.current

    // Action

    // Action - Display: Description
    val descriptionFieldState = rememberTextFieldState(initialText = (alarm.action as Alarm.Action.Display).description)

    // Trigger

    // Trigger: Trigger Type
    var triggerType by rememberSaveable {
        mutableStateOf(
            when (alarm.trigger) {
                is Alarm.Trigger.RelativeTrigger -> TriggerType.RELATIVE
                is Alarm.Trigger.AbsoluteTrigger -> TriggerType.ABSOLUTE
            }
        )
    }

    // Trigger - RelativeTrigger: Relative To
    var relativeTo by rememberSaveable {
        mutableStateOf(
            when (alarm.trigger) {
                is Alarm.Trigger.RelativeTrigger -> (alarm.trigger as Alarm.Trigger.RelativeTrigger).relativeTo
                is Alarm.Trigger.AbsoluteTrigger -> TriggerRelationship.START
            }
        )
    }

    // Trigger - RelativeTrigger: Offset
    // TODO: Let users enter relative offsets in units other than minutes.
    val offsetFieldState = rememberTextFieldState(
        initialText = when (alarm.trigger) {
            is Alarm.Trigger.RelativeTrigger -> (-(alarm.trigger as Alarm.Trigger.RelativeTrigger).offset.toMinutes()).toString()
            is Alarm.Trigger.AbsoluteTrigger -> 15.toString()
        }
    )
    // TODO: Let users choose whether a relative alarm fires before or after its event boundary.
    val isOffsetFieldError = !offsetFieldState.text.let { it.isNotEmpty() && it.isDigitsOnly() }

    // Trigger - AbsoluteTrigger: At
    var atDate by rememberSaveable {
        mutableStateOf(
            when (alarm.trigger) {
                is Alarm.Trigger.RelativeTrigger -> LocalDateTime.now().plusMinutes(15)
                    .toLocalDate()

                is Alarm.Trigger.AbsoluteTrigger -> (alarm.trigger as Alarm.Trigger.AbsoluteTrigger).at.atZone(timeZone).toLocalDate()
            }
        )
    }
    var atTime by rememberSaveable {
        mutableStateOf(
            when (alarm.trigger) {
                is Alarm.Trigger.RelativeTrigger -> LocalDateTime.now().plusMinutes(15)
                    .toLocalTime()

                is Alarm.Trigger.AbsoluteTrigger -> (alarm.trigger as Alarm.Trigger.AbsoluteTrigger).at.atZone(timeZone).toLocalTime()
            }
        )
    }

    // TODO: interval & repeat - Add UI for alarm repeat interval and repeat count.

    LaunchedEffect(
        descriptionFieldState.text,
        triggerType,
        relativeTo,
        offsetFieldState.text,
        atDate,
        atTime
    ) {
        if (triggerType == TriggerType.RELATIVE && isOffsetFieldError) {
            return@LaunchedEffect
        }

        val newAction = Alarm.Action.Display(description = descriptionFieldState.text.toString())
        val newTrigger = when (triggerType) {
            TriggerType.RELATIVE -> Alarm.Trigger.RelativeTrigger(
                relativeTo = relativeTo,
                offset = Duration.ofMinutes(-offsetFieldState.text.toString().toLong())
            )

            TriggerType.ABSOLUTE -> Alarm.Trigger.AbsoluteTrigger(
                at = ZonedDateTime.of(atDate, atTime, timeZone).toInstant()
            )
        }

        onModify(
            alarm.copy(
                action = newAction,
                trigger = newTrigger
            )
        )
    }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            // TODO: Action: Action Type - Add an action type selector when multiple alarm actions are editable.

            // Action - Display: Description
            OutlinedTextField(
                state = descriptionFieldState,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger

            // Trigger: Trigger Type
            // TODO: Replace the segmented row with a connected button group when available.
            SingleChoiceSegmentedButtonRow {
                triggerTypeOptions.forEachIndexed { index, pair ->
                    SegmentedButton(
                        selected = triggerType == pair.first,
                        onClick = { triggerType = pair.first },
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

            when (triggerType) {
                TriggerType.RELATIVE -> {
                    // Trigger - RelativeTrigger: Relative To
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                            text = triggerRelationshipOptions.getValue(relativeTo),
                            items = triggerRelationshipOptions,
                            onItemSelect = { relativeTo = it },
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trigger - RelativeTrigger: Offest
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_alarm_offset_label),
                            modifier = Modifier.weight(0.4f),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            state = offsetFieldState,
                            modifier = Modifier.weight(0.6f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            suffix = {
                                Text(
                                    text = stringResource(R.string.text_minute_s),
                                    color = Color.Unspecified,
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingText = {
                                if (isOffsetFieldError) {
                                    Text(
                                        text = stringResource(R.string.text_non_negative_integer_only_error),
                                        color = Color.Unspecified,
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            },
                            isError = isOffsetFieldError,
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

                TriggerType.ABSOLUTE -> {
                    // Trigger - AbsoluteTrigger: At
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.text_alarm_at_label),
                            modifier = Modifier.weight(0.125f),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.weight(0.875f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateFieldComponent(
                                date = atDate,
                                onPickDate = { atDate = it },
                                modifier = Modifier.weight(0.62f)
                            )

                            TimeFieldComponent(
                                time = atTime,
                                onPickTime = { atTime = it },
                                modifier = Modifier.weight(0.38f)
                            )
                        }
                    }
                }
            }

            // TODO: interval & repeat - Add controls for alarm repeat interval and repeat count.
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmCardEditComponentPreview() {
    var alarm by remember {
        mutableStateOf(
            Alarm(
                id = null,
                action = Alarm.Action.Display("This is an example description."),
                trigger = Alarm.Trigger.RelativeTrigger(
                    relativeTo = TriggerRelationship.START,
                    offset = Duration.ofMinutes(-30)
                )
            )
        )
    }

    CompositionLocalProvider(LocalTimeZone provides ZoneId.systemDefault()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = alarm.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                AlarmCardEditComponent(
                    titleText = "Alarm",
                    alarm = alarm,
                    onModify = { alarm = it },
                    onRemove = {}
                )
            }
        }
    }
}