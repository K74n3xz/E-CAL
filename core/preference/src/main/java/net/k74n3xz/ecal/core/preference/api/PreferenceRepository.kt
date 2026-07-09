package net.k74n3xz.ecal.core.preference.api

import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    val timeZone: Flow<ZoneId>
}
