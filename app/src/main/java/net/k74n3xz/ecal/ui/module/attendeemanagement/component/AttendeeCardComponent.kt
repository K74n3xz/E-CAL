package net.k74n3xz.ecal.ui.module.attendeemanagement.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attendee

@Composable
internal fun AttendeeCard(
    attendee: Attendee,
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
                    text = attendee.name ?: stringResource(R.string.text_no_name_hint),
                    color = MaterialTheme.colorScheme.onSurface.let {
                        if (attendee.name == null) it.copy(alpha = 0.4f) else it
                    },
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = attendee.email,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = attendee.description ?: stringResource(R.string.text_no_description_hint),
                    color = MaterialTheme.colorScheme.onSurface.let {
                        if (attendee.description == null) it.copy(alpha = 0.4f) else it
                    },
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
                            R.string.button_content_description_edit_attendee,
                            attendee.name ?: attendee.email
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
                            R.string.button_content_description_delete_attendee,
                            attendee.name ?: attendee.email
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendeeCardPreview() {
    AttendeeCard(
        attendee = Attendee(
            id = 1L,
            name = "Alice Johnson",
            description = "Product manager responsible for the mobile application.",
            email = "alice.johnson@example.com"
        ),
        operationsEnabled = true,
        onEdit = {},
        onDelete = {}
    )
}
