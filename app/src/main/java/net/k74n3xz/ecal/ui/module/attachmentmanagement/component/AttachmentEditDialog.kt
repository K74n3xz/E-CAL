package net.k74n3xz.ecal.ui.module.attachmentmanagement.component

import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.IOException
import net.k74n3xz.ecal.R

private data class Document(val name: String, val mimeType: String, val sizeBytes: Long, val uri: Uri)

@Composable
internal fun AttachmentEditDialog(
    initialDescription: String?,
    canImport: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val description = rememberTextFieldState(initialDescription ?: "")
    var isDescriptionClear by remember { mutableStateOf(initialDescription == null) }

    var selectedDocument by remember { mutableStateOf<Document?>(null) }
    var hasErrorOccurred by remember { mutableStateOf(false) }

    val contentResolver = LocalContext.current.applicationContext.contentResolver
    val selectDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedDocument = null
            hasErrorOccurred = false

            try {
                // TODO: Move this file metadata query to an asynchronous thread.
                contentResolver.query(
                    uri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    ),
                    null,
                    null,
                    null
                )?.use {
                    val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeTypeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeBytesIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    if (it.count == 1 && it.moveToFirst()) {
                        selectedDocument = Document(
                            name = it.getString(nameIndex),
                            mimeType = it.getString(mimeTypeIndex),
                            sizeBytes = it.getLong(sizeBytesIndex),
                            uri = uri
                        )
                    } else {
                        throw IOException("The file metadata query returned an invalid result.")
                    }
                } ?: throw IOException("Failed to retrieve file metadata.")
            } catch (_: Exception) {
                hasErrorOccurred = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        if (isDescriptionClear) null else description.text.toString(),
                        selectedDocument?.uri.toString()
                    )
                },
                enabled = !canImport || selectedDocument != null
            ) {
                Text(
                    text = stringResource(if (canImport) R.string.text_add else R.string.text_save),
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
                text = stringResource(
                    if (canImport) R.string.dialog_title_add_attachment else R.string.dialog_title_edit_attachment
                ),
                color = Color.Unspecified,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClearableTextFieldComponent(
                    textFieldState = description,
                    isClear = isDescriptionClear,
                    onClear = { isDescriptionClear = true },
                    onDirty = { isDescriptionClear = false },
                    labelText = stringResource(R.string.text_field_label_description)
                )

                if (canImport) {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        selectedDocument.let {
                            if (it != null) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(
                                        R.string.text_attachment_file_metadata,
                                        it.name,
                                        it.mimeType,
                                        Formatter.formatShortFileSize(LocalContext.current, it.sizeBytes)
                                    ),
                                    color = Color.Unspecified,
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else if (hasErrorOccurred) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.error_attachment_file_read_failed),
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        TextButton(onClick = { selectDocumentLauncher.launch(arrayOf("*/*")) }) {
                            Text(
                                text = stringResource(R.string.text_select_file),
                                color = Color.Unspecified,
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun AttachmentEditDialogPreview() {
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
            AttachmentEditDialog(
                initialDescription = null,
                canImport = true,
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }
    }
}
