package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.domain.model.Event
import net.k74n3xz.ecal.application.usecase.DeleteEventUseCase
import net.k74n3xz.ecal.application.usecase.SaveEventUseCase
import net.k74n3xz.ecal.domain.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventEditViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val saveEventUseCase: SaveEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) : ViewModel() {
    // TODO: Expose save and delete operation states so the UI can prevent duplicate actions and report failures.

    private val _event: MutableStateFlow<Event?> = MutableStateFlow(null)
    val event: StateFlow<Event?> = _event.asStateFlow()
    fun loadEventByUid(uid: String) {
        viewModelScope.launch(Dispatchers.IO) { _event.value = eventRepository.getEventByUid(uid) }
    }

    fun saveEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) { saveEventUseCase(event) }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) { deleteEventUseCase(event.uid) }
    }
}