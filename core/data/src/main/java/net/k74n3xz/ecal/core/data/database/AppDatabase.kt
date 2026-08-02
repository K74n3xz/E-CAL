package net.k74n3xz.ecal.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.k74n3xz.ecal.core.data.database.dao.AlarmDao
import net.k74n3xz.ecal.core.data.database.dao.AlarmOccurrenceDao
import net.k74n3xz.ecal.core.data.database.dao.AttachmentDao
import net.k74n3xz.ecal.core.data.database.dao.AttendeeDao
import net.k74n3xz.ecal.core.data.database.dao.EventDao
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.database.entity.AttachmentEntity
import net.k74n3xz.ecal.core.data.database.entity.AttendeeEntity
import net.k74n3xz.ecal.core.data.database.entity.EventEntity
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttachmentCrossRef
import net.k74n3xz.ecal.core.data.database.entity.relation.AlarmAttendeeCrossRef

@Database(
    entities = [
        EventEntity::class,
        AlarmEntity::class,
        AlarmOccurrenceEntity::class,
        AttachmentEntity::class,
        AttendeeEntity::class,
        AlarmAttachmentCrossRef::class,
        AlarmAttendeeCrossRef::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmOccurrenceDao(): AlarmOccurrenceDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun attendeeDao(): AttendeeDao
}
