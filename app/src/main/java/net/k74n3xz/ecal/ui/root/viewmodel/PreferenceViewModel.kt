package net.k74n3xz.ecal.ui.root.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class PreferenceViewModel @Inject constructor(private val preferenceRepository: PreferenceRepository) :
    ViewModel() {
    // NOTE Repository supply Flow only, default comes from StateFlow of ViewModel.
    val timeZone: StateFlow<ZoneId> =
        preferenceRepository
            .timeZone
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault())
}