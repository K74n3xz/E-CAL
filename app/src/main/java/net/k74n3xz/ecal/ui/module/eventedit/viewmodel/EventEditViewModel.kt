package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.core.application.port.`in`.service.AttachmentQueryService
import net.k74n3xz.ecal.core.application.port.`in`.service.AttendeeQueryService
import net.k74n3xz.ecal.core.application.port.`in`.service.EventQueryService
import net.k74n3xz.ecal.core.application.port.`in`.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.core.application.port.`in`.usecase.SaveEventUseCase
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Trigger
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EventEditOperationState
import net.k74n3xz.ecal.ui.presentation.form.AlarmForm
import net.k74n3xz.ecal.ui.presentation.form.EventForm
import net.k74n3xz.ecal.ui.presentation.form.utils.toAlarmForm
import net.k74n3xz.ecal.ui.presentation.form.utils.toEventForm
import net.k74n3xz.ecal.ui.utils.generateEventUid

@HiltViewModel
internal class EventEditViewModel @Inject constructor(
    private val eventQueryService: EventQueryService,
    private val attachmentQueryService: AttachmentQueryService,
    private val attendeeQueryService: AttendeeQueryService,
    private val saveEventUseCase: SaveEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private var timeZone: ZoneId = ZoneId.systemDefault()

    init {
        viewModelScope.launch {
            preferenceRepository.timeZone.collect { timeZone = it }
        }
    }

    private val _uiState: MutableStateFlow<EventEditUiState> =
        MutableStateFlow(EventEditUiState(operationState = EventEditOperationState.InitializationState.Uninitialized))
    val uiState: StateFlow<EventEditUiState> = _uiState.asStateFlow()

    private lateinit var event: Event
    lateinit var eventForm: EventForm
        private set

    private lateinit var alarmsWithForms: MutableList<Pair<Alarm, AlarmForm>>
    private val _alarmForms: MutableStateFlow<List<AlarmForm>> = MutableStateFlow(emptyList())
    val alarmForms: StateFlow<List<AlarmForm>> = _alarmForms.asStateFlow()

    val audioAttachments: StateFlow<List<Attachment>> =
        attachmentQueryService.observeAvailableAttachmentsByMimeTopLevelType("audio")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendees: StateFlow<List<Attendee>> = attendeeQueryService.observeAllAttendees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments: StateFlow<List<Attachment>> = attachmentQueryService.observeAvailableAttachments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initialize(eventUid: String?) {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EventEditOperationState.InitializationState.Uninitialized) {
            return
        }

        if (eventUid == null) {
            event = Event(
                uid = generateEventUid(),
                schedule = EventTiming.Timed.InstantTiming(Instant.now())
            )
            alarmsWithForms = event.alarms.map { it to it.toAlarmForm(timeZone) }.toMutableList()
            eventForm = event.toEventForm(timeZone)
            _alarmForms.value = alarmsWithForms.map { it.second }
            _uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = EventEditOperationState.Idle(null))
            )
        } else {
            val newUiStateSnapshot =
                uiStateSnapshot.copy(operationState = EventEditOperationState.InitializationState.Initializing)
            if (!_uiState.compareAndSet(expect = uiStateSnapshot, update = newUiStateSnapshot)) {
                return
            }
            viewModelScope.launch {
                var event: Event? = null
                val cause: Exception? = try {
                    event = eventQueryService.findEventByUid(eventUid)
                    null
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (exception: Exception) {
                    exception
                }

                if (cause is Exception) {
                    _uiState.compareAndSet(
                        expect = newUiStateSnapshot,
                        update = newUiStateSnapshot.copy(
                            operationState = EventEditOperationState.InitializationState.InitializationFailed(cause)
                        )
                    )
                } else if (event == null) {
                    _uiState.compareAndSet(
                        expect = newUiStateSnapshot,
                        update = newUiStateSnapshot.copy(
                            operationState = EventEditOperationState.InitializationState.InitializationFailed(
                                IllegalArgumentException("Event(uid=$eventUid) doesn't exist.")
                            )
                        )
                    )
                } else {
                    this@EventEditViewModel.event = event
                    alarmsWithForms = event.alarms.map { it to it.toAlarmForm(timeZone) }.toMutableList()
                    eventForm = event.toEventForm(timeZone)
                    _alarmForms.value = alarmsWithForms.map { it.second }
                    _uiState.compareAndSet(
                        expect = newUiStateSnapshot,
                        update = newUiStateSnapshot.copy(operationState = EventEditOperationState.Idle(null))
                    )
                }
            }
        }
    }

    fun addAlarm() {
        Alarm(
            action = Action.Display(""),
            trigger = Trigger.RelativeTrigger(
                relativeTo = TriggerRelationship.START,
                offset = Duration.ofMinutes(-15)
            ),
            repetition = null
        ).let {
            alarmsWithForms.add(it to it.toAlarmForm(timeZone))
        }
        _alarmForms.value = alarmsWithForms.map { it.second }
    }

    fun removeAlarm(index: Int) {
        alarmsWithForms.removeAt(index)
        _alarmForms.value = alarmsWithForms.map { it.second }
    }

    fun saveEvent() {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EventEditOperationState.Idle) {
            return
        }

        if (!_uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = EventEditOperationState.Saving)
            )
        ) {
            return
        }
        viewModelScope.launch {
            val operationState = try {
                saveEventUseCase(
                    eventForm.resolve(
                        originalEvent = event,
                        newAlarms = alarmsWithForms.map { it.second.resolve(it.first.id, timeZone).getOrThrow() },
                        timeZone = timeZone
                    ).getOrThrow()
                )
                EventEditOperationState.Exit
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                EventEditOperationState.Idle(exception)
            }
            _uiState.update { it.copy(operationState = operationState) }
        }
    }

    fun deleteEvent() {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EventEditOperationState.Idle) {
            return
        }

        if (!_uiState.compareAndSet(
                expect = uiStateSnapshot,
                update = uiStateSnapshot.copy(operationState = EventEditOperationState.Deleting)
            )
        ) {
            return
        }
        viewModelScope.launch {
            val operationState = try {
                deleteEventUseCase(event.uid)
                EventEditOperationState.Exit
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                EventEditOperationState.Idle(exception)
            }
            _uiState.update { it.copy(operationState = operationState) }
        }
    }

    fun requestExit() {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EventEditOperationState.InitializationState &&
            uiStateSnapshot.operationState !is EventEditOperationState.Idle
        ) {
            return
        }

        _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(operationState = EventEditOperationState.Exit)
        )
    }

    fun consumeExitState(): Boolean {
        val uiStateSnapshot = _uiState.value

        if (uiStateSnapshot.operationState !is EventEditOperationState.Exit) {
            return false
        }

        return _uiState.compareAndSet(
            expect = uiStateSnapshot,
            update = uiStateSnapshot.copy(operationState = EventEditOperationState.Exited)
        )
    }
}
