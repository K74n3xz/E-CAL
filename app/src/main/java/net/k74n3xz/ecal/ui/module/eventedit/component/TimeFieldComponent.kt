package net.k74n3xz.ecal.ui.module.eventedit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.k74n3xz.ecal.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFieldComponent(
    time: LocalTime,
    onPickTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = true  // TODO: Use the system time format setting as the default.
) {
    val timeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)  // TODO: Format displayed time according to is24Hour.

    var isShowingDialog by remember { mutableStateOf(false) }
    var isUsingPicker by remember { mutableStateOf(true) }

    OutlinedTextField(
        value = timeFormatter.format(time),
        onValueChange = {},
        modifier = modifier,
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
            IconButton(onClick = { isShowingDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,  // TODO: Use a time-specific edit icon when one is available.
                    contentDescription = stringResource(R.string.text_field_trailing_icon_content_description_select_time)
                )
            }
        },
        singleLine = true
    )

    if (isShowingDialog) {
        val timePickerState = TimePickerState(time.hour, time.minute, is24Hour)

        TimePickerDialog(
            title = { /* TODO: Add a localized dialog title for time selection. */ },
            onDismissRequest = { isShowingDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onPickTime(time)
                        isShowingDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_ok),
                        color = Color.Unspecified,
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingDialog = false }) {
                    Text(
                        text = stringResource(R.string.text_cancel),
                        color = Color.Unspecified,
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current
                    )
                }
            },
            modeToggleButton = {
                IconButton(onClick = { isUsingPicker = !isUsingPicker }) {
                    if (isUsingPicker) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.button_content_description_switch_to_text_input)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.button_content_description_switch_to_clock_picker)
                        )
                    }
                }
            }
        ) {
            if (isUsingPicker) {
                TimePicker(timePickerState)
            } else {
                TimeInput(timePickerState)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeFieldComponentPreview() {
    var time by remember { mutableStateOf(LocalTime.now()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time: $time",
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall
            )
            TimeFieldComponent(
                time = time,
                onPickTime = { time = it }
            )
        }
    }
}