package net.k74n3xz.ecal.ui.module.eventedit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.k74n3xz.ecal.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFieldComponent(
    date: LocalDate,
    onPickDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val utcZoneId = ZoneId.of("UTC")
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    var isShowingDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = dateFormatter.format(date),
        onValueChange = {},
        modifier = modifier,
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
            IconButton(onClick = { isShowingDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = stringResource(R.string.text_field_trailing_icon_content_description_select_date)
                )
            }
        },
        singleLine = true
    )

    if (isShowingDialog) {
        val datePickerState = DatePickerState(
            locale = LocalLocale.current.platformLocale,
            initialSelectedDate = date
        )

        DatePickerDialog(
            onDismissRequest = { isShowingDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), utcZoneId)
                            onPickDate(date)
                        }
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
            modifier = Modifier.fillMaxSize()
            // NOTE: DatePickerDialog intentionally allows its content to collapse when
            // DatePicker switches between picker/input modes via weight(fill = false).
            // Filling the dialog content stabilizes the outer measurement space, so the
            // internal DatePicker height animation does not cause visible dialog jumps.
        ) {
            DatePicker(datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DateFieldComponentPreview() {
    var date by remember { mutableStateOf(LocalDate.now()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Date: $date",
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall
            )
            DateFieldComponent(
                date = date,
                onPickDate = { date = it }
            )
        }
    }
}