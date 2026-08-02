package net.k74n3xz.ecal.core.data.database

import androidx.room.TypeConverter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period

internal class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromDuration(value: Duration?): String? = value?.toString()

    @TypeConverter
    fun toDuration(value: String?): Duration? = value?.let { Duration.parse(it) }

    @TypeConverter
    fun fromPeriod(value: Period?): String? = value?.toString()

    @TypeConverter
    fun toPeriod(value: String?): Period? = value?.let { Period.parse(it) }
}
