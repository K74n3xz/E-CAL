package net.k74n3xz.ecal.ui.module.monthcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.yearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import net.k74n3xz.ecal.data.settings.repository.AppSettingsRepository
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.domain.repository.EventRepository
import net.k74n3xz.ecal.utils.atEndOfDay
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class MonthCalenderViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val eventRepository: EventRepository
) : ViewModel() {
    // NOTE Repository supply Flow only, default comes from StateFlow of ViewModel.
    private val timeZone: Flow<ZoneId> = appSettingsRepository.timeZone

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    fun setSelectedDate(value: LocalDate) {
        _selectedDate.value = value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val busyDatesRecently: StateFlow<Set<LocalDate>> =
        selectedDate
            .combine(timeZone) { localDate, timeZone -> localDate to timeZone }
            .flatMapLatest { (localDate, timeZone) ->
                val leftBound = ZonedDateTime.of(
                    localDate.yearMonth.minusMonths(1).atStartOfMonth().atStartOfDay(),
                    timeZone
                )
                val rightBound = ZonedDateTime.of(
                    localDate.yearMonth.plusMonths(1).atStartOfMonth().atStartOfDay(),
                    timeZone
                )

                eventRepository.observeEventsOverlappingRange(leftBound, rightBound)
            }
            .combine(timeZone) { events, timeZone -> events to timeZone }
            .mapLatest { (events, timeZone) ->
                buildSet {
                    events.forEach { event ->
                        val startLocalDate = event.startAt.atZone(timeZone).toLocalDate()
                        val endLocalDate =
                            event.endAt?.atZone(timeZone)?.toLocalDate() ?: startLocalDate

                        var localDate = startLocalDate
                        while (!localDate.isAfter(endLocalDate)) {
                            add(localDate)
                            localDate = localDate.plusDays(1)
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventsOnSelectedDate: StateFlow<List<Event>> =
        selectedDate
            .combine(timeZone) { localDate, timeZone -> localDate to timeZone }
            .flatMapLatest { (localDate, timeZone) ->
                eventRepository.observeEventsOverlappingRange(
                    localDate.atStartOfDay(timeZone),
                    localDate.atEndOfDay(timeZone)
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}