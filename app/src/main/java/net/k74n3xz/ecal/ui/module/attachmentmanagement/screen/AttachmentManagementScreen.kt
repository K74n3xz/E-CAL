package net.k74n3xz.ecal.ui.module.attachmentmanagement.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.AttachmentListComponent

@Composable
fun AttachmentManagementScreen(
    attachments: List<Attachment>,
    operationsEnabled: Boolean,
    onEdit: (Attachment) -> Unit,
    onDelete: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.text_no_attachments),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        AttachmentListComponent(
            attachments = attachments,
            operationsEnabled = operationsEnabled,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AttachmentManagementScreenPreview1() {
    AttachmentManagementScreen(
        attachments = emptyList(),
        operationsEnabled = true,
        onEdit = {},
        onDelete = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AttachmentManagementScreenPreview2() {
    AttachmentManagementScreen(
        attachments = listOf(
            Attachment(
                id = 1L,
                description = "Project requirements document",
                name = "requirements.pdf",
                mimeType = "application/pdf",
                sizeBytes = 245_760L
            ),
            Attachment(
                id = 2L,
                description = "Application screenshot",
                name = "dashboard.png",
                mimeType = "image/png",
                sizeBytes = 1_048_576L
            ),
            Attachment(
                id = 3L,
                description = "Customer data export",
                name = "customers.csv",
                mimeType = "text/csv",
                sizeBytes = 98_304L
            ),
            Attachment(
                id = 4L,
                description = null,
                name = "meeting-notes.txt",
                mimeType = "text/plain",
                sizeBytes = 4_096L
            )
        ),
        operationsEnabled = true,
        onEdit = {},
        onDelete = {}
    )
}
