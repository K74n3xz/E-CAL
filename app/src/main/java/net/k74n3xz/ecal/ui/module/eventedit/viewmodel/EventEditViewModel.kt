package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.data.calendar.model.Alarm
import net.k74n3xz.ecal.data.calendar.model.Event
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import net.k74n3xz.ecal.data.calendar.repository.EventRepository
import net.k74n3xz.ecal.usecase.ApplyAlarmsUseCase
import net.k74n3xz.ecal.usecase.DeleteEventUseCase
import javax.inject.Inject

@HiltViewModel
class EventEditViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val alarmRepository: AlarmRepository,
    private val deleteEvent: DeleteEventUseCase,
    private val applyAlarms: ApplyAlarmsUseCase
) : ViewModel() {
    // TODO: Expose save and delete operation states so the UI can prevent duplicate actions and report failures.

    private val _event: MutableStateFlow<Event?> = MutableStateFlow(null)
    val event: StateFlow<Event?> = _event.asStateFlow()
    fun loadEventWithAlarmsByUid(uid: String) {
        viewModelScope.launch(Dispatchers.IO) { _event.value = eventRepository.getEventByUid(uid) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val alarms: StateFlow<List<Alarm>?> =
        event
            .flatMapLatest {
                it?.let { alarmRepository.observeAlarmForEventByEventUid(it.uid) } ?: flowOf(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveEventAndApplyAlarms(event: Event, alarms: List<Alarm>) {
        viewModelScope.launch(Dispatchers.IO) {
            eventRepository.upsertEvent(event)
            applyAlarms(event.uid, alarms)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) { deleteEvent(event.uid) }
    }
}