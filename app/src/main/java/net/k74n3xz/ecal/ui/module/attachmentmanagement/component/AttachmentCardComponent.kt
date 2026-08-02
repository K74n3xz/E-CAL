package net.k74n3xz.ecal.ui.module.attachmentmanagement.component

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attachment

@Composable
internal fun AttachmentCard(
    attachment: Attachment,
    operationsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = attachment.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = attachment.description ?: stringResource(R.string.text_no_description_hint),
                    color = MaterialTheme.colorScheme.onSurface.let {
                        if (attachment.description == null) it.copy(alpha = 0.4f) else it
                    },
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(
                        R.string.text_attachment_metadata,
                        attachment.mimeType,
                        Formatter.formatShortFileSize(LocalContext.current, attachment.sizeBytes)
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    enabled = operationsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(
                            R.string.button_content_description_edit_attachment,
                            attachment.name
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    enabled = operationsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(
                            R.string.button_content_description_delete_attachment,
                            attachment.name
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentCardPreview() {
    AttachmentCard(
        attachment = Attachment(
            id = null,
            description = "Project requirements document",
            name = "requirements.pdf",
            mimeType = "application/pdf",
            sizeBytes = 1_048_576L
        ),
        operationsEnabled = true,
        onEdit = {},
        onDelete = {}
    )
}
