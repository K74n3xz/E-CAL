package net.k74n3xz.ecal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.k74n3xz.ecal.data.settings.repository.AppSettingsRepository
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(private val appSettingsRepository: AppSettingsRepository) :
    ViewModel() {
    val timeZone: StateFlow<ZoneId> =
        appSettingsRepository
            .timeZone
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZoneId.systemDefault())
}