package net.k74n3xz.ecal.core.application.impl.usecase

import java.time.ZoneId
import net.k74n3xz.ecal.core.application.port.inbound.usecase.RebuildAllDayAlarmOccurrencesUseCase
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.outbound.repository.AlarmRepository

internal class RebuildAllDayAlarmOccurrencesUseCaseImpl(
    private val alarmRepository: AlarmRepository,
    private val alarmReconciler: AlarmReconciler
) : RebuildAllDayAlarmOccurrencesUseCase {
    override suspend fun invoke(timeZone: ZoneId) {
        alarmRepository.rebuildAllDayAlarmOccurrences(timeZone)
        alarmReconciler.request()
    }
}
