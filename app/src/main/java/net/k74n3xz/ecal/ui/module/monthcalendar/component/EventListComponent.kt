package net.k74n3xz.ecal.ui.module.monthcalendar.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.domain.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.data.calendar.utils.formatTimeRange
import net.k74n3xz.ecal.data.calendar.utils.generateEventUid
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import java.time.Instant
import java.time.ZoneId
import java.time.format.FormatStyle

@Composable
fun EventListComponent(
    eventList: List<Event>,
    onEventEdit: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(
            items = eventList,
            key = { it.uid }
        ) {
            EventCard(
                event = it,
                onEdit = onEventEdit,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun EventCard(event: Event, onEdit: (Event) -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                val summary = event.summary ?: stringResource(R.string.text_no_summary_hint)
                val summaryColor = if (event.summary != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(0.4f)
                }
                val description = event.description ?: stringResource(R.string.text_no_description_hint)
                val descriptionColor = if (event.summary != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(0.4f)
                }
                val location = event.location

                Text(
                    text = summary,
                    color = summaryColor,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = description,
                    color = descriptionColor,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!location.isNullOrBlank()) {
                    Text(
                        text = event.location,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = event.formatTimeRange(FormatStyle.SHORT, LocalTimeZone.current),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(onClick = { onEdit(event) }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.button_content_description_edit)
                )
            }
        }
    }
}

private val sampleEvents = listOf(
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
    ),
    Event(
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
private fun EventCardPreview1() {
    val timeZone = ZoneId.systemDefault()

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        EventCard(sampleEvents[0], {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EventCardPreview2() {
    val timeZone = ZoneId.systemDefault()

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        EventCard(Event(), {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListComponentPreview() {
    val timeZone = ZoneId.systemDefault()

    CompositionLocalProvider(LocalTimeZone provides timeZone) {
        EventListComponent(sampleEvents, {})
    }
}