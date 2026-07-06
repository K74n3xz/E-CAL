package net.k74n3xz.ecal.core.preference.api

import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

interface PreferenceRepository {
    val timeZone: Flow<ZoneId>
}