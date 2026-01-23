package net.k74n3xz.ecal.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.data.AppDatabase
import net.k74n3xz.ecal.data.entity.Alarm
import net.k74n3xz.ecal.data.entity.Event
import net.k74n3xz.ecal.data.entity.enumeration.alarm.AlarmType
import net.k74n3xz.ecal.data.repository.AlarmRepository
import net.k74n3xz.ecal.data.repository.EventRepository
import net.k74n3xz.ecal.reminder.ReminderScheduler
import net.k74n3xz.ecal.ui.components.CalendarType.DAY
import net.k74n3xz.ecal.ui.components.CalendarType.MONTH
import net.k74n3xz.ecal.ui.components.CalendarType.WEEK
import net.k74n3xz.ecal.util.atEndOfDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class MainScreenViewModel(
    private val eventRepository: EventRepository,
    private val alarmRepository: AlarmRepository,
    private val context: Context
) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainScreenViewModel(
                    EventRepository.getRepository(AppDatabase.getDatabase(this[APPLICATION_KEY]!!.applicationContext)),
                    AlarmRepository.getRepository(AppDatabase.getDatabase(this[APPLICATION_KEY]!!.applicationContext)),
                    this[APPLICATION_KEY]!!
                )
            }
        }
    }

    var zoneId: ZoneId by mutableStateOf(ZoneId.systemDefault())
        private set

    var locale: Locale by mutableStateOf(Locale.getDefault())
        private set

    var calendarType by mutableStateOf(MONTH)

    var firstDayOfWeek by mutableStateOf(DayOfWeek.MONDAY)
        private set

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dateWithEvent =
        combine(snapshotFlow { calendarType }, snapshotFlow { selectedDate }) { type, localDate ->
            type to localDate
        }.distinctUntilChanged().map {
            val leftBound: LocalDate
            val rightBound: LocalDate
            when (it.first) {
                MONTH -> {
                    val firstDayOfMonth = it.second.withDayOfMonth(1)
                    leftBound =
                        firstDayOfMonth.minusDays((firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value).toLong())
                            .minusWeeks(if (firstDayOfMonth.dayOfWeek == firstDayOfWeek) 1 else 0)
                    rightBound = leftBound.plusDays(41)
                }

                WEEK -> {
                    leftBound =
                        it.second.minusDays((it.second.dayOfWeek.value - firstDayOfWeek.value).toLong())
                    rightBound = leftBound.plusDays(6)
                }

                DAY -> {
                    leftBound = it.second
                    rightBound = leftBound.plusDays(0)
                }
            }
            leftBound to rightBound
        }.flatMapLatest {
            eventRepository.getEventOverlappingInCloseRange(
                it.first.atStartOfDay(zoneId),
                it.second.atEndOfDay(zoneId)
            ).distinctUntilChanged().map { events ->
                buildSet {
                    events.forEach { event ->
                        val startLocalDate =
                            ZonedDateTime.ofInstant(event.startAt, zoneId).toLocalDate()
                        val endLocalDate = event.endAt?.let {
                            ZonedDateTime.ofInstant(event.endAt, zoneId).toLocalDate()
                        } ?: startLocalDate

                        var localDate = startLocalDate
                        while (!localDate.isAfter(endLocalDate)) {
                            add(localDate)
                            localDate = localDate.plusDays(1)
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventInSelectedDate =
        combine(snapshotFlow { selectedDate }, snapshotFlow { zoneId }) { localDate, zoneId ->
            localDate to zoneId
        }.distinctUntilChanged()
            .flatMapLatest { eventRepository.getEventCoveringDate(it.first, it.second) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyArray())

    var editingEvent: Event? by mutableStateOf(null)

    val alarmForEditingEvent = snapshotFlow { editingEvent }.map {
        if (it == null) emptyArray() else alarmRepository.getAlarmForEvent(it)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000), emptyArray()
    )

    fun upsertEventWithAlarms(event: Event, alarms: Array<Alarm>) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmRepository.getAlarmForEvent(event)
                .forEach { ReminderScheduler.cancelReminder(context, it.id!!) }
            eventRepository.upsertEventWithAlarms(event, alarms)
            alarmRepository.getAlarmForEvent(event).forEach {
                ReminderScheduler.scheduleReminder(
                    context,
                    it.id!!,
                    when (it.type) {
                        AlarmType.ABSOLUTE -> it.absoluteTrigger!!.toEpochMilli()
                        AlarmType.RELATIVE -> event.startAt.minusSeconds(60 * it.relativeMinutesTrigger!!)
                            .toEpochMilli()
                    },
                    event.summary,
                    event.description
                )
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) { eventRepository.deleteEvent(event) }
    }
}