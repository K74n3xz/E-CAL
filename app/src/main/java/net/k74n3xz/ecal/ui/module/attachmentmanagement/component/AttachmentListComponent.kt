package net.k74n3xz.ecal.ui.module.attachmentmanagement.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.core.model.Attachment

@Composable
internal fun AttachmentListComponent(
    attachments: List<Attachment>,
    operationsEnabled: Boolean,
    onEdit: (Attachment) -> Unit,
    onDelete: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = attachments,
            key = { requireNotNull(it.id) }
        ) {
            AttachmentCard(
                attachment = it,
                operationsEnabled = operationsEnabled,
                onEdit = { onEdit(it) },
                onDelete = { onDelete(it) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentListComponentPreview() {
    AttachmentListComponent(
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
