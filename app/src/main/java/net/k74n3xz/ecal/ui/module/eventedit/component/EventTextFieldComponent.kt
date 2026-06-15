package net.k74n3xz.ecal.ui.module.eventedit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.k74n3xz.ecal.R

@Composable
fun EventTextFieldComponent(
    textFieldState: TextFieldState,
    isClear: Boolean,
    onClear: () -> Unit,
    onDirty: () -> Unit,
    labelText: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }
            .collect {
                if (isClear && textFieldState.text.isNotEmpty()) {
                    onDirty()
                }
            }
    }

    OutlinedTextField(
        state = textFieldState,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        labelPosition = TextFieldLabelPosition.Attached(),
        label = {
            Text(
                text = labelText,
                color = Color.Unspecified,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current
            )
        },
        trailingIcon = {
            if (!isClear) {
                IconButton(onClick = {
                    textFieldState.clearText()
                    onClear()
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.button_content_description_clear)
                    )
                }
            }
        },
        keyboardOptions = keyboardOptions
    )
}

@Preview(showBackground = true)
@Composable
private fun EventTextFieldComponentPreview() {
    val fieldState = rememberTextFieldState()
    var isFieldClear by remember { mutableStateOf(true) }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EventTextFieldComponent(
            textFieldState = fieldState,
            isClear = isFieldClear,
            onClear = { isFieldClear = true },
            onDirty = { isFieldClear = false },
            labelText = "Label",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                showKeyboardOnFocus = true
            )
        )

        Text(
            text = """
                text: [${fieldState.text}]
                isFieldClear: $isFieldClear
            """.trimIndent(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}