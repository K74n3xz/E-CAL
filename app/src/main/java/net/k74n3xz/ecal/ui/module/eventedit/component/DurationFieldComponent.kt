package net.k74n3xz.ecal.ui.module.eventedit.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.util.Locale
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.ui.presentation.form.DurationForm
import net.k74n3xz.ecal.ui.presentation.form.enumeration.common.Sign
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError

@Composable
internal fun DurationFieldComponent(duration: Duration, onSave: (Duration) -> Unit, modifier: Modifier = Modifier) {
    val durationText = formatDuration(LocalContext.current, LocalLocale.current.platformLocale, duration)

    val durationForm = remember { DurationForm(duration) }
    var isShowingDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = durationText,
        onValueChange = {},
        modifier = modifier,
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
            IconButton(onClick = { isShowingDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.button_content_description_edit)
                )
            }
        },
        singleLine = true
    )

    if (isShowingDialog) {
        AlertDialog(
            onDismissRequest = { isShowingDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        durationForm.resolve().let {
                            if (it.isSuccess) {
                                onSave(it.getOrThrow())
                            }
                            isShowingDialog = false
                        }
                    },
                    enabled = durationForm.isValid
                ) {
                    Text(
                        text = stringResource(R.string.text_save),
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
            text = {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val signOptions = listOf(
                        Sign.NEGATIVE to stringResource(R.string.text_before_alarm_type_hint),
                        Sign.POSITIVE to stringResource(R.string.text_after_alarm_type_hint)
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        signOptions.forEachIndexed { index, pair ->
                            SegmentedButton(
                                selected = durationForm.sign == pair.first,
                                onClick = { durationForm.sign = pair.first },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = signOptions.size
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

                    Spacer(modifier = Modifier.height(24.dp))

                    listOf(
                        Triple(
                            durationForm.daysField,
                            durationForm.daysFieldError,
                            stringResource(R.string.unit_days)
                        ),
                        Triple(
                            durationForm.hoursField,
                            durationForm.hoursFieldError,
                            stringResource(R.string.unit_hours)
                        ),
                        Triple(
                            durationForm.minutesField,
                            durationForm.minutesFieldError,
                            stringResource(R.string.unit_minutes)
                        ),
                        Triple(
                            durationForm.secondsField,
                            durationForm.secondsFieldError,
                            stringResource(R.string.unit_seconds)
                        ),
                        Triple(
                            durationForm.millisField,
                            durationForm.millisFieldError,
                            stringResource(R.string.unit_milliseconds)
                        ),
                        Triple(
                            durationForm.nanosField,
                            durationForm.nanosFieldError,
                            stringResource(R.string.unit_nanoseconds)
                        )
                    ).forEach {
                        DurationPartField(it.first, it.second, it.third)
                    }
                }
            }
        )
    }
}

@Composable
private fun DurationPartField(
    textFieldState: TextFieldState,
    textFieldError: NumberTextFieldError?,
    temporalUnitText: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        state = textFieldState,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        suffix = {
            Text(
                text = temporalUnitText,
                color = Color.Unspecified,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingText = {
            if (textFieldError != null) {
                Text(
                    text = when (textFieldError) {
                        is NumberTextFieldError.InvalidCharacter ->
                            stringResource(R.string.text_non_negative_integer_only_error)

                        is NumberTextFieldError.TooBigNumber ->
                            stringResource(R.string.error_number_too_large)

                        else -> stringResource(R.string.error_invalid_input)
                    },
                    color = Color.Unspecified,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        isError = textFieldError != null,
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

private fun formatDuration(context: Context, locale: Locale, duration: Duration): String {
    if (duration.isZero) {
        return context.resources.getQuantityString(R.plurals.duration_seconds, 0, 0)
    }

    val absoluteDuration = duration.abs()
    val resultParts = mutableListOf<String>()
    absoluteDuration.toDaysPart().takeIf { it != 0L }?.let {
        resultParts.add(
            context.resources.getQuantityString(
                R.plurals.duration_days,
                it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                it
            )
        )
    }
    absoluteDuration.toHoursPart().takeIf { it != 0 }?.let {
        resultParts.add(context.resources.getQuantityString(R.plurals.duration_hours, it, it))
    }
    absoluteDuration.toMinutesPart().takeIf { it != 0 }?.let {
        resultParts.add(context.resources.getQuantityString(R.plurals.duration_minutes, it, it))
    }
    val seconds = absoluteDuration.toSecondsPart()
    val nanos = absoluteDuration.toNanosPart()
    if (seconds != 0 || nanos != 0) {
        if (nanos == 0) {
            resultParts.add(context.resources.getQuantityString(R.plurals.duration_seconds, seconds, seconds))
        } else {
            val fraction = nanos.toString().padStart(9, '0').trimEnd('0')
            val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
            resultParts.add(
                context.resources.getString(R.string.text_fractional_seconds, "$seconds$decimalSeparator$fraction")
            )
        }
    }

    return context.resources.getString(
        if (duration.isNegative) R.string.text_duration_before else R.string.text_duration_after,
        resultParts.joinToString(context.resources.getString(R.string.text_temporal_parts_separator))
    )
}

@Preview(showBackground = true)
@Composable
private fun DurationFieldComponentPreview() {
    var duration by remember { mutableStateOf(Duration.ofMinutes(15)) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DurationFieldComponent(
            duration = duration,
            onSave = { duration = it }
        )
    }
}
