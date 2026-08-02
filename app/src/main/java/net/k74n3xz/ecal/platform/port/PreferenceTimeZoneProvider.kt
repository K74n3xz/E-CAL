package net.k74n3xz.ecal.platform.port

import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import net.k74n3xz.ecal.core.application.port.outbound.platform.TimeZoneProvider
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository

@Singleton
internal class PreferenceTimeZoneProvider @Inject constructor(preferenceRepository: PreferenceRepository) :
    TimeZoneProvider {
    override val timeZone: Flow<ZoneId> = preferenceRepository.timeZone
}
