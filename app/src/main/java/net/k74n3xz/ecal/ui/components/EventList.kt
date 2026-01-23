package net.k74n3xz.ecal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.data.entity.Event
import net.k74n3xz.ecal.data.entity.enumeration.event.EventStatus
import net.k74n3xz.ecal.data.util.generateEventUid
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun EventList(
    events: List<Event>,
    noEventHint: @Composable (() -> Unit)?,
    onEventClick: (Event) -> Unit,
    timezone: ZoneId,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        if (noEventHint != null) {
            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                noEventHint()
            }
        }
    } else {
        LazyColumn(modifier.fillMaxWidth()) {
            items(events, key = { it.uid }) { event ->
                EventListItem(
                    event = event,
                    onClick = { onEventClick(event) },
                    timezone = timezone,
                    locale = locale
                )
            }
        }
    }
}

@Composable
private fun EventListItem(
    event: Event, onClick: () -> Unit, timezone: ZoneId, locale: Locale
) {
    val timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
    val start = event.startAt.atZone(timezone)
    val end = event.endAt?.atZone(timezone)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(headlineContent = {
            Text(
                text = event.summary ?: stringResource(R.string.text_no_title_hint),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }, supportingContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!event.location.isNullOrBlank()) {
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (end != null) {
                        "${start.format(timeFormatter)} — ${end.format(timeFormatter)}"
                    } else {
                        start.format(timeFormatter)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }, leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.icon_content_description_event),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }, trailingContent = {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.button_content_description_edit),
                    tint = Color.Gray
                )
            }
        })
    }
}

internal fun sampleEvents() = listOf(
    Event(
        uid = generateEventUid(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        summary = "Team Standup",
        description = "Daily sync",
        location = "Conference Room A",
        startAt = Instant.now().plusSeconds(3600),
        isAllDayEvent = false,
        endAt = Instant.now().plusSeconds(3600 * 2),
        status = EventStatus.CONFIRMED
    ), Event(
        uid = generateEventUid(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        summary = "Design Review",
        description = "Review new UI flows",
        location = "Zoom",
        startAt = Instant.now().plusSeconds(3600 * 24),
        isAllDayEvent = false,
        endAt = Instant.now().plusSeconds(3600 * 25),
        status = EventStatus.CONFIRMED
    )
)

@Preview(showBackground = true)
@Composable
private fun EventListPreview() {
    EventList(
        sampleEvents(),
        { Text(stringResource(R.string.text_no_events_today_hint)) },
        {},
        ZoneId.systemDefault(),
        Locale.getDefault()
    )
}

@Preview(showBackground = true)
@Composable
private fun EmptyEventListPreview() {
    EventList(
        listOf(),
        { Text(stringResource(R.string.text_no_events_today_hint)) },
        {},
        ZoneId.systemDefault(),
        Locale.getDefault()
    )
}