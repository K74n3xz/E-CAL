package net.k74n3xz.ecal.ui.module.monthcalendar.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.domain.model.enumeration.event.EventStatus
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.data.calendar.utils.generateEventUid
import net.k74n3xz.ecal.ui.compositionlocal.LocalTimeZone
import net.k74n3xz.ecal.ui.module.monthcalendar.component.EventListComponent
import net.k74n3xz.ecal.ui.module.monthcalendar.component.MonthCalendarComponent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MonthCalendarScreen(
    busyDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit,
    eventList: List<Event>,
    onEventEdit: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthCalendarComponent(
            busyDates = busyDates,
            selectedDate = selectedDate,
            onDateSelected = onSelectedDateChange
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        if (eventList.isEmpty()) {
            Text(
                text = stringResource(R.string.text_no_events_today_hint),
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            EventListComponent(
                eventList = eventList,
                onEventEdit = onEventEdit
            )
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
private fun MonthCalendarScreenPreview1() {
    val today = LocalDate.now()

    var selectedDate by remember { mutableStateOf(today) }

    CompositionLocalProvider(LocalTimeZone provides ZoneId.systemDefault()) {
        MonthCalendarScreen(
            busyDates = setOf(
                today,
                today.minusDays(2),
                today.plusDays(1),
                today.plusDays(3)
            ),
            selectedDate = selectedDate,
            onSelectedDateChange = { selectedDate = it },
            eventList = sampleEvents,
            onEventEdit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthCalendarScreenPreview2() {
    val today = LocalDate.now()

    var selectedDate by remember { mutableStateOf(today) }

    CompositionLocalProvider(LocalTimeZone provides ZoneId.systemDefault()) {
        MonthCalendarScreen(
            busyDates = emptySet(),
            selectedDate = selectedDate,
            onSelectedDateChange = { selectedDate = it },
            eventList = emptyList(),
            onEventEdit = {}
        )
    }
}