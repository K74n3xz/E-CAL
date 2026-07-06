package net.k74n3xz.ecal.core.database.calendar.converter

import androidx.room.TypeConverter
import java.time.Duration
import java.time.Instant

internal class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }

    @TypeConverter
    fun fromDuration(value: Duration?): String? = value?.toString()

    @TypeConverter
    fun toDuration(value: String?): Duration? = value?.let { Duration.parse(it) }
}