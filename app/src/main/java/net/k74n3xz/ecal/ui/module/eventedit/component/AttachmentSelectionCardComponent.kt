package net.k74n3xz.ecal.ui.module.eventedit.component

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
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
internal fun AttachmentSelectionCard(
    attachment: Attachment,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
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

            Checkbox(checked = isSelected, onCheckedChange = onSelectedChange)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentSelectionCardPreview() {
    AttachmentSelectionCard(
        attachment = Attachment(
            id = null,
            description = "Project requirements document",
            name = "requirements.pdf",
            mimeType = "application/pdf",
            sizeBytes = 1_048_576L
        ),
        isSelected = false,
        onSelectedChange = {}
    )
}
