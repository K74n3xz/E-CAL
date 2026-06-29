package net.k74n3xz.ecal.ui.module.monthcalendar.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.ui.module.monthcalendar.component.scaffold.AddEventFabComponent
import net.k74n3xz.ecal.ui.module.monthcalendar.screen.MonthCalendarScreen
import net.k74n3xz.ecal.ui.module.monthcalendar.viewmodel.MonthCalenderViewModel

fun EntryProviderScope<NavKey>.registerMonthCalendarEntry(
    navigateToAddEvent: () -> Unit,
    navigateToEditEvent: (Event) -> Unit
) {
    entry<MonthCalendar> {
        val viewModel: MonthCalenderViewModel = hiltViewModel()

        val busyDates by viewModel.busyDatesRecently.collectAsStateWithLifecycle()
        val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
        val events by viewModel.eventsOnSelectedDate.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = { AddEventFabComponent(navigateToAddEvent) },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            MonthCalendarScreen(
                busyDates = busyDates,
                selectedDate = selectedDate,
                onSelectedDateChange = viewModel::setSelectedDate,
                eventList = events,
                onEventEdit = navigateToEditEvent,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}