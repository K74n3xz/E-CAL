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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Period
import kotlin.math.absoluteValue
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.ui.presentation.form.PeriodForm
import net.k74n3xz.ecal.ui.presentation.form.enumeration.common.Sign
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError

@Composable
internal fun PeriodFieldComponent(period: Period, onSave: (Period) -> Unit, modifier: Modifier = Modifier) {
    val periodText = formatPeriod(LocalContext.current, period)

    val periodForm = remember { PeriodForm(period) }
    var isShowingDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = periodText,
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
                        periodForm.resolve().let {
                            if (it.isSuccess) {
                                onSave(it.getOrThrow())
                            }
                            isShowingDialog = false
                        }
                    },
                    enabled = periodForm.isValid
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
                                selected = periodForm.yearMonthSign == pair.first,
                                onClick = { periodForm.yearMonthSign = pair.first },
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

                    PeriodPartField(
                        periodForm.yearsField,
                        periodForm.yearsFieldError,
                        stringResource(R.string.unit_years)
                    )

                    PeriodPartField(
                        periodForm.monthsField,
                        periodForm.monthsFieldError,
                        stringResource(R.string.unit_months)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        signOptions.forEachIndexed { index, pair ->
                            SegmentedButton(
                                selected = periodForm.daySign == pair.first,
                                onClick = { periodForm.daySign = pair.first },
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

                    PeriodPartField(
                        periodForm.daysField,
                        periodForm.daysFieldError,
                        stringResource(R.string.unit_days)
                    )
                }
            }
        )
    }
}

@Composable
private fun PeriodPartField(
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

private fun formatPeriod(context: Context, period: Period): String {
    if (period.isZero) {
        return context.resources.getQuantityString(R.plurals.period_days, 0, 0)
    }

    val separator = context.resources.getString(R.string.text_temporal_parts_separator)

    val values = listOf(
        period.years to R.plurals.period_years,
        period.months to R.plurals.period_months,
        period.days to R.plurals.period_days
    ).filter { it.first != 0 }

    val formattedParts = mutableListOf<String>()
    for ((value, pluralResource) in values) {
        formattedParts.add(
            context.resources.getQuantityString(
                pluralResource,
                value.absoluteValue,
                value.absoluteValue
            )
        )
    }

    return when {
        values.all { it.first < 0 } -> context.resources.getString(
            R.string.text_duration_before,
            formattedParts.joinToString(separator)
        )

        values.all { it.first > 0 } -> context.resources.getString(
            R.string.text_duration_after,
            formattedParts.joinToString(separator)
        )

        else -> {
            val signedParts = mutableListOf<String>()
            for ((index, valueAndResource) in values.withIndex()) {
                signedParts.add(
                    context.resources.getString(
                        if (valueAndResource.first < 0) R.string.text_duration_before else R.string.text_duration_after,
                        formattedParts[index]
                    )
                )
            }
            signedParts.joinToString(separator)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodFieldComponentPreview() {
    var period by remember { mutableStateOf(Period.ofDays(1)) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PeriodFieldComponent(
            period = period,
            onSave = { period = it }
        )
    }
}
