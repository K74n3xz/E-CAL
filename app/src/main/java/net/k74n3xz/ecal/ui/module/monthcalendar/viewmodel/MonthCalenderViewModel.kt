package net.k74n3xz.ecal.ui.module.monthcalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.yearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
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
import net.k74n3xz.ecal.core.application.port.inbound.service.EventQueryService
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.event.DateRange
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.model.utils.atEndOfDay
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository

@HiltViewModel
class MonthCalenderViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val eventQueryService: EventQueryService
) : ViewModel() {
    private val timeZone: Flow<ZoneId> = preferenceRepository.timeZone

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
                val leftBound =
                    ZonedDateTime.of(localDate.yearMonth.minusMonths(1).atStartOfMonth().atStartOfDay(), timeZone)
                val rightBound =
                    ZonedDateTime.of(localDate.yearMonth.plusMonths(1).atEndOfMonth().atEndOfDay(), timeZone)

                eventQueryService.observeEventsOverlappingRange(leftBound, rightBound)
            }
            .combine(timeZone) { events, timeZone -> events to timeZone }
            .mapLatest { (events, timeZone) ->
                buildSet {
                    events.forEach { event ->
                        val (startLocalDate, endLocalDate) = when (val schedule = event.schedule) {
                            is EventTiming.AllDay -> schedule.dateRange

                            is EventTiming.Timed -> {
                                val (startAt, endAt) = schedule.timeRange
                                DateRange(startAt.atZone(timeZone).toLocalDate(), endAt.atZone(timeZone).toLocalDate())
                            }
                        }

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
                eventQueryService.observeEventsOverlappingRange(
                    localDate.atStartOfDay(timeZone),
                    localDate.atEndOfDay(timeZone)
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
