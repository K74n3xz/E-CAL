package net.k74n3xz.ecal.ui.module.attendeemanagement.screen

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
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.AttendeeListComponent

@Composable
internal fun AttendeeManagementScreen(
    attendees: List<Attendee>,
    operationsEnabled: Boolean,
    onEdit: (Attendee) -> Unit,
    onDelete: (Attendee) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attendees.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.text_no_attendees),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        AttendeeListComponent(
            attendees = attendees,
            operationsEnabled = operationsEnabled,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendeeManagementScreenPreview() {
    AttendeeManagementScreen(
        attendees = listOf(Attendee(1, "Ada Lovelace", "Organizer", "ada@example.com")),
        operationsEnabled = true,
        onEdit = {},
        onDelete = {}
    )
}
