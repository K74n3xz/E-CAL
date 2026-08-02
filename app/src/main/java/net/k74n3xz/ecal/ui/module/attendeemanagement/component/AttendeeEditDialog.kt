package net.k74n3xz.ecal.ui.module.attendeemanagement.component

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
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
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attendee

@Composable
internal fun AttendeeEditDialog(
    attendee: Attendee?,
    onDismiss: () -> Unit,
    onConfirm: (Attendee) -> Unit,
    modifier: Modifier = Modifier
) {
    val name = rememberTextFieldState(attendee?.name.orEmpty())
    var isNameClear by rememberSaveable { mutableStateOf(attendee?.name == null) }

    val description = rememberTextFieldState(attendee?.description.orEmpty())
    var isDescriptionClear by rememberSaveable { mutableStateOf(attendee?.description == null) }

    val email = rememberTextFieldState(attendee?.email.orEmpty())
    val normalizedEmail = email.text.toString().trim()
    val isEmailValid = normalizedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Attendee(
                            id = attendee?.id,
                            name = if (isNameClear) null else name.text.toString(),
                            description = if (isDescriptionClear) null else description.text.toString(),
                            email = normalizedEmail
                        )
                    )
                },
                enabled = isEmailValid
            ) {
                Text(
                    text = stringResource(if (attendee == null) R.string.text_add else R.string.text_save),
                    color = Color.Unspecified,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.text_cancel),
                    color = Color.Unspecified,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        title = {
            Text(
                stringResource(
                    if (attendee == null) R.string.dialog_title_add_attendee else R.string.dialog_title_edit_attendee
                ),
                color = Color.Unspecified,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val defaultKeyboardOptions = KeyboardOptions.Default.copy(
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Text,
                    showKeyboardOnFocus = true
                )

                ClearableTextFieldComponent(
                    textFieldState = name,
                    isClear = isNameClear,
                    onClear = { isNameClear = true },
                    onDirty = { isNameClear = false },
                    labelText = stringResource(R.string.text_field_label_name),
                    keyboardOptions = defaultKeyboardOptions.copy(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                ClearableTextFieldComponent(
                    textFieldState = description,
                    isClear = isDescriptionClear,
                    onClear = { isDescriptionClear = true },
                    onDirty = { isDescriptionClear = false },
                    labelText = stringResource(R.string.text_field_label_description),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = email,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    labelPosition = TextFieldLabelPosition.Attached(),
                    label = {
                        Text(
                            text = stringResource(R.string.text_field_label_email),
                            color = Color.Unspecified,
                            textAlign = TextAlign.Center,
                            style = LocalTextStyle.current
                        )
                    },
                    supportingText = {
                        if (!isEmailValid) {
                            Text(
                                text = stringResource(R.string.error_invalid_email),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    isError = !isEmailValid,
                    keyboardOptions = defaultKeyboardOptions.copy(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun AttendeeEditDialogPreview() {
    var isShowingDialog by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { isShowingDialog = true }) {
                Text(
                    text = "show dialog",
                    color = Color.Unspecified,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (isShowingDialog) {
            AttendeeEditDialog(
                attendee = Attendee(1, "Ada Lovelace", "Organizer", "ada@example.com"),
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}
