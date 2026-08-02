package net.k74n3xz.ecal.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.k74n3xz.ecal.core.application.port.out.repository.AlarmRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttachmentRepository
import net.k74n3xz.ecal.core.application.port.out.repository.AttendeeRepository
import net.k74n3xz.ecal.core.application.port.out.repository.EventRepository
import net.k74n3xz.ecal.core.data.repository.AlarmRepositoryImpl
import net.k74n3xz.ecal.core.data.repository.AttachmentRepositoryImpl
import net.k74n3xz.ecal.core.data.repository.AttendeeRepositoryImpl
import net.k74n3xz.ecal.core.data.repository.EventRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    internal abstract fun bindEventRepository(eventRepositoryImpl: EventRepositoryImpl): EventRepository

    @Binds
    internal abstract fun bindAlarmRepository(alarmRepositoryImpl: AlarmRepositoryImpl): AlarmRepository

    @Binds
    internal abstract fun bindAttachmentRepository(
        attachmentRepositoryImpl: AttachmentRepositoryImpl
    ): AttachmentRepository

    @Binds
    internal abstract fun bindAttendeeRepository(attendeeRepositoryImpl: AttendeeRepositoryImpl): AttendeeRepository
}
