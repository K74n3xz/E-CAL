package net.k74n3xz.ecal.core.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import net.k74n3xz.ecal.core.application.port.outbound.repository.AttendeeRepository
import net.k74n3xz.ecal.core.data.database.AppDatabase
import net.k74n3xz.ecal.core.data.database.dao.AlarmDao
import net.k74n3xz.ecal.core.data.database.dao.AlarmOccurrenceDao
import net.k74n3xz.ecal.core.data.database.dao.AttendeeDao
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.utils.toAttendee
import net.k74n3xz.ecal.core.data.utils.toAttendeeEntity
import net.k74n3xz.ecal.core.model.Attendee

internal class AttendeeRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val attendeeDao: AttendeeDao,
    private val alarmDao: AlarmDao,
    private val alarmOccurrenceDao: AlarmOccurrenceDao
) : AttendeeRepository {
    override suspend fun getAttendeeById(attendeeId: Long): Attendee? = attendeeDao.queryById(attendeeId)?.toAttendee()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllAttendees(): Flow<List<Attendee>> =
        attendeeDao.observeAllAttendees().mapLatest { list -> list.map { it.toAttendee() } }

    override suspend fun saveAttendee(attendee: Attendee) {
        attendeeDao.upsert(attendee.toAttendeeEntity())
    }

    override suspend fun deleteAttendeeById(attendeeId: Long) {
        appDatabase.withTransaction {
            alarmDao.queryIdsWithOnlyAttendee(attendeeId).forEach { alarmId ->
                alarmOccurrenceDao.updateDesiredStateByAlarmId(alarmId, DesiredState.INACTIVE)
                alarmOccurrenceDao.unlinkAlarmFromAlarmOccurrenceByAlarmId(alarmId)
                alarmDao.deleteById(alarmId)
            }
            attendeeDao.deleteById(attendeeId)
        }
    }
}
