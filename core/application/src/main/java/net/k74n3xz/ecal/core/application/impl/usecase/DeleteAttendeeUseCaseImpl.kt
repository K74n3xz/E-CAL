package net.k74n3xz.ecal.core.application.impl.usecase

import net.k74n3xz.ecal.core.application.port.inbound.usecase.DeleteAttendeeUseCase
import net.k74n3xz.ecal.core.application.port.outbound.platform.AlarmReconciler
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttendeeRepository

internal class DeleteAttendeeUseCaseImpl(
    private val attendeeRepository: AttendeeRepository,
    private val alarmReconciler: AlarmReconciler
) : DeleteAttendeeUseCase {
    override suspend fun invoke(attendeeId: Long) {
        attendeeRepository.deleteAttendeeById(attendeeId)
        alarmReconciler.request()
    }
}
