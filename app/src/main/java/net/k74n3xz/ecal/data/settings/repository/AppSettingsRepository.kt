package net.k74n3xz.ecal.data.settings.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor() {
    // TODO: Persist app settings with DataStore instead of keeping them only in memory.
    private val _timeZone: MutableStateFlow<ZoneId> = MutableStateFlow(ZoneId.systemDefault())
    val timeZone: Flow<ZoneId> = _timeZone.asStateFlow()
}