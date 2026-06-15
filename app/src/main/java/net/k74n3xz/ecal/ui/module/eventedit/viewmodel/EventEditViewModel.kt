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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.data.calendar.database.entity.enumeration.TriggerRelationship
import net.k74n3xz.ecal.data.calendar.model.Alarm
import net.k74n3xz.ecal.data.calendar.model.Event
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import net.k74n3xz.ecal.data.calendar.repository.EventRepository
import net.k74n3xz.ecal.reminder.ReminderScheduler
import javax.inject.Inject

@HiltViewModel
class EventEditViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val alarmRepository: AlarmRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val _event: MutableStateFlow<Event?> = MutableStateFlow(null)
    val event: StateFlow<Event?> = _event.asStateFlow()
    fun loadEventWithAlarmsByUid(uid: String) {
        viewModelScope.launch(Dispatchers.IO) { _event.value = eventRepository.getEventByUid(uid) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val alarms: StateFlow<List<Alarm>?> =
        event
            .flatMapLatest {
                it?.let { alarmRepository.getAlarmForEventByEventUid(it.uid) } ?: flowOf(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) { eventRepository.upsertEvent(event) }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) { eventRepository.deleteEvent(event) }
    }

    fun replaceAlarmsForEvent(event: Event, alarms: List<Alarm>) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Track alarm changes with a dirty flag and reconcile scheduling inside the database transaction.
            val oldAlarms = alarmRepository.getAlarmForEventByEventUid(event.uid).first()
            alarmRepository.replaceAlarms(oldAlarms, alarms)
            val newAlarms = alarmRepository.getAlarmForEventByEventUid(event.uid).first()
            oldAlarms.forEach { reminderScheduler.cancelReminder(it.id!!) }
            newAlarms.forEach {
                reminderScheduler.scheduleReminder(
                    reminderId = it.id!!,
                    triggerAtMillis = when (it.trigger) {
                        is Alarm.Trigger.RelativeTrigger -> when (it.trigger.relativeTo) {
                            TriggerRelationship.START -> event.startAt
                            TriggerRelationship.END -> event.endAt ?: event.startAt
                            // TODO: Normalize all-day event boundaries before applying relative alarm offsets.
                        }.minusSeconds(it.trigger.offset.toSeconds()).toEpochMilli()

                        is Alarm.Trigger.AbsoluteTrigger -> it.trigger.at.toEpochMilli()
                    },
                    title = event.summary,
                    text = event.description
                )
            }
        }
    }
}