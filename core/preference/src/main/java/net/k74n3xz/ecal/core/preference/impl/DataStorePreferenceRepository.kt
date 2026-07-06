package net.k74n3xz.ecal.core.preference.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStorePreferenceRepository @Inject constructor() : PreferenceRepository {
    // TODO: Persist app settings with DataStore instead of keeping them only in memory.
    private val _timeZone: MutableStateFlow<ZoneId> = MutableStateFlow(ZoneId.systemDefault())
    override val timeZone: Flow<ZoneId> = _timeZone.asStateFlow()
}