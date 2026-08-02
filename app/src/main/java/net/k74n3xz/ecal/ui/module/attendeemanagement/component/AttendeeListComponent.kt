package net.k74n3xz.ecal.ui.module.attendeemanagement.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.core.model.Attendee

@Composable
internal fun AttendeeListComponent(
    attendees: List<Attendee>,
    operationsEnabled: Boolean,
    onEdit: (Attendee) -> Unit,
    onDelete: (Attendee) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = attendees,
            key = { requireNotNull(it.id) }
        ) {
            AttendeeCard(
                attendee = it,
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
private fun AttendeeListComponentPreview() {
    AttendeeListComponent(
        attendees = listOf(
            Attendee(
                id = 1L,
                name = "Alice Johnson",
                description = "Product manager responsible for the mobile application.",
                email = "alice.johnson@example.com"
            ),
            Attendee(
                id = 2L,
                name = "Brian Smith",
                description = "Backend engineer specializing in Kotlin and Spring Boot.",
                email = "brian.smith@example.com"
            ),
            Attendee(
                id = 3L,
                name = "Catherine Lee",
                description = "UI designer focused on accessible user experiences.",
                email = "catherine.lee@example.com"
            ),
            Attendee(
                id = 4L,
                name = "Daniel Brown",
                description = "Data analyst presenting the quarterly performance report.",
                email = "daniel.brown@example.com"
            ),
            Attendee(
                id = 5L,
                name = "Emma Wilson",
                description = "Guest speaker discussing modern software architecture.",
                email = "emma.wilson@example.com"
            ),
            Attendee(
                id = 6L,
                name = "Frank Miller",
                description = null,
                email = "frank.miller@example.com"
            ),
            Attendee(
                id = 7L,
                name = null,
                description = "An attendee who chose not to provide a display name.",
                email = "anonymous.attendee@example.com"
            ),
            Attendee(
                id = 8L,
                name = "Grace Taylor",
                description = "New attendee whose registration is still being processed.",
                email = "grace.taylor@example.com"
            )
        ),
        operationsEnabled = true,
        onEdit = {},
        onDelete = {}
    )
}
